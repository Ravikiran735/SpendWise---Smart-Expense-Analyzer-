package com.spendwise.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.di.AppModule
import com.spendwise.app.domain.model.*
import com.spendwise.app.utils.AnalysisEngine
import com.spendwise.app.utils.FinancialHealth
import com.spendwise.app.utils.SmartSnapshot
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class DashboardImportSummary(
    val lastImportDate: Date? = null,
    val lastImportFileName: String = "",
    val transactionsImported: Int = 0,
    val duplicatesPrevented: Int = 0,
    val needsReviewCount: Int = 0,
    val totalImportsCount: Int = 0
)

data class DashboardState(
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val savings: Double = 0.0,
    val savingsRate: Double = 0.0,
    val currency: String = "INR",
    val categoryTotals: Map<String, Double> = emptyMap(),
    val insights: List<Insight> = emptyList(),
    val dailySpending: Map<Int, Double> = emptyMap(),
    val financialHealth: FinancialHealth = FinancialHealth(),
    val smartSnapshot: SmartSnapshot = SmartSnapshot(),
    val importSummary: DashboardImportSummary = DashboardImportSummary(),
    val moneyHabitScore: MoneyHabitScore = MoneyHabitScore(),
    val essentialVsDiscretionary: EssentialDiscretionaryAnalysis = EssentialDiscretionaryAnalysis(),
    val topSpendingLeaks: List<SpendingLeak> = emptyList(),
    val goalRoadmap: List<GoalRoadmapItem> = emptyList(),
    val upcomingRecurring: List<RecurringMoneyItem> = emptyList(),
    val todayInsight: String = "",
    val oneActionToConsider: String = "",
    val actionCategory: String = "Food",
    val actionReductionAmount: Double = 1000.0,
    val activeMoneyAlerts: List<MoneyAlert> = emptyList(),
    val primaryGoal: String = "Save Money",
    val financialMode: String = "SAVE"
)

class DashboardViewModel : ViewModel() {
    private val expenseRepository = AppModule.expenseRepository
    private val incomeRepository = AppModule.incomeRepository
    private val budgetRepository = AppModule.budgetRepository
    private val savingsGoalRepository = AppModule.savingsGoalRepository
    private val importRepository = AppModule.importRepository
    private val settingsRepository = AppModule.settingsRepository
    private val authRepository = AppModule.authRepository

    val currentUser = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val expenses = expenseRepository.getExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomes = incomeRepository.getIncomes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets = budgetRepository.getBudgets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val savingsGoals = savingsGoalRepository.getSavingsGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val importHistory = importRepository.getImportHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings = settingsRepository.getUserSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    private val _timeFilter = MutableStateFlow("30D")
    val timeFilter: StateFlow<String> = _timeFilter.asStateFlow()

    fun setTimeFilter(filter: String) {
        _timeFilter.value = filter
    }

    private val coreDataFlow = combine(expenses, incomes, budgets, savingsGoals) { exps, incs, buds, goals ->
        CoreFinancialData(exps, incs, buds, goals)
    }

    val dashboardState: StateFlow<DashboardState> = combine(
        coreDataFlow,
        userSettings,
        importHistory,
        timeFilter
    ) { core, sets, historyList, filter ->

        val exps = core.expenses
        val incs = core.incomes
        val buds = core.budgets
        val goals = core.goals

        val filterDays = when (filter) {
            "7D" -> 7
            "30D" -> 30
            "3M" -> 90
            "6M" -> 180
            "1Y" -> 365
            else -> 30
        }
        val cutoffCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -filterDays) }
        val cutoffDate = cutoffCal.time

        val filteredExps = exps.filter { it.date.after(cutoffDate) || it.date == cutoffDate }

        val calendar = Calendar.getInstance()
        val daily: Map<Int, Double> = filteredExps.groupBy {
            calendar.time = it.date
            calendar.get(Calendar.DAY_OF_MONTH)
        }.mapValues { entry -> entry.value.sumOf { it.amount } }

        val totalInc = AnalysisEngine.calculateTotalIncome(incs)
        val totalExp = AnalysisEngine.calculateTotalExpenses(exps)
        val totalSav = AnalysisEngine.calculateSavings(incs, exps)
        val savRate = AnalysisEngine.calculateSavingsRate(incs, exps)
        val health = AnalysisEngine.calculateFinancialHealth(incs, exps, buds, goals)
        val snapshot = AnalysisEngine.calculateSmartSnapshot(incs, exps, buds, goals)
        val habitScore = AnalysisEngine.calculateMoneyHabitScore(incs, exps, buds, goals, sets)
        val essentialAnalysis = AnalysisEngine.calculateEssentialVsDiscretionary(exps, sets.essentialOverrides)
        val spendingLeaks = AnalysisEngine.detectSpendingLeaks(exps)
        val goalRoadmap = AnalysisEngine.generateGoalRoadmap(goals, incs, exps)
        val recurringMap = AnalysisEngine.detectRecurringMoneyMap(exps, sets.recurringOverrides)
        val alerts = AnalysisEngine.generateMoneyAlerts(incs, exps, buds, goals, sets.dismissedAlertIds)
        val weeklyReview = AnalysisEngine.generateWeeklyMoneyReview(incs, exps)

        // Real import metrics
        val lastImport = historyList.firstOrNull()
        val totalImported = historyList.sumOf { it.newRecords }
        val totalDuplicates = historyList.sumOf { it.duplicateRecords }
        val totalNeedsReview = historyList.sumOf { it.reviewRecords }

        val importSummary = DashboardImportSummary(
            lastImportDate = lastImport?.createdAt,
            lastImportFileName = lastImport?.fileName ?: "",
            transactionsImported = totalImported,
            duplicatesPrevented = totalDuplicates,
            needsReviewCount = totalNeedsReview,
            totalImportsCount = historyList.size
        )

        val topCat = AnalysisEngine.calculateCategoryTotals(exps).maxByOrNull { it.value }?.key ?: "Shopping"

        DashboardState(
            totalIncome = totalInc,
            totalExpenses = totalExp,
            savings = totalSav,
            savingsRate = savRate,
            currency = sets.currency,
            categoryTotals = AnalysisEngine.calculateCategoryTotals(exps),
            insights = AnalysisEngine.generateInsights(incs, exps, buds),
            dailySpending = daily,
            financialHealth = health,
            smartSnapshot = snapshot,
            importSummary = importSummary,
            moneyHabitScore = habitScore,
            essentialVsDiscretionary = essentialAnalysis,
            topSpendingLeaks = spendingLeaks,
            goalRoadmap = goalRoadmap,
            upcomingRecurring = recurringMap.items.take(3),
            todayInsight = weeklyReview.aiSummary.ifBlank { "Your spending is well-aligned with monthly targets." },
            oneActionToConsider = weeklyReview.oneActionForNextWeek.ifBlank { "Try keeping $topCat spending below planned targets." },
            actionCategory = weeklyReview.actionCategory,
            actionReductionAmount = weeklyReview.suggestedReductionAmount,
            activeMoneyAlerts = alerts,
            primaryGoal = sets.primaryGoal,
            financialMode = sets.financialMode
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    fun dismissAlert(alertId: String) {
        viewModelScope.launch {
            val currentSettings = userSettings.value
            val updatedDismissed = currentSettings.dismissedAlertIds + alertId
            settingsRepository.updateUserSettings(currentSettings.copy(dismissedAlertIds = updatedDismissed))
        }
    }
}

private data class CoreFinancialData(
    val expenses: List<Expense>,
    val incomes: List<Income>,
    val budgets: List<Budget>,
    val goals: List<SavingsGoal>
)
