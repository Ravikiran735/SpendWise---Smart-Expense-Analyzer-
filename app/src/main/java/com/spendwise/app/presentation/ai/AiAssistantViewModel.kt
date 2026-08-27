package com.spendwise.app.presentation.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.di.AppModule
import com.spendwise.app.domain.model.*
import com.spendwise.app.domain.usecase.AiAssistantUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

data class AiAssistantUiState(
    val messages: List<AiMessage> = emptyList(),
    val isLoading: Boolean = false,
    val selectedTab: Int = 0, // 0: Chat, 1: What-If Simulator, 2: Can I Afford This?, 3: Financial Profile / Twin, 4: Habit Score, 5: Essential/Discretionary, 6: Recurring Map, 7: Spending Leaks, 8: Goal Roadmap & Priority, 9: Reviews, 10: Forecast, 11: Calendar & Journey
    val spendingAnalysis: AiSpendingAnalysis = AiSpendingAnalysis(),
    val budgetRecommendations: List<AiBudgetRecommendation> = emptyList(),
    val savingsPlan: AiSavingsPlan = AiSavingsPlan(),
    val anomalies: List<AiAnomaly> = emptyList(),
    val monthlyReview: AiMonthlyReview = AiMonthlyReview(),
    val comprehensiveReview: ComprehensiveMonthEndReview = ComprehensiveMonthEndReview(),
    val weeklyReview: WeeklyMoneyReview = WeeklyMoneyReview(),
    val whatIfResult: WhatIfSimulationResult = WhatIfSimulationResult(),
    val affordabilityResult: PurchaseAffordabilityResult? = null,
    val digitalTwin: FinancialDigitalTwin = FinancialDigitalTwin(),
    val habitScore: MoneyHabitScore = MoneyHabitScore(),
    val essentialAnalysis: EssentialDiscretionaryAnalysis = EssentialDiscretionaryAnalysis(),
    val recurringMoneyMap: RecurringMoneyMap = RecurringMoneyMap(),
    val spendingLeaks: List<SpendingLeak> = emptyList(),
    val goalRoadmap: List<GoalRoadmapItem> = emptyList(),
    val multiGoalPriority: MultiGoalPriorityDistribution = MultiGoalPriorityDistribution(),
    val forecast: FinancialForecast = FinancialForecast(),
    val cashflowCalendar: List<CashflowCalendarEntry> = emptyList(),
    val moneyJourney: List<MoneyJourneyMilestone> = emptyList(),
    val currency: String = "₹",
    val statusMessage: String? = null
)

