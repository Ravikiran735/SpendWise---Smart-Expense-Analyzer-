package com.spendwise.app.utils

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.spendwise.app.domain.model.Budget
import com.spendwise.app.domain.model.Expense
import com.spendwise.app.domain.model.ImportHistory
import com.spendwise.app.domain.model.Income
import com.spendwise.app.domain.model.SavingsGoal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FirestoreParser {

    fun parseDate(raw: Any?, fallback: Date = Date()): Date {
        if (raw == null) return fallback
        return when (raw) {
            is Timestamp -> raw.toDate()
            is Date -> raw
            is Long -> Date(raw)
            is Double -> Date(raw.toLong())
            is String -> {
                val patterns = listOf(
                    "yyyy-MM-dd",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd'T'HH:mm:ssX",
                    "dd-MM-yyyy",
                    "MM/dd/yyyy",
                    "dd/MM/yyyy"
                )
                for (pattern in patterns) {
                    try {
                        val sdf = SimpleDateFormat(pattern, Locale.ROOT)
                        val parsed = sdf.parse(raw)
                        if (parsed != null) return parsed
                    } catch (_: Exception) {}
                }
                fallback
            }
            else -> fallback
        }
    }

    fun parseDouble(raw: Any?, default: Double = 0.0): Double {
        if (raw == null) return default
        return when (raw) {
            is Number -> raw.toDouble()
            is String -> raw.toDoubleOrNull() ?: default
            else -> default
        }
    }

    fun parseInt(raw: Any?, default: Int = 0): Int {
        if (raw == null) return default
        return when (raw) {
            is Number -> raw.toInt()
            is String -> raw.toIntOrNull() ?: default
            else -> default
        }
    }

    fun parseExpense(doc: DocumentSnapshot): Expense? {
        return try {
            val data = doc.data ?: return null
            Expense(
                id = doc.id,
                userId = data["userId"] as? String ?: "",
                amount = parseDouble(data["amount"]),
                category = data["category"] as? String ?: "Other",
                description = data["description"] as? String ?: "",
                paymentMethod = data["paymentMethod"] as? String ?: "Cash",
                date = parseDate(data["date"]),
                createdAt = parseDate(data["createdAt"]),
                updatedAt = parseDate(data["updatedAt"]),
                source = data["source"] as? String ?: "MANUAL",
                importId = data["importId"] as? String,
                reviewStatus = data["reviewStatus"] as? String ?: "confirmed",
                isEssential = data["isEssential"] as? Boolean
            )
        } catch (_: Exception) {
            null
        }
    }

    fun parseIncome(doc: DocumentSnapshot): Income? {
        return try {
            val data = doc.data ?: return null
            Income(
                id = doc.id,
                userId = data["userId"] as? String ?: "",
                amount = parseDouble(data["amount"]),
                source = (data["source"] as? String) ?: (data["category"] as? String) ?: "Other",
                description = data["description"] as? String ?: "",
                date = parseDate(data["date"]),
                createdAt = parseDate(data["createdAt"]),
                updatedAt = parseDate(data["updatedAt"]),
                origin = data["origin"] as? String ?: "MANUAL",
                importId = data["importId"] as? String,
                paymentMethod = data["paymentMethod"] as? String ?: "Bank Transfer",
                reviewStatus = data["reviewStatus"] as? String ?: "confirmed"
            )
        } catch (_: Exception) {
            null
        }
    }

    fun parseBudget(doc: DocumentSnapshot): Budget? {
        return try {
            val data = doc.data ?: return null
            Budget(
                id = doc.id,
                userId = data["userId"] as? String ?: "",
                category = data["category"] as? String ?: "Other",
                amount = parseDouble(data["amount"]),
                spentAmount = parseDouble(data["spentAmount"]),
                month = parseInt(data["month"]),
                year = parseInt(data["year"]),
                createdAt = parseDate(data["createdAt"]),
                updatedAt = parseDate(data["updatedAt"])
            )
        } catch (_: Exception) {
            null
        }
    }

    fun parseSavingsGoal(doc: DocumentSnapshot): SavingsGoal? {
        return try {
            val data = doc.data ?: return null
            val rawDeadline = data["deadline"] ?: data["targetDate"]
            val parsedDeadline = if (rawDeadline != null) parseDate(rawDeadline) else null
            SavingsGoal(
                id = doc.id,
                userId = data["userId"] as? String ?: "",
                title = (data["title"] as? String) ?: (data["name"] as? String) ?: "Goal",
                targetAmount = parseDouble(data["targetAmount"]),
                currentAmount = parseDouble(data["currentAmount"] ?: data["savedAmount"]),
                deadline = parsedDeadline,
                createdAt = parseDate(data["createdAt"]),
                updatedAt = parseDate(data["updatedAt"])
            )
        } catch (_: Exception) {
            null
        }
    }

    fun parseImportHistory(doc: DocumentSnapshot): ImportHistory? {
        return try {
            val data = doc.data ?: return null
            ImportHistory(
                importId = doc.id,
                userId = data["userId"] as? String ?: "",
                fileName = data["fileName"] as? String ?: "",
                sourceType = data["sourceType"] as? String ?: "CSV",
                totalRecords = parseInt(data["totalRecords"]),
                newRecords = parseInt(data["newRecords"]),
                duplicateRecords = parseInt(data["duplicateRecords"]),
                reviewRecords = parseInt(data["reviewRecords"]),
                status = data["status"] as? String ?: "COMPLETED",
                createdAt = parseDate(data["createdAt"])
            )
        } catch (_: Exception) {
            null
        }
    }
}
