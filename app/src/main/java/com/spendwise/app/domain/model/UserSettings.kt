package com.spendwise.app.domain.model

import java.util.Date

data class UserSettings(
    val theme: String = "dark", // "dark", "light", "system"
    val accentColor: String = "indigo", // "indigo", "emerald", "rose", "amber", "cyan"
    val currency: String = "INR", // "INR", "USD", "EUR", "GBP"
    val language: String = "en",
    val notificationsEnabled: Boolean = true,
    val budgetAlerts: Boolean = true,
    val transactionAlerts: Boolean = true,
    val savingsReminders: Boolean = true,
    val financialInsights: Boolean = true,
    val importNotifications: Boolean = true,
    val autoCategorization: Boolean = true,
    val duplicateDetection: Boolean = true,
    val primaryGoal: String = "Save Money", // "Save Money", "Control Spending", "Build Emergency Fund", "Buy Something", "Travel", "Education"
    val financialMode: String = "SAVE", // "BUILD", "BALANCE", "SAVE", "CONTROL"
    val dismissedAlertIds: List<String> = emptyList(),
    val recurringOverrides: Map<String, String> = emptyMap(), // id -> "keep" | "review" | "cancel"
    val essentialOverrides: Map<String, Boolean> = emptyMap(), // category -> isEssential
    val updatedAt: Date = Date()
)

