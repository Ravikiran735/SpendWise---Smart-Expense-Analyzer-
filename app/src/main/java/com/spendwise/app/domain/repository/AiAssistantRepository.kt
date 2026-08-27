package com.spendwise.app.domain.repository

import com.spendwise.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AiAssistantRepository {
    fun getConversationHistory(): Flow<List<AiMessage>>
    suspend fun sendMessage(messageText: String): AiMessage
    suspend fun clearConversation(): Result<Unit>
    
    suspend fun getSpendingAnalysis(): AiSpendingAnalysis
    suspend fun getBudgetRecommendations(): List<AiBudgetRecommendation>
    suspend fun calculateSavingsPlan(targetAmount: Double, targetMonths: Int): AiSavingsPlan
    suspend fun getSpendingAnomalies(): List<AiAnomaly>
    suspend fun getMonthlyReview(): AiMonthlyReview
    suspend fun getHealthScoreExplanation(healthScore: Int): String
    suspend fun applyBudgetRecommendation(recommendation: AiBudgetRecommendation): Result<Unit>

    // Decision Intelligence Extensions
    suspend fun simulateWhatIf(
        simulatedIncomeDelta: Double,
        simulatedCategoryDeltas: Map<String, Double>,
        simulatedRecurringDelta: Double
    ): WhatIfSimulationResult

    suspend fun assessPurchaseAffordability(
        amount: Double,
        description: String,
        category: String
    ): PurchaseAffordabilityResult

    suspend fun getFinancialDigitalTwin(): FinancialDigitalTwin
    suspend fun getMoneyHabitScore(): MoneyHabitScore
    suspend fun getEssentialVsDiscretionary(): EssentialDiscretionaryAnalysis
    suspend fun getRecurringMoneyMap(): RecurringMoneyMap
    suspend fun getSpendingLeaks(): List<SpendingLeak>
    suspend fun getGoalRoadmap(): List<GoalRoadmapItem>
    suspend fun getMultiGoalPriority(): MultiGoalPriorityDistribution
    suspend fun getMoneyAlerts(): List<MoneyAlert>
    suspend fun getWeeklyMoneyReview(): WeeklyMoneyReview
    suspend fun getComprehensiveMonthEndReview(): ComprehensiveMonthEndReview
    suspend fun getFinancialForecast(): FinancialForecast
    suspend fun getCashflowCalendar(): List<CashflowCalendarEntry>
    suspend fun getMoneyJourneyTimeline(): List<MoneyJourneyMilestone>
}
