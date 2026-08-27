package com.spendwise.app.domain.model

import java.util.Date

data class Insight(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: InsightType = InsightType.SPENDING,
    val priority: Int = 0,
    val createdAt: Date = Date()
)

enum class InsightType {
    SPENDING,
    COMPARISON,
    BUDGET,
    RECURRING,
    SAVINGS,
    UNUSUAL
}
