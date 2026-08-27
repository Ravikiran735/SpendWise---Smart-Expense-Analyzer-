package com.spendwise.app.domain.model

import java.util.Date

data class Budget(
    val id: String = "",
    val userId: String = "",
    val category: String = "",
    val amount: Double = 0.0,
    val spentAmount: Double = 0.0,
    val month: Int = 0,
    val year: Int = 0,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
