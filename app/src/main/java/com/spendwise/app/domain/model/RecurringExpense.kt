package com.spendwise.app.domain.model

import java.util.Date

data class RecurringExpense(
    val id: String = "",
    val userId: String = "",
    val description: String = "",
    val category: String = "",
    val averageAmount: Double = 0.0,
    val frequency: String = "monthly",
    val confidence: Double = 0.0,
    val lastDetectedDate: Date = Date()
)