class AiAssistantViewModel @JvmOverloads constructor(
    private val useCase: AiAssistantUseCase = AppModule.aiAssistantUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    init {
        loadConversationHistory()
        loadAllDecisionIntelligence()
    }

    private fun loadConversationHistory() {
        viewModelScope.launch {
            try {
                useCase.getConversationHistory().collect { msgs ->
                    if (msgs.isNotEmpty()) {
                        val currentList = _uiState.value.messages
                        // Combine while preserving order and uniqueness
                        val existingIds = currentList.map { it.id }.toSet()
                        val newFromFirestore = msgs.filterNot { it.id in existingIds }
                        if (currentList.isEmpty()) {
                            _uiState.value = _uiState.value.copy(messages = msgs)
                        } else if (newFromFirestore.isNotEmpty()) {
                            _uiState.value = _uiState.value.copy(messages = currentList + newFromFirestore)
                        }
                    }
                }
            } catch (e: Exception) {
                // Gracefully keep current state
            }
        }
    }

    fun setSelectedTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
        when (tabIndex) {
            0 -> Unit
            1 -> runWhatIfSimulation(0.0, mapOf("Food" to -2000.0), 0.0)
            2 -> assessAffordability(10000.0, "New Headphones", "Shopping")
            3 -> loadDigitalTwin()
            4 -> loadHabitScore()
            5 -> loadEssentialVsDiscretionary()
            6 -> loadRecurringMoneyMap()
            7 -> loadSpendingLeaks()
            8 -> {
                loadGoalRoadmap()
                loadMultiGoalPriority()
            }
            9 -> loadReviews()
            10 -> loadForecast()
            11 -> loadCalendarAndJourney()
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        val userMessage = AiMessage(
            id = UUID.randomUUID().toString(),
            sender = AiMessageSender.USER,
            text = trimmed,
            timestamp = Date()
        )

        // Optimistically display the user's message immediately
        val currentMessages = _uiState.value.messages
        _uiState.value = _uiState.value.copy(
            messages = currentMessages + userMessage,
            isLoading = true,
            statusMessage = null
        )

        viewModelScope.launch {
            try {
                val aiResponse = useCase.sendMessage(trimmed)
                val updatedList = _uiState.value.messages
                val alreadyAdded = updatedList.any { it.id == aiResponse.id || (it.sender == AiMessageSender.AI && it.text == aiResponse.text) }
                if (!alreadyAdded) {
                    _uiState.value = _uiState.value.copy(
                        messages = updatedList + aiResponse
                    )
                }
            } catch (e: Exception) {
                val fallbackResponse = AiMessage(
                    id = UUID.randomUUID().toString(),
                    sender = AiMessageSender.AI,
                    text = "I encountered an issue connecting to the cloud service. However, your local financial data remains safe. Please try again or tap one of the suggested prompts.",
                    timestamp = Date()
                )
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + fallbackResponse,
                    statusMessage = "Notice: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun clearConversation() {
        _uiState.value = _uiState.value.copy(messages = emptyList())
        viewModelScope.launch {
            try {
                useCase.clearConversation()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun loadAllDecisionIntelligence() {
        loadSpendingAnalysis()
        loadBudgetRecommendations()
        loadDigitalTwin()
        loadHabitScore()
        loadEssentialVsDiscretionary()
        loadRecurringMoneyMap()
        loadSpendingLeaks()
        loadGoalRoadmap()
        loadMultiGoalPriority()
        loadReviews()
        loadForecast()
        loadCalendarAndJourney()
        runWhatIfSimulation(0.0, mapOf("Food" to -2000.0), 0.0)
        assessAffordability(10000.0, "New Headphones", "Shopping")
    }

    fun runWhatIfSimulation(
        incomeDelta: Double,
        categoryDeltas: Map<String, Double>,
        recurringDelta: Double
    ) {
        viewModelScope.launch {
            try {
                val result = useCase.simulateWhatIf(incomeDelta, categoryDeltas, recurringDelta)
                _uiState.value = _uiState.value.copy(whatIfResult = result)
            } catch (e: Exception) {
                // Keep default state
            }
        }
    }

    fun assessAffordability(amount: Double, description: String, category: String) {
        viewModelScope.launch {
            try {
                val result = useCase.assessPurchaseAffordability(amount, description, category)
                _uiState.value = _uiState.value.copy(affordabilityResult = result)
            } catch (e: Exception) {
                // Keep default state
            }
        }
    }

    fun loadDigitalTwin() {
        viewModelScope.launch {
            try {
                val twin = useCase.getFinancialDigitalTwin()
                _uiState.value = _uiState.value.copy(digitalTwin = twin)
            } catch (e: Exception) {
                // Keep default state
            }
        }
    }

    fun loadHabitScore() {
        viewModelScope.launch {
            try {
                val habit = useCase.getMoneyHabitScore()
                _uiState.value = _uiState.value.copy(habitScore = habit)
            } catch (e: Exception) {
                // Keep default state
            }
        }
    }

    fun loadEssentialVsDiscretionary() {
        viewModelScope.launch {
            try {
                val essential = useCase.getEssentialVsDiscretionary()
                _uiState.value = _uiState.value.copy(essentialAnalysis = essential)
            } catch (e: Exception) {
                // Keep default state
            }
        }
    }

    fun loadRecurringMoneyMap() {
        viewModelScope.launch {
            try {
                val map = useCase.getRecurringMoneyMap()
                _uiState.value = _uiState.value.copy(recurringMoneyMap = map)
            } catch (e: Exception) {
                // Keep default state
            }
        }
    }

    fun loadSpendingLeaks() {
        viewModelScope.launch {
            try {
                val leaks = useCase.getSpendingLeaks()
                _uiState.value = _uiState.value.copy(spendingLeaks = leaks)
            } catch (e: Exception) {
                // Keep default state
            }
        }
    }

    fun loadGoalRoadmap() {
        viewModelScope.launch {
            try {
                val roadmap = useCase.getGoalRoadmap()
                _uiState.value = _uiState.value.copy(goalRoadmap = roadmap)
            } catch (e: Exception) {
                // Keep default state
            }
        }
    }

    fun loadMultiGoalPriority() {
        viewModelScope.launch {
            try {
                val priority = useCase.getMultiGoalPriority()
                _uiState.value = _uiState.value.copy(multiGoalPriority = priority)
            } catch (e: Exception) {
                // Keep default state
            }
        }
    }

    fun loadReviews() {
        viewModelScope.launch {
            try {
                val weekly = useCase.getWeeklyMoneyReview()
                val comp = useCase.getComprehensiveMonthEndReview()
                _uiState.value = _uiState.value.copy(weeklyReview = weekly, comprehensiveReview = comp)
            } catch (e: Exception) {
                // Keep default state
            }
        }
    }

    fun loadForecast() {
        viewModelScope.launch {
            try {
                val fc = useCase.getFinancialForecast()
                _uiState.value = _uiState.value.copy(forecast = fc)
            } catch (e: Exception) {
                // Keep default state
            }
        }
    }

    fun loadCalendarAndJourney() {
        viewModelScope.launch {
            try {
                val cal = useCase.getCashflowCalendar()
                val journey = useCase.getMoneyJourneyTimeline()
                _uiState.value = _uiState.value.copy(cashflowCalendar = cal, moneyJourney = journey)
            } catch (e: Exception) {
                // Keep default state
            }
        }
    }

    fun loadSpendingAnalysis() {
        viewModelScope.launch {
            try {
                val analysis = useCase.getSpendingAnalysis()
                _uiState.value = _uiState.value.copy(spendingAnalysis = analysis)
            } catch (e: Exception) {
                // Keep default state
            }
        }
    }

    fun loadBudgetRecommendations() {
        viewModelScope.launch {
            try {
                val recs = useCase.getBudgetRecommendations()
                _uiState.value = _uiState.value.copy(budgetRecommendations = recs)
            } catch (e: Exception) {
                // Keep default state
            }
        }
    }

    fun applyBudgetRecommendation(recommendation: AiBudgetRecommendation, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val res = useCase.applyBudgetRecommendation(recommendation)
                if (res.isSuccess) {
                    _uiState.value = _uiState.value.copy(statusMessage = "Budget for ${recommendation.category} updated to ₹${recommendation.recommendedAmount.toInt()}!")
                    loadBudgetRecommendations()
                    onComplete()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(statusMessage = "Failed to update budget: ${e.message}")
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }
}
