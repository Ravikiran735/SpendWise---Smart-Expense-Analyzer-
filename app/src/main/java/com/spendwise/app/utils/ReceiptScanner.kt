package com.spendwise.app.utils

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.regex.Pattern

data class ScannedReceipt(
    val amount: Double,
    val description: String,
    val merchant: String = "Receipt",
    val suggestedCategory: String = "Shopping",
    val suggestedPaymentMethod: String = "UPI",
    val confidence: Double = 0.92,
    val reason: String = "Online retail purchase detected."
)

object ReceiptScanner {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun scanReceipt(context: Context, imageUri: Uri): ScannedReceipt? {
        val image = InputImage.fromFilePath(context, imageUri)
        val result = recognizer.process(image).await()

        var amount: Double? = null
        var merchant = "Receipt Scan"
        var category = "Shopping"
        var paymentMethod = "UPI"
        var reason = "Retail receipt scanned."

        val amountPattern = Pattern.compile("(?i)(total|amt|amount|sum|net|balance|grand total)\\s*[:=]?\\s*(?:rs\\.?|₹|inr)?\\s*([0-9.,]+)")
        val fallbackAmountPattern = Pattern.compile("(?:rs\\.?|₹|inr)\\s*([0-9.,]+)")

        val fullText = result.text.lowercase(Locale.ROOT)

        for (block in result.textBlocks) {
            val text = block.text
            val matcher = amountPattern.matcher(text)
            if (matcher.find()) {
                val amtStr = matcher.group(2)?.replace(",", "")
                val parsed = amtStr?.toDoubleOrNull()
                if (parsed != null && parsed > 0) {
                    amount = parsed
                }
            }
        }

        if (amount == null) {
            val fallbackMatcher = fallbackAmountPattern.matcher(result.text)
            if (fallbackMatcher.find()) {
                val amtStr = fallbackMatcher.group(1)?.replace(",", "")
                amount = amtStr?.toDoubleOrNull()
            }
        }

        // Multi-tier fallback: Look for decimal amounts on line items
        if (amount == null) {
            val decimalPattern = Pattern.compile("([0-9]{1,6}\\.[0-9]{2})")
            val decimalMatcher = decimalPattern.matcher(result.text)
            val candidateAmounts = mutableListOf<Double>()
            while (decimalMatcher.find()) {
                decimalMatcher.group(1)?.toDoubleOrNull()?.let {
                    if (it > 0) candidateAmounts.add(it)
                }
            }
            if (candidateAmounts.isNotEmpty()) {
                // Usually the highest decimal number on a receipt is the Total
                amount = candidateAmounts.maxOrNull()
            }
        }

        // Final fallback: look for any isolated number
        if (amount == null) {
            val numberPattern = Pattern.compile("\\b([1-9][0-9]{1,5})\\b")
            val numMatcher = numberPattern.matcher(result.text)
            val candidateNums = mutableListOf<Double>()
            while (numMatcher.find()) {
                numMatcher.group(1)?.toDoubleOrNull()?.let {
                    if (it in 10.0..100000.0) candidateNums.add(it)
                }
            }
            if (candidateNums.isNotEmpty()) {
                amount = candidateNums.maxOrNull()
            }
        }

        // First block often contains merchant name
        if (result.textBlocks.isNotEmpty()) {
            val topLines = result.textBlocks.first().lines
            if (topLines.isNotEmpty()) {
                merchant = topLines.first().text.take(30).trim()
            }
        }

        if (merchant.isBlank() || merchant.equals("Receipt Scan", ignoreCase = true)) {
            merchant = if (result.text.isNotBlank()) result.text.lines().firstOrNull { it.isNotBlank() }?.take(25) ?: "Scanned Receipt" else "Scanned Receipt"
        }

        // Suggest category
        when {
            fullText.contains("swiggy") || fullText.contains("zomato") || fullText.contains("restaurant") ||
                    fullText.contains("cafe") || fullText.contains("food") || fullText.contains("dine") ||
                    fullText.contains("kitchen") || fullText.contains("mcdonald") || fullText.contains("starbucks") ||
                    fullText.contains("bakery") || fullText.contains("hotel") || fullText.contains("lunch") ||
                    fullText.contains("dinner") || fullText.contains("tea") || fullText.contains("coffee") -> {
                category = "Food"
                reason = "Dining / restaurant receipt detected."
            }
            fullText.contains("uber") || fullText.contains("ola") || fullText.contains("fuel") ||
                    fullText.contains("petrol") || fullText.contains("hpcl") || fullText.contains("bpcl") ||
                    fullText.contains("diesel") || fullText.contains("cab") || fullText.contains("taxi") ||
                    fullText.contains("toll") || fullText.contains("parking") -> {
                category = "Transport"
                reason = "Fuel / transport receipt detected."
            }
            fullText.contains("amazon") || fullText.contains("flipkart") || fullText.contains("retail") ||
                    fullText.contains("store") || fullText.contains("mall") || fullText.contains("dmart") ||
                    fullText.contains("myntra") || fullText.contains("supermarket") || fullText.contains("mart") ||
                    fullText.contains("bazaar") || fullText.contains("clothing") -> {
                category = "Shopping"
                reason = "Online / retail shopping receipt detected."
            }
            fullText.contains("hospital") || fullText.contains("pharmacy") || fullText.contains("medical") ||
                    fullText.contains("apollo") || fullText.contains("chemist") || fullText.contains("clinic") ||
                    fullText.contains("dr.") || fullText.contains("medicine") || fullText.contains("lab") -> {
                category = "Healthcare"
                reason = "Pharmacy / healthcare receipt detected."
            }
            fullText.contains("electricity") || fullText.contains("water") || fullText.contains("gas") ||
                    fullText.contains("broadband") || fullText.contains("wifi") || fullText.contains("airtel") ||
                    fullText.contains("jio") -> {
                category = "Utilities"
                reason = "Utility / recurring bill detected."
            }
        }

        if (fullText.contains("card") || fullText.contains("visa") || fullText.contains("mastercard") || fullText.contains("pos")) {
            paymentMethod = "Credit Card"
        } else if (fullText.contains("upi") || fullText.contains("gpay") || fullText.contains("phonepe") || fullText.contains("paytm")) {
            paymentMethod = "UPI"
        } else if (fullText.contains("cash")) {
            paymentMethod = "Cash"
        }

        return ScannedReceipt(
            amount = amount ?: 0.0,
            description = merchant,
            merchant = merchant,
            suggestedCategory = category,
            suggestedPaymentMethod = paymentMethod,
            confidence = if (amount != null) 0.94 else 0.65,
            reason = reason
        )
    }
}
