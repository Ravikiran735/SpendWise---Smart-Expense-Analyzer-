package com.spendwise.app.domain.model

import java.util.Date

data class AiSpendingAnalysis(
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netSavings: Double = 0.0,
    val savingsRate: Double = 0.0,
    val explanation: String = "",
    val topCategories: List<Pair<String, Double>> = emptyList()
)

data class AiBudgetRecommendation(
    val category: String = "",
    val averageSpending: Double = 0.0,
    val currentBudget: Double = 0.0,
    val recommendedAmount: Double = 0.0,
    val recommendedRange: String = "",
    val reason: String = ""
)

data class AiSavingsPlan(
    val targetAmount: Double = 0.0,
    val currentSavings: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val suggestedMonthlySaving: Double = 0.0,
    val estimatedDurationMonths: Int = 0,
    val explanation: String = ""
)

data class AiAnomaly(
    val expense: Expense,
    val averageAmount: Double = 0.0,
    val ratio: Double = 0.0,
    val tag: String = "Higher than usual"
)

data class AiMonthlyReview(
    val income: Double = 0.0,
    val expenses: Double = 0.0,
    val savings: Double = 0.0,
    val savingsRate: Double = 0.0,
    val whatWentWell: List<String> = emptyList(),
    val watchOut: List<String> = emptyList(),
    val recommendations: List<String> = emptyList()
)

// ==========================================
// 1. WHAT-IF FINANCIAL SIMULATOR MODELS
// ==========================================
data class GoalTimelineShift(
    val goalTitle: String = "",
    val currentMonthsRemaining: Double = 0.0,
    val projectedMonthsRemaining: Double = 0.0,
    val daysSavedOrDelayed: Int = 0
)

data class WhatIfSimulationResult(
    val currentMonthlySavings: Double = 0.0,
    val projectedMonthlySavings: Double = 0.0,
    val savingsImprovementPct: Double = 0.0,
    val currentSavingsRate: Double = 0.0,
    val projectedSavingsRate: Double = 0.0,
    val goalShifts: List<GoalTimelineShift> = emptyList(),
    val explanation: String = "",
    val deltaSummary: String = ""
)

// ==========================================
// 2. PURCHASE IMPACT ANALYZER MODELS
// ==========================================
enum class AffordabilityRating {
    SAFE,
    CAUTION,
    NOT_RECOMMENDED
}

data class PurchaseAffordabilityResult(
    val amount: Double = 0.0,
    val description: String = "",
    val rating: AffordabilityRating = AffordabilityRating.SAFE,
    val verdictTitle: String = "SAFE",
    val message: String = "",
    val goalImpactDays: Int = 0,
    val affectedGoalTitle: String = "",
    val monthlySurplusAfter: Double = 0.0,
    val emergencyBufferRemaining: Double = 0.0,
    val discretionaryImpactPct: Double = 0.0,
    val explanationWhy: String = "",
    val dataUsedSummary: String = ""
)

// ==========================================
// 3. FINANCIAL DIGITAL TWIN / PROFILE
// ==========================================
data class FinancialDigitalTwin(
    val avgMonthlyIncome: Double = 0.0,
    val avgMonthlyExpenses: Double = 0.0,
    val avgMonthlySavings: Double = 0.0,
    val savingsRate: Double = 0.0,
    val topSpendingCategories: List<Pair<String, Double>> = emptyList(),
    val monthlyRecurringTotal: Double = 0.0,
    val discretionarySpendingTotal: Double = 0.0,
    val essentialSpendingTotal: Double = 0.0,
    val incomeStabilityPct: Int = 80,
    val savingsDisciplinePct: Int = 75,
    val budgetDisciplinePct: Int = 80,
    val spendingStabilityPct: Int = 70,
    val spendingVolatilityPct: Double = 0.0,
    val goalProgressPct: Int = 0,
    val summaryNarrative: String = ""
)

