package com.spendwise.app.utils

import com.spendwise.app.domain.model.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipInputStream

object ImportAnalyzer {

    // Column Mapping Definitions
    data class ColumnMapping(
        val dateIdx: Int = -1,
        val descIdx: Int = -1,
        val amountIdx: Int = -1,
        val debitIdx: Int = -1,
        val creditIdx: Int = -1,
        val categoryIdx: Int = -1,
        val paymentMethodIdx: Int = -1,
        val typeIdx: Int = -1
    ) {
        val isValid: Boolean
            get() = dateIdx >= 0 && (amountIdx >= 0 || (debitIdx >= 0 || creditIdx >= 0))
    }

    // Supported Date Formats for Auto-detection
    private val dateFormats = listOf(
        "yyyy-MM-dd",
        "yyyy/MM/dd",
        "dd/MM/yyyy",
        "dd-MM-yyyy",
        "MM/dd/yyyy",
        "MM-dd-yyyy",
        "dd-MMM-yyyy",
        "dd MMM yyyy",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "dd/MM/yy",
        "MM/dd/yy"
    )

    /**
     * Parse CSV Stream into raw table of strings
     */
    fun parseCsvStream(inputStream: InputStream): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        reader.useLines { lines ->
            for (line in lines) {
                if (line.isBlank()) continue
                val row = parseCsvLine(line)
                if (row.isNotEmpty() && row.any { it.isNotBlank() }) {
                    rows.add(row)
                }
            }
        }
        return rows
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.setLength(0)
                }
                else -> sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    /**
     * Parse Excel (.xlsx) file using Android SDK OpenXML & ZipInputStream
     * Zero external dependencies needed.
     */
    fun parseExcelStream(inputStream: InputStream): List<List<String>> {
        val sharedStrings = mutableListOf<String>()
        var sheetXmlBytes: ByteArray? = null

        val zip = ZipInputStream(inputStream)
        var entry = zip.nextEntry
        while (entry != null) {
            val name = entry.name.lowercase(Locale.ROOT)
            if (name.contains("sharedstrings.xml")) {
                sharedStrings.addAll(parseSharedStrings(zip.readBytes()))
            } else if (name.contains("sheet1.xml") || (name.contains("worksheets/") && sheetXmlBytes == null)) {
                sheetXmlBytes = zip.readBytes()
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }

        if (sheetXmlBytes == null) return emptyList()
        return parseSheetXml(sheetXmlBytes, sharedStrings)
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(bytes.inputStream(), "UTF-8")

            var eventType = parser.eventType
            var currentText = java.lang.StringBuilder()
            var insideText = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "t") {
                            insideText = true
                            currentText.setLength(0)
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (insideText) currentText.append(parser.text)
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "t") {
                            strings.add(currentText.toString())
                            insideText = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return strings
    }

    private fun parseSheetXml(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(bytes.inputStream(), "UTF-8")

            var eventType = parser.eventType
            var currentRow = mutableListOf<String>()
            var currentVal = java.lang.StringBuilder()
            var cellType = ""
            var insideVal = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "row" -> currentRow = mutableListOf()
                            "c" -> {
                                cellType = parser.getAttributeValue(null, "t") ?: ""
                                currentVal.setLength(0)
                            }
                            "v" -> insideVal = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (insideVal) currentVal.append(parser.text)
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "v" -> insideVal = false
                            "c" -> {
                                val raw = currentVal.toString().trim()
                                val value = if (cellType == "s") {
                                    val idx = raw.toIntOrNull() ?: -1
                                    if (idx in sharedStrings.indices) sharedStrings[idx] else raw
                                } else raw
                                currentRow.add(value)
                            }
                            "row" -> {
                                if (currentRow.isNotEmpty() && currentRow.any { it.isNotBlank() }) {
                                    rows.add(currentRow)
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return rows
    }

    /**
     * Detect Column Mapping from Header Row
     */
    fun detectColumnMapping(headers: List<String>): ColumnMapping {
        var dateIdx = -1
        var descIdx = -1
        var amountIdx = -1
        var debitIdx = -1
        var creditIdx = -1
        var categoryIdx = -1
        var paymentMethodIdx = -1
        var typeIdx = -1

        headers.forEachIndexed { index, header ->
            val h = header.lowercase(Locale.ROOT).trim().replace("_", " ").replace("-", " ")
            when {
                dateIdx == -1 && (h.contains("date") || h.contains("time") || h == "dt") -> dateIdx = index
                descIdx == -1 && (h.contains("desc") || h.contains("particular") || h.contains("narration") ||
                        h.contains("merchant") || h.contains("payee") || h.contains("detail") ||
                        h.contains("remark") || h.contains("note") || h == "name" || h == "title") -> descIdx = index
                debitIdx == -1 && (h == "debit" || h == "dr" || h.contains("withdrawal") || h.contains("spent") || h == "expense") -> debitIdx = index
                creditIdx == -1 && (h == "credit" || h == "cr" || h.contains("deposit") || h.contains("received") || h == "income") -> creditIdx = index
                amountIdx == -1 && (h == "amount" || h.contains("amount") || h == "sum" || h == "value" || h == "total") -> amountIdx = index
                categoryIdx == -1 && (h.contains("category") || h == "cat" || h == "tag") -> categoryIdx = index
                paymentMethodIdx == -1 && (h.contains("payment") || h.contains("mode") || h.contains("channel") || h.contains("method")) -> paymentMethodIdx = index
                typeIdx == -1 && (h == "type" || h == "transaction type" || h == "txn type" || h == "cr/dr") -> typeIdx = index
            }
        }

        return ColumnMapping(
            dateIdx = dateIdx,
            descIdx = descIdx,
            amountIdx = amountIdx,
            debitIdx = debitIdx,
            creditIdx = creditIdx,
            categoryIdx = categoryIdx,
            paymentMethodIdx = paymentMethodIdx,
            typeIdx = typeIdx
        )
    }

    /**
     * Full Analyzer Pipeline: Parse -> Normalize -> Validate -> Duplicate Detect -> Categorize
     */
    fun analyzeRows(
        rawRows: List<List<String>>,
        mapping: ColumnMapping,
        sourceType: String,
        existingExpenses: List<Expense>,
        existingIncomes: List<Income>,
        enableDuplicateDetection: Boolean = true,
        enableAutoCategorization: Boolean = true
    ): List<ImportCandidate> {
        if (rawRows.isEmpty()) return emptyList()

        // Filter out header row if present
        val dataRows = if (isHeaderRow(rawRows[0], mapping)) rawRows.drop(1) else rawRows

        // Build set of existing transaction fingerprints
        val existingFingerprints = mutableSetOf<String>()
        if (enableDuplicateDetection) {
            existingExpenses.forEach {
                existingFingerprints.add(computeFingerprint(it.date, it.amount, it.description, "Expense", it.paymentMethod))
            }
            existingIncomes.forEach {
                existingFingerprints.add(computeFingerprint(it.date, it.amount, it.description, "Income", it.paymentMethod))
            }
        }

        val inBatchFingerprints = mutableSetOf<String>()
        val candidates = mutableListOf<ImportCandidate>()

        for ((index, row) in dataRows.withIndex()) {
            val candidate = processRow(
                row = row,
                mapping = mapping,
                sourceType = sourceType,
                candidateIndex = index,
                existingFingerprints = existingFingerprints,
                inBatchFingerprints = inBatchFingerprints,
                enableDuplicateDetection = enableDuplicateDetection,
                enableAutoCategorization = enableAutoCategorization
            )
            candidates.add(candidate)
        }

        return candidates
    }

    private fun isHeaderRow(row: List<String>, mapping: ColumnMapping): Boolean {
        if (mapping.dateIdx in row.indices) {
            val dateVal = row[mapping.dateIdx].lowercase(Locale.ROOT)
            if (dateVal.contains("date") || dateVal.contains("txn") || parseDate(dateVal) == null) {
                return true
            }
        }
        return false
    }

    private fun processRow(
        row: List<String>,
        mapping: ColumnMapping,
        sourceType: String,
        candidateIndex: Int,
        existingFingerprints: Set<String>,
        inBatchFingerprints: MutableSet<String>,
        enableDuplicateDetection: Boolean,
        enableAutoCategorization: Boolean
    ): ImportCandidate {
        val rawDateStr = row.getOrNull(mapping.dateIdx)?.trim() ?: ""
        val rawDesc = (if (mapping.descIdx in row.indices) row[mapping.descIdx] else "").trim()

        var rawAmount = 0.0
        var txType = "Expense"

        // Handle Debit vs Credit or unified Amount
        if (mapping.debitIdx in row.indices && mapping.creditIdx in row.indices) {
            val debitVal = parseAmount(row.getOrNull(mapping.debitIdx) ?: "")
            val creditVal = parseAmount(row.getOrNull(mapping.creditIdx) ?: "")
            if (creditVal > 0.0) {
                rawAmount = creditVal
                txType = "Income"
            } else if (debitVal > 0.0) {
                rawAmount = debitVal
                txType = "Expense"
            }
        } else if (mapping.amountIdx in row.indices) {
            val amtStr = row.getOrNull(mapping.amountIdx) ?: ""
            val parsedAmt = parseAmount(amtStr)
            if (amtStr.contains("-") || amtStr.lowercase(Locale.ROOT).contains("dr")) {
                rawAmount = Math.abs(parsedAmt)
                txType = "Expense"
            } else if (amtStr.contains("+") || amtStr.lowercase(Locale.ROOT).contains("cr")) {
                rawAmount = Math.abs(parsedAmt)
                txType = "Income"
            } else {
                rawAmount = Math.abs(parsedAmt)
                // Check if Type column is present
                if (mapping.typeIdx in row.indices) {
                    val typeStr = row[mapping.typeIdx].lowercase(Locale.ROOT)
                    if (typeStr.contains("credit") || typeStr.contains("income") || typeStr == "cr") {
                        txType = "Income"
                    }
                }
            }
        }

        val parsedDate = parseDate(rawDateStr) ?: Date()
        val isDateValid = rawDateStr.isNotBlank() && parseDate(rawDateStr) != null
        val isAmountValid = rawAmount > 0.0

        // Payment Method Extraction
        val rawPaymentMethod = (if (mapping.paymentMethodIdx in row.indices) row[mapping.paymentMethodIdx] else "").trim()
        val paymentMethod = if (rawPaymentMethod.isNotBlank()) {
            normalizePaymentMethod(rawPaymentMethod)
        } else {
            detectPaymentMethod(rawDesc)
        }

        // Categorization & Confidence
        val rawCategory = (if (mapping.categoryIdx in row.indices) row[mapping.categoryIdx] else "").trim()
        val (category, confidence, inferredType) = if (rawCategory.isNotBlank() && !rawCategory.equals("other", ignoreCase = true)) {
            Triple(rawCategory, 0.95, txType)
        } else if (enableAutoCategorization) {
            categorizeTransaction(rawDesc, txType)
        } else {
            Triple(if (txType == "Income") "Other" else "Other", 0.5, txType)
        }

        val finalType = inferredType

        // Validation & Duplicate Status determination
        var status = CandidateStatus.NEW
        var statusReason = ""

        if (!isDateValid || !isAmountValid || rawDesc.isBlank()) {
            status = CandidateStatus.INVALID
            statusReason = when {
                !isAmountValid -> "Invalid or zero amount"
                !isDateValid -> "Unrecognized date format ($rawDateStr)"
                rawDesc.isBlank() -> "Missing description"
                else -> "Invalid record"
            }
        } else if (enableDuplicateDetection) {
            val fingerprint = computeFingerprint(parsedDate, rawAmount, rawDesc, finalType, paymentMethod)
            if (existingFingerprints.contains(fingerprint)) {
                status = CandidateStatus.DUPLICATE
                statusReason = "Already exists in SpendWise"
            } else if (inBatchFingerprints.contains(fingerprint)) {
                status = CandidateStatus.DUPLICATE
                statusReason = "Duplicate within import file"
            } else {
                inBatchFingerprints.add(fingerprint)
                if (confidence < 0.65 || category == "Other") {
                    status = CandidateStatus.NEEDS_REVIEW
                    statusReason = "Low categorization confidence"
                }
            }
        } else if (confidence < 0.65 || category == "Other") {
            status = CandidateStatus.NEEDS_REVIEW
            statusReason = "Low categorization confidence"
        }

        return ImportCandidate(
            id = UUID.randomUUID().toString(),
            date = parsedDate,
            description = rawDesc.ifBlank { "Imported Transaction #${candidateIndex + 1}" },
            amount = rawAmount,
            type = finalType,
            category = category,
            paymentMethod = paymentMethod,
            status = status,
            confidence = confidence,
            source = sourceType,
            statusReason = statusReason,
            isSelected = status != CandidateStatus.DUPLICATE && status != CandidateStatus.INVALID
        )
    }

    /**
     * Compute deterministic hash fingerprint
     */
    fun computeFingerprint(date: Date, amount: Double, description: String, type: String, paymentMethod: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        val dateStr = sdf.format(date)
        val amountStr = String.format(Locale.ROOT, "%.2f", amount)
        val cleanDesc = description.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
        val rawKey = "$dateStr|$amountStr|$cleanDesc|${type.lowercase(Locale.ROOT)}|${paymentMethod.lowercase(Locale.ROOT)}"

        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(rawKey.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Parse Date across common formats
     */
    fun parseDate(dateStr: String): Date? {
        if (dateStr.isBlank()) return null
        val cleaned = dateStr.trim().replace(Regex("\\s+"), " ")
        for (fmt in dateFormats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.ROOT)
                sdf.isLenient = false
                val parsed = sdf.parse(cleaned)
                if (parsed != null) return parsed
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Parse numerical amount from messy currency strings
     */
    fun parseAmount(amountStr: String): Double {
        if (amountStr.isBlank()) return 0.0
        val clean = amountStr
            .replace("₹", "")
            .replace("$", "")
            .replace("€", "")
            .replace("£", "")
            .replace(",", "")
            .replace(" ", "")
            .replace("CR", "", ignoreCase = true)
            .replace("DR", "", ignoreCase = true)
            .trim()
        return clean.toDoubleOrNull() ?: 0.0
    }

    /**
     * Shared Smart Categorization Rules & Confidence Calculator
     */
    fun categorizeTransaction(description: String, currentType: String): Triple<String, Double, String> {
        val d = description.lowercase(Locale.ROOT)

        // 1. Income Checks
        if (d.contains("salary") || d.contains("payroll") || d.contains("stipend") || d.contains("wages") || d.contains("direct dep")) {
            return Triple("Salary", 0.98, "Income")
        }
        if (d.contains("freelance") || d.contains("upwork") || d.contains("fiverr") || d.contains("client payment") || d.contains("consulting")) {
            return Triple("Freelance", 0.95, "Income")
        }
        if (d.contains("business") || d.contains("merchant payout") || d.contains("stripe payout") || d.contains("razorpay") || d.contains("revenue")) {
            return Triple("Business", 0.92, "Income")
        }
        if (d.contains("cashback") || d.contains("reward") || d.contains("gift") || d.contains("bonus") || d.contains("refund")) {
            return Triple("Gift", 0.88, "Income")
        }
        if (d.contains("dividend") || d.contains("interest cr") || d.contains("stock profit")) {
            return Triple("Investment", 0.90, "Income")
        }

        // 2. Expense Categories
        if (matchesAny(d, "swiggy", "zomato", "mcdonald", "starbucks", "kfc", "burger", "pizza", "dominos",
                "restaurant", "cafe", "dining", "food", "blinkit", "zepto", "instamart", "bigbasket",
                "dmart", "grocery", "groceries", "bakery", "kitchen", "eats", "bar", "pub")) {
            return Triple("Food", 0.95, "Expense")
        }

        if (matchesAny(d, "uber", "ola", "rapido", "taxi", "cab", "auto", "metro", "fuel", "petrol",
                "diesel", "shell", "hpcl", "bpcl", "iocl", "toll", "fastag", "parking", "bus ticket", "gas station")) {
            return Triple("Transport", 0.95, "Expense")
        }

        if (matchesAny(d, "amazon", "flipkart", "myntra", "zara", "h&m", "ajio", "nykaa", "retail",
                "mall", "clothing", "electronics", "store", "supermarket", "shop", "apparel", "apple store")) {
            return Triple("Shopping", 0.92, "Expense")
        }

        if (matchesAny(d, "rent", "landlord", "society maintenance", "housing", "lease", "apartment")) {
            return Triple("Rent", 0.95, "Expense")
        }

        if (matchesAny(d, "electricity", "power bill", "bescom", "tata power", "water bill", "gas bill",
                "lpg", "indane", "hp gas", "wifi", "internet", "broadband", "airtel", "jio", "vi bill",
                "mobile recharge", "recharge", "dth")) {
            return Triple("Utilities", 0.94, "Expense")
        }

        if (matchesAny(d, "school", "college", "university", "tuition", "udemy", "coursera", "edx",
                "books", "course", "exam fee", "academy", "classes")) {
            return Triple("Education", 0.90, "Expense")
        }

        if (matchesAny(d, "hospital", "clinic", "pharmacy", "apollo", "medplus", "1mg", "netmeds",
                "doctor", "lab", "dental", "medical", "chemist", "healthcare", "diagnostic")) {
            return Triple("Healthcare", 0.95, "Expense")
        }

        if (matchesAny(d, "netflix", "spotify", "prime video", "hotstar", "disney", "cinema", "pvr",
                "inox", "movie", "concert", "gaming", "steam", "playstation", "theatre", "entertainment")) {
            return Triple("Entertainment", 0.94, "Expense")
        }

        if (matchesAny(d, "google one", "icloud", "apple.com/bill", "chatgpt", "openai", "github",
                "linkedin premium", "gym", "cult.fit", "fitness", "membership", "subscription")) {
            return Triple("Subscriptions", 0.92, "Expense")
        }

        if (matchesAny(d, "flight", "indigo", "air india", "vistara", "makemytrip", "cleartrip",
                "easemytrip", "goibibo", "hotel", "airbnb", "booking.com", "train", "irctc", "redbus",
                "travel", "tour", "resort")) {
            return Triple("Travel", 0.95, "Expense")
        }

        if (matchesAny(d, "zerodha", "groww", "upstox", "mutual fund", "sip", "stocks", "coin",
                "kuvera", "smallcase", "fixed deposit", "fd", "bonds", "etf", "securities")) {
            return Triple("Investment", 0.92, "Expense")
        }

        // Low confidence fallback
        return Triple("Other", 0.40, currentType)
    }

    private fun matchesAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
    }

    private fun detectPaymentMethod(description: String): String {
        val d = description.lowercase(Locale.ROOT)
        return when {
            d.contains("upi") || d.contains("vpa") || d.contains("@") || d.contains("gpay") || d.contains("phonepe") || d.contains("paytm") -> "UPI"
            d.contains("credit card") || d.contains("cc ") || d.contains("visa") || d.contains("mastercard") || d.contains("amex") -> "Credit Card"
            d.contains("debit card") || d.contains("dc ") || d.contains("pos") || d.contains("atm wdl") -> "Debit Card"
            d.contains("neft") || d.contains("rtgs") || d.contains("imps") || d.contains("bank transfer") || d.contains("transfer") || d.contains("ach") -> "Bank Transfer"
            d.contains("cash") -> "Cash"
            else -> "UPI"
        }
    }

    fun normalizePaymentMethod(input: String): String {
        val i = input.lowercase(Locale.ROOT).trim()
        return when {
            i.contains("upi") || i.contains("gpay") || i.contains("phonepe") -> "UPI"
            i.contains("credit") -> "Credit Card"
            i.contains("debit") -> "Debit Card"
            i.contains("cash") -> "Cash"
            i.contains("bank") || i.contains("transfer") || i.contains("neft") || i.contains("imps") -> "Bank Transfer"
            else -> "Other"
        }
    }
}
