package com.spendwise.app.domain.model

import java.util.Date

enum class AiMessageSender {
    USER, AI
}

data class AiMessage(
    val id: String = "",
    val sender: AiMessageSender = AiMessageSender.AI,
    val text: String = "",
    val timestamp: Date = Date(),
    val suggestedActions: List<String> = emptyList()
)
