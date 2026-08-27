package com.spendwise.app.domain.model

import java.util.Date

data class Expense(
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val description: String = "",
    val paymentMethod: String = "",
    val date: Date = Date(),
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val source: String = "MANUAL", // "CSV", "EXCEL", "RECEIPT", "VOICE", "MANUAL"
    val importId: String? = null,
    val reviewStatus: String = "confirmed",
    val isEssential: Boolean? = null
)

enum class ExpenseCategory(val displayName: String) {
    FOOD("Food"),
    TRANSPORT("Transport"),
    SHOPPING("Shopping"),
    RENT("Rent"),
    UTILITIES("Utilities"),
    EDUCATION("Education"),
    HEALTHCARE("Healthcare"),
    ENTERTAINMENT("Entertainment"),
    SUBSCRIPTIONS("Subscriptions"),
    TRAVEL("Travel"),
    INVESTMENT("Investment"),
    OTHER("Other")
}

enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    UPI("UPI"),
    CREDIT_CARD("Credit Card"),
    DEBIT_CARD("Debit Card"),
    BANK_TRANSFER("Bank Transfer"),
    OTHER("Other")
}
