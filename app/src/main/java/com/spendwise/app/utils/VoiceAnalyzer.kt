package com.spendwise.app.utils

import java.util.Locale
import java.util.regex.Pattern

data class VoiceExpense(
    val amount: Double,
    val category: String,
    val merchant: String = "Voice Entry",
    val description: String,
    val confidence: Double = 0.95,
    val needsReview: Boolean = false
)

object VoiceAnalyzer {
    fun analyzeVoiceCommand(text: String): VoiceExpense? {
        val lower = text.lowercase(Locale.ROOT)

        // 1. Amount matching (supports "650 rupees", "rs 650", "₹650", "spent 650")
        val amountPattern = Pattern.compile("(?i)(?:rs\\.?|₹|inr)?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:rupees|rs|bucks|dollars)?")
        val amountMatcher = amountPattern.matcher(text)

        var amount: Double? = null
        while (amountMatcher.find()) {
            val numStr = amountMatcher.group(1)
            val parsed = numStr?.toDoubleOrNull()
            if (parsed != null && parsed > 0) {
                amount = parsed
                break
            }
        }

        if (amount == null) return null

        // 2. Merchant extraction (e.g. "at Swiggy", "for Uber", "to Amazon")
        var merchant = "Voice Entry"
        val merchantPattern = Pattern.compile("(?i)(?:at|to|for|on)\\s+([a-zA-Z0-9_]+)")
        val merchantMatcher = merchantPattern.matcher(text)
        if (merchantMatcher.find()) {
            val m = merchantMatcher.group(1)?.replaceFirstChar { it.uppercase() } ?: ""
            if (m.isNotBlank() && !m.equals("rupees", true) && !m.equals("yesterday", true)) {
                merchant = m
            }
        }

        // 3. Category matching with confidence scoring
        var category = "Other"
        var confidence = 0.60

        when {
            lower.contains("swiggy") || lower.contains("zomato") || lower.contains("food") ||
                    lower.contains("lunch") || lower.contains("dinner") || lower.contains("breakfast") ||
                    lower.contains("restaurant") || lower.contains("cafe") || lower.contains("coffee") ||
                    lower.contains("starbucks") || lower.contains("mcdonald") || lower.contains("burger") ||
                    lower.contains("pizza") || lower.contains("groceries") || lower.contains("blinkit") ||
                    lower.contains("zepto") || lower.contains("instamart") -> {
                category = "Food"
                confidence = 0.96
            }
            lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") ||
                    lower.contains("taxi") || lower.contains("cab") || lower.contains("auto") ||
                    lower.contains("petrol") || lower.contains("fuel") || lower.contains("diesel") ||
                    lower.contains("metro") || lower.contains("bus") -> {
                category = "Transport"
                confidence = 0.95
            }
            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") ||
                    lower.contains("zara") || lower.contains("h&m") || lower.contains("shopping") ||
                    lower.contains("cloth") || lower.contains("mall") -> {
                category = "Shopping"
                confidence = 0.94
            }
            lower.contains("rent") || lower.contains("house") || lower.contains("flat") || lower.contains("landlord") -> {
                category = "Rent"
                confidence = 0.95
            }
            lower.contains("electricity") || lower.contains("power") || lower.contains("water") ||
                    lower.contains("gas") || lower.contains("wifi") || lower.contains("internet") ||
                    lower.contains("broadband") || lower.contains("recharge") -> {
                category = "Utilities"
                confidence = 0.93
            }
            lower.contains("hospital") || lower.contains("doctor") || lower.contains("medicine") ||
                    lower.contains("pharmacy") || lower.contains("apollo") || lower.contains("clinic") -> {
                category = "Healthcare"
                confidence = 0.95
            }
            lower.contains("netflix") || lower.contains("spotify") || lower.contains("movie") ||
                    lower.contains("cinema") || lower.contains("pvr") || lower.contains("hotstar") -> {
                category = "Entertainment"
                confidence = 0.94
            }
            lower.contains("zerodha") || lower.contains("groww") || lower.contains("stocks") ||
                    lower.contains("mutual fund") || lower.contains("sip") || lower.contains("investment") -> {
                category = "Investment"
                confidence = 0.92
            }
        }

        return VoiceExpense(
            amount = amount,
            category = category,
            merchant = merchant,
            description = text.trim(),
            confidence = confidence,
            needsReview = confidence < 0.80
        )
    }
}