// ==========================================
// 4. ESSENTIAL VS DISCRETIONARY SPENDING
// ==========================================
data class EssentialDiscretionaryAnalysis(
    val essentialTotal: Double = 0.0,
    val discretionaryTotal: Double = 0.0,
    val essentialPct: Double = 0.0,
    val discretionaryPct: Double = 0.0,
    val essentialCategories: List<Pair<String, Double>> = emptyList(),
    val discretionaryCategories: List<Pair<String, Double>> = emptyList(),
    val recommendation: String = ""
)

// ==========================================
// 5. RECURRING MONEY MAP
// ==========================================
enum class RecurringStatus {
    KEEP,
    REVIEW,
    CANCEL
}

data class RecurringMoneyItem(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val monthlyAmount: Double = 0.0,
    val annualAmount: Double = 0.0,
    val frequency: String = "monthly",
    val status: RecurringStatus = RecurringStatus.KEEP,
    val confidence: Double = 0.85,
    val lastChargedDate: Date = Date()
)

data class RecurringMoneyMap(
    val monthlyRecurringTotal: Double = 0.0,
    val annualProjectedTotal: Double = 0.0,
    val items: List<RecurringMoneyItem> = emptyList(),
    val potentialSavingsIfCancelled: Double = 0.0
)

// ==========================================
// 6. SPENDING LEAK DETECTOR
// ==========================================
data class SpendingLeak(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val frequencyCount: Int = 0,
    val averageAmount: Double = 0.0,
    val monthlyTotal: Double = 0.0,
    val annualImpact: Double = 0.0,
    val aiExplanation: String = ""
)

// ==========================================
// 7. MONEY HABIT SCORE
// ==========================================
data class HabitBulletPoint(
    val isPositive: Boolean = true,
    val title: String = "",
    val description: String = ""
)

data class MoneyHabitScore(
    val score: Int = 75,
    val label: String = "GOOD MONEY HABITS",
    val savingsConsistencyScore: Int = 20, // Max 25
    val budgetAdherenceScore: Int = 20,    // Max 25
    val recurringExpenseControlScore: Int = 15, // Max 15
    val discretionaryRatioScore: Int = 15, // Max 15
    val goalContributionsScore: Int = 10,  // Max 10
    val spendingVolatilityScore: Int = 10, // Max 10
    val bulletPoints: List<HabitBulletPoint> = emptyList(),
    val habitExplanation: String = ""
)

// ==========================================
// 8. GOAL ROADMAP & MULTI-GOAL PRIORITY
// ==========================================
data class GoalRoadmapItem(
    val id: String = "",
    val title: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val progressPct: Int = 0,
    val monthlyContributionRate: Double = 0.0,
    val projectedCompletionDate: String = "November 2026",
    val aiSuggestion: String = "",
    val milestoneReached: Int = 0 // 25, 50, 75, 100
)

data class GoalPriorityAllocation(
    val goalId: String = "",
    val goalTitle: String = "",
    val recommendedMonthlyAmount: Double = 0.0,
    val allocationPercentage: Double = 0.0,
    val priorityRank: Int = 1,
    val reasonWhy: String = ""
)

data class MultiGoalPriorityDistribution(
    val monthlyAvailableSavings: Double = 0.0,
    val allocations: List<GoalPriorityAllocation> = emptyList(),
    val overallRationale: String = "",
    val dataUsed: String = ""
)

// ==========================================
// 9. MONEY ALERTS & UNIFIED ALERT CENTER
// ==========================================
enum class MoneyAlertType {
    BUDGET_RISK,
    SPENDING_OPPORTUNITY,
    GOAL_MILESTONE,
    UNUSUAL_SPENDING,
    RECURRING_EXPENSE
}

