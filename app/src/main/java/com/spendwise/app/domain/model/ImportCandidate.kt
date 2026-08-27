package com.spendwise.app.domain.model

import java.util.Date

enum class CandidateStatus {
    NEW,
    DUPLICATE,
    NEEDS_REVIEW,
    INVALID
}

data class ImportCandidate(
    val id: String = "",
    val date: Date = Date(),
    val description: String = "",
    val amount: Double = 0.0,
    val type: String = "Expense", // "Expense" or "Income"
    val category: String = "Other",
    val paymentMethod: String = "UPI",
    val status: CandidateStatus = CandidateStatus.NEW,
    val confidence: Double = 1.0,
    val source: String = "CSV",
    val statusReason: String = "",
    val isSelected: Boolean = true
)
