package com.spendwise.app.domain.model

import java.util.Date

data class ImportHistory(
    val importId: String = "",
    val userId: String = "",
    val fileName: String = "",
    val sourceType: String = "CSV", // "CSV", "EXCEL", "RECEIPT", "VOICE", "MANUAL"
    val totalRecords: Int = 0,
    val newRecords: Int = 0,
    val duplicateRecords: Int = 0,
    val reviewRecords: Int = 0,
    val status: String = "COMPLETED", // "PROCESSING", "COMPLETED", "PARTIAL", "FAILED"
    val createdAt: Date = Date()
)
