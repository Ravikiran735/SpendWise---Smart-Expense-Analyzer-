package com.spendwise.app.domain.model

data class ImportPreferences(
    val autoCategorization: Boolean = true,
    val duplicateDetection: Boolean = true,
    val importNotifications: Boolean = true,
    val defaultImportBehavior: String = "REVIEW" // "REVIEW", "AUTO_IMPORT"
)
