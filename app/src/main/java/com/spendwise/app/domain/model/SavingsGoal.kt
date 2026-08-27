package com.spendwise.app.domain.model

import java.util.Date

data class SavingsGoal(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val deadline: Date? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
