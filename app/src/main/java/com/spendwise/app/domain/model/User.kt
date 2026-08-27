package com.spendwise.app.domain.model

import java.util.Date

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val currency: String = "INR",
    val monthlyIncome: Double = 0.0,
    val role: String = "user",
    val status: String = "active",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
