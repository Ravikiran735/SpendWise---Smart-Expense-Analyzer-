package com.spendwise.app.domain.usecase

import com.spendwise.app.domain.model.*
import com.spendwise.app.domain.repository.AiAssistantRepository
import kotlinx.coroutines.flow.Flow

class AiAssistantUseCase(
    private val repository: AiAssistantRepository
) {
    fun getConversationHistory(): Flow<List<AiMessage>> = repository.getConversationHistory()

    suspend fun sendMessage(message: String): AiMessage = repository.sendMessage(message)

    suspend fun clearConversation(): Result<Unit> = repository.clearConversation()

    suspend fun getSpendingAnalysis(): AiSpendingAnalysis = repository.getSpendingAnalysis()

    suspend fun getBudgetRecommendations(): List<AiBudgetRecommendation> = repository.getBudgetRecommendations()

    suspend fun calculateSavingsPlan(targetAmount: Double, targetMonths: Int): AiSavingsPlan =
        repository.calculateSavingsPlan(targetAmount, targetMonths)

    suspend fun getSpendingAnomalies(): List<AiAnomaly> = repository.getSpendingAnomalies()

    suspend fun getMonthlyReview(): AiMonthlyReview = repository.getMonthlyReview()

    suspend fun getHealthScoreExplanation(healthScore: Int): String =
        repository.getHealthScoreExplanation(healthScore)

    suspend fun applyBudgetRecommendation(recommendation: AiBudgetRecommendation): Result<Unit> =
        repository.applyBudgetRecommendation(recommendation)

    // Decision Intelligence Extensions
    suspend fun simulateWhatIf(
        simulatedIncomeDelta: Double,
        simulatedCategoryDeltas: Map<String, Double>,
        simulatedRecurringDelta: Double
    ): WhatIfSimulationResult = repository.simulateWhatIf(simulatedIncomeDelta, simulatedCategoryDeltas, simulatedRecurringDelta)

    suspend fun assessPurchaseAffordability(
        amount: Double,
        description: String,
        category: String
    ): PurchaseAffordabilityResult = repository.assessPurchaseAffordability(amount, description, category)

    suspend fun getFinancialDigitalTwin(): FinancialDigitalTwin = repository.getFinancialDigitalTwin()

    suspend fun getMoneyHabitScore(): MoneyHabitScore = repository.getMoneyHabitScore()

    suspend fun getEssentialVsDiscretionary(): EssentialDiscretionaryAnalysis = repository.getEssentialVsDiscretionary()

    suspend fun getRecurringMoneyMap(): RecurringMoneyMap = repository.getRecurringMoneyMap()

    suspend fun getSpendingLeaks(): List<SpendingLeak> = repository.getSpendingLeaks()

    suspend fun getGoalRoadmap(): List<GoalRoadmapItem> = repository.getGoalRoadmap()

    suspend fun getMultiGoalPriority(): MultiGoalPriorityDistribution = repository.getMultiGoalPriority()

    suspend fun getMoneyAlerts(): List<MoneyAlert> = repository.getMoneyAlerts()

    suspend fun getWeeklyMoneyReview(): WeeklyMoneyReview = repository.getWeeklyMoneyReview()

    suspend fun getComprehensiveMonthEndReview(): ComprehensiveMonthEndReview = repository.getComprehensiveMonthEndReview()

    suspend fun getFinancialForecast(): FinancialForecast = repository.getFinancialForecast()

    suspend fun getCashflowCalendar(): List<CashflowCalendarEntry> = repository.getCashflowCalendar()

    suspend fun getMoneyJourneyTimeline(): List<MoneyJourneyMilestone> = repository.getMoneyJourneyTimeline()
}
