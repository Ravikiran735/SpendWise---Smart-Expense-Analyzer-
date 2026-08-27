package com.spendwise.app.domain.model

import java.util.Date

data class Income(
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val source: String = "Other", // Income Category/Source: Salary, Freelance, etc.
    val description: String = "",
    val date: Date = Date(),
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val origin: String = "MANUAL", // "CSV", "EXCEL", "RECEIPT", "VOICE", "MANUAL"
    val importId: String? = null,
    val paymentMethod: String = "Bank Transfer",
    val reviewStatus: String = "confirmed"
)

enum class IncomeSource(val displayName: String) {
    SALARY("Salary"),
    FREELANCE("Freelance"),
    BUSINESS("Business"),
    INVESTMENT("Investment"),
    GIFT("Gift"),
    OTHER("Other")
}