data class MoneyAlert(
    val id: String = "",
    val type: MoneyAlertType = MoneyAlertType.BUDGET_RISK,
    val title: String = "",
    val message: String = "",
    val severity: String = "warning", // "danger", "warning", "success", "info"
    val timestamp: Date = Date(),
    val actionLabel: String = "View",
    val actionRoute: String = "budget",
    val isDismissed: Boolean = false
)

// ==========================================
// 10. WEEKLY & MONTH-END REVIEW MODELS
// ==========================================
data class WeeklyMoneyReview(
    val spentThisWeek: Double = 0.0,
    val savedThisWeek: Double = 0.0,
    val largestCategory: String = "Food",
    val largestCategoryAmount: Double = 0.0,
    val changePctVsLastWeek: Double = 0.0,
    val isSpendingLowerThanLastWeek: Boolean = true,
    val aiSummary: String = "",
    val oneActionForNextWeek: String = "",
    val actionCategory: String = "Food",
    val suggestedReductionAmount: Double = 500.0
)

data class NextMonthPlan(
    val recommendedCategoryBudgets: Map<String, Double> = emptyMap(),
    val recommendedSavingsTarget: Double = 0.0,
    val primaryFocusArea: String = ""
)

data class ComprehensiveMonthEndReview(
    val monthYear: String = "",
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalSavings: Double = 0.0,
    val savingsRate: Double = 0.0,
    val whatWentWell: List<String> = emptyList(),
    val whereMoneyWent: List<Pair<String, Double>> = emptyList(),
    val budgetsStatus: List<String> = emptyList(),
    val savingsStatus: List<String> = emptyList(),
    val goalsStatus: List<String> = emptyList(),
    val unusualSpendingList: List<String> = emptyList(),
    val recommendedAction: String = "",
    val nextMonthPlan: NextMonthPlan = NextMonthPlan()
)

// ==========================================
// 11. FINANCIAL FORECAST
// ==========================================
data class FinancialForecast(
    val nextMonthName: String = "",
    val expectedIncome: Double = 0.0,
    val expectedExpenses: Double = 0.0,
    val expectedSavings: Double = 0.0,
    val confidenceLevel: String = "Medium", // "High", "Medium", "Low"
    val varianceMarginPct: Double = 10.0,
    val budgetRiskNotes: String = "",
    val isEstimateDisclaimer: String = "Projections are mathematical estimates based on your historical transaction patterns. Not a guaranteed outcome."
)

// ==========================================
// 12. CASHFLOW CALENDAR & MONEY JOURNEY
// ==========================================
enum class CashflowEntryType {
    INCOME,
    RECURRING_EXPENSE,
    LARGE_TRANSACTION,
    BUDGET_DEADLINE,
    SAVINGS_CONTRIBUTION
}

data class CashflowCalendarEntry(
    val id: String = "",
    val date: Date = Date(),
    val dayOfMonth: Int = 1,
    val monthName: String = "AUG",
    val title: String = "",
    val amount: Double = 0.0,
    val type: CashflowEntryType = CashflowEntryType.RECURRING_EXPENSE,
    val isProjected: Boolean = true
)

data class MoneyJourneyMilestone(
    val id: String = "",
    val monthLabel: String = "AUG",
    val year: Int = 2026,
    val title: String = "",
    val description: String = "",
    val iconName: String = "fa-chart-line",
    val type: String = "milestone", // "income", "expense", "budget", "goal", "milestone"
    val deltaAmount: Double = 0.0
)

// ==========================================
// 13. AI ACTION CARDS & EXPLAINABILITY
// ==========================================
data class AiActionCard(
    val title: String = "",
    val actionType: String = "SIMULATE", // "SIMULATE", "VIEW_SPENDING", "SET_BUDGET", "VIEW_GOAL"
    val payloadCategory: String = "",
    val payloadAmount: Double = 0.0
)

data class AiExplainableRecommendation(
    val recommendationText: String = "",
    val whyExplanation: String = "",
    val dataUsedSummary: String = "",
    val actionCards: List<AiActionCard> = emptyList()
)
