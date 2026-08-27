package com.spendwise.app.presentation.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.spendwise.app.domain.model.*
import com.spendwise.app.presentation.dashboard.BottomNavigationBar
import com.spendwise.app.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    navController: NavController,
    viewModel: AiAssistantViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var selectedBudgetForApply by remember { mutableStateOf<AiBudgetRecommendation?>(null) }

    val tabTitles = listOf(
        "Copilot Chat",
        "What-If",
        "Can I Afford?",
        "Digital Twin",
        "Habit Score",
        "Essential/Wants",
        "Recurring Map",
        "Spending Leaks",
        "Goal Roadmap",
        "Reviews",
        "Forecast",
        "Calendar & Journey"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text("SPENDWISE FINANCIAL COPILOT", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                        Text(
                            "Understand your money. Plan your next move.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (state.selectedTab == 0 && state.messages.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearConversation() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat")
                        }
                    }
                }
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Scrollable Mode Switcher Tabs
            ScrollableTabRow(
                selectedTabIndex = state.selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = state.selectedTab == index,
                        onClick = { viewModel.setSelectedTab(index) },
                        text = {
                            Text(
                                title,
                                fontWeight = if (state.selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (state.selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            // Tab Content Router
            when (state.selectedTab) {
                0 -> CopilotChatTab(
                    messages = state.messages,
                    isLoading = state.isLoading,
                    inputText = inputText,
                    onInputChanged = { inputText = it },
                    onSend = {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    },
                    onQuestionClick = { question ->
                        viewModel.sendMessage(question)
                    }
                )
                1 -> WhatIfSimulatorTab(
                    result = state.whatIfResult,
                    currency = state.currency,
                    onSimulate = { incDelta, catDelta, recDelta ->
                        viewModel.runWhatIfSimulation(incDelta, catDelta, recDelta)
                    }
                )
                2 -> PurchaseAffordabilityTab(
                    result = state.affordabilityResult,
                    currency = state.currency,
                    onAssess = { amt, desc, cat ->
                        viewModel.assessAffordability(amt, desc, cat)
                    }
                )
                3 -> DigitalTwinTab(twin = state.digitalTwin, currency = state.currency)
                4 -> MoneyHabitScoreTab(habit = state.habitScore)
                5 -> EssentialDiscretionaryTab(analysis = state.essentialAnalysis, currency = state.currency)
                6 -> RecurringMoneyMapTab(map = state.recurringMoneyMap, currency = state.currency)
                7 -> SpendingLeaksTab(leaks = state.spendingLeaks, currency = state.currency)
                8 -> GoalRoadmapTab(goals = state.goalRoadmap, priority = state.multiGoalPriority, currency = state.currency)
                9 -> ReviewsTab(weekly = state.weeklyReview, monthEnd = state.comprehensiveReview, currency = state.currency)
                10 -> FinancialForecastTab(forecast = state.forecast, currency = state.currency)
                11 -> CalendarAndJourneyTab(calendar = state.cashflowCalendar, journey = state.moneyJourney, currency = state.currency)
            }
        }
    }
}

// =========================================================================================
// TAB 0: COPILOT CHAT
// =========================================================================================
@Composable
fun CopilotChatTab(
    messages: List<AiMessage>,
    isLoading: Boolean,
    inputText: String,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onQuestionClick: (String) -> Unit
) {
    val suggestedQuestions = listOf(
        "Can I afford a ₹10,000 purchase?",
        "Why did I spend more this month?",
        "How can I save more money?",
        "Where am I spending the most?",
        "Simulate ₹2,000 food reduction.",
        "Compare this month with last month."
    )

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(messages.size, isLoading) {
        val totalCount = messages.size + (if (isLoading) 1 else 0) + (if (messages.isEmpty()) 1 else 0)
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Suggested questions chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suggestedQuestions) { question ->
                SuggestionChip(
                    onClick = { onQuestionClick(question) },
                    label = { Text(question, fontSize = 11.sp) }
                )
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("SpendWise Financial Copilot", fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "Hello! I am your Financial Copilot. I connect your actual transactions, budgets, savings goals, and habits to help you plan your next move.\n\nAsk me anything or tap a suggestion above!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(messages) { msg ->
                val isUser = msg.sender == AiMessageSender.USER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isUser) "You" else "Financial Copilot",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Copilot is analyzing your financial data...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                placeholder = { Text("Ask SpendWise Financial Copilot...", fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )

            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

// =========================================================================================
// TAB 1: WHAT-IF SIMULATOR
// =========================================================================================
@Composable
fun WhatIfSimulatorTab(
    result: WhatIfSimulationResult,
    currency: String,
    onSimulate: (Double, Map<String, Double>, Double) -> Unit
) {
    var foodReduction by remember { mutableStateOf(2000f) }
    var shoppingReduction by remember { mutableStateOf(1000f) }
    var incomeIncrease by remember { mutableStateOf(0f) }
    var recurringBill by remember { mutableStateOf(0f) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("WHAT-IF FINANCIAL SIMULATOR", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        "Test hypothetical financial decisions before spending real money. This simulation does not alter your actual transactions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Side-by-Side Current vs Projected Cards
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("CURRENT SAVINGS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            CurrencyFormatter.format(result.currentMonthlySavings, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("${result.currentSavingsRate.toInt()}% rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("PROJECTED SAVINGS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            CurrencyFormatter.format(result.projectedMonthlySavings, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            "${if (result.savingsImprovementPct >= 0) "+" else ""}${String.format(Locale.ROOT, "%.1f", result.savingsImprovementPct)}% improvement",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Sliders
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Hypothetical Adjustments", fontWeight = FontWeight.Bold)

                    // Food Reduction Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Reduce Food Spending", fontSize = 12.sp)
                            Text("₹${foodReduction.toInt()}/mo", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = foodReduction,
                            onValueChange = {
                                foodReduction = it
                                onSimulate(incomeIncrease.toDouble(), mapOf("Food" to -foodReduction.toDouble(), "Shopping" to -shoppingReduction.toDouble()), recurringBill.toDouble())
                            },
                            valueRange = 0f..10000f,
                            steps = 19
                        )
                    }

                    // Shopping Reduction Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Reduce Shopping Spending", fontSize = 12.sp)
                            Text("₹${shoppingReduction.toInt()}/mo", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = shoppingReduction,
                            onValueChange = {
                                shoppingReduction = it
                                onSimulate(incomeIncrease.toDouble(), mapOf("Food" to -foodReduction.toDouble(), "Shopping" to -shoppingReduction.toDouble()), recurringBill.toDouble())
                            },
                            valueRange = 0f..10000f,
                            steps = 19
                        )
                    }

                    // Income Increase Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Increase Monthly Income", fontSize = 12.sp)
                            Text("+₹${incomeIncrease.toInt()}/mo", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                        Slider(
                            value = incomeIncrease,
                            onValueChange = {
                                incomeIncrease = it
                                onSimulate(incomeIncrease.toDouble(), mapOf("Food" to -foodReduction.toDouble(), "Shopping" to -shoppingReduction.toDouble()), recurringBill.toDouble())
                            },
                            valueRange = 0f..30000f,
                            steps = 29
                        )
                    }
                }
            }
        }

        // Goal shifts list
        if (result.goalShifts.isNotEmpty()) {
            item {
                Text("Impact on Active Goals", fontWeight = FontWeight.Bold)
            }
            items(result.goalShifts) { shift ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(shift.goalTitle, fontWeight = FontWeight.SemiBold)
                            Text("Timeline: ${shift.currentMonthsRemaining} mo → ${shift.projectedMonthsRemaining} mo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${shift.daysSavedOrDelayed} days saved",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================================
// TAB 2: PURCHASE AFFORDABILITY ("CAN I AFFORD THIS?")
// =========================================================================================
@Composable
fun PurchaseAffordabilityTab(
    result: PurchaseAffordabilityResult?,
    currency: String,
    onAssess: (Double, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf("10000") }
    var descriptionText by remember { mutableStateOf("New headphones") }
    var categoryText by remember { mutableStateOf("Shopping") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("CAN I AFFORD THIS?", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Purchase Amount ($currency)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = descriptionText,
                        onValueChange = { descriptionText = it },
                        label = { Text("Purchase Description") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 10000.0
                            onAssess(amt, descriptionText, categoryText)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Checklist, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Evaluate Affordability")
                    }
                }
            }
        }

        result?.let { res ->
            item {
                val ratingColor = when (res.rating) {
                    AffordabilityRating.SAFE -> Color(0xFF10B981)
                    AffordabilityRating.CAUTION -> Color(0xFFF59E0B)
                    AffordabilityRating.NOT_RECOMMENDED -> Color(0xFFEF4444)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ratingColor.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("AFFORDABILITY VERDICT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                            Surface(shape = RoundedCornerShape(8.dp), color = ratingColor) {
                                Text(
                                    text = res.verdictTitle,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Text(res.message, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)

                        Divider(color = ratingColor.copy(alpha = 0.3f))

                        Text("WHY?", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(res.explanationWhy, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Text("DATA USED:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(res.dataUsedSummary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

// =========================================================================================
// TAB 3: FINANCIAL DIGITAL TWIN
// =========================================================================================
@Composable
fun DigitalTwinTab(twin: FinancialDigitalTwin, currency: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("YOUR FINANCIAL PROFILE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                    Text("Continuously updated mathematical model representing your cashflow behavior.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // 4 Score Progress Bars
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    TwinScoreBar("Income Stability", twin.incomeStabilityPct)
                    TwinScoreBar("Savings Discipline", twin.savingsDisciplinePct)
                    TwinScoreBar("Budget Discipline", twin.budgetDisciplinePct)
                    TwinScoreBar("Spending Stability", twin.spendingStabilityPct)
                }
            }
        }

        // Key Twin Metrics
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Twin Summary Metrics", fontWeight = FontWeight.Bold)
                    TwinMetricRow("Average Monthly Income", CurrencyFormatter.format(twin.avgMonthlyIncome, currency))
                    TwinMetricRow("Average Monthly Expenses", CurrencyFormatter.format(twin.avgMonthlyExpenses, currency))
                    TwinMetricRow("Average Monthly Savings", CurrencyFormatter.format(twin.avgMonthlySavings, currency))
                    TwinMetricRow("Savings Rate", "${twin.savingsRate.toInt()}%")
                    TwinMetricRow("Essential Spending Total", CurrencyFormatter.format(twin.essentialSpendingTotal, currency))
                    TwinMetricRow("Discretionary Spending Total", CurrencyFormatter.format(twin.discretionarySpendingTotal, currency))
                }
            }
        }
    }
}

@Composable
fun TwinScoreBar(label: String, score: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("$score%", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        }
        LinearProgressIndicator(
            progress = (score / 100f).coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun TwinMetricRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

// =========================================================================================
// TAB 4: MONEY HABIT SCORE
// =========================================================================================
@Composable
fun MoneyHabitScoreTab(habit: MoneyHabitScore) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("MONEY HABIT SCORE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                    Text("${habit.score}", fontSize = 48.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
                        Text(
                            text = habit.label,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item {
            Text("Habits Breakdown & Checklist", fontWeight = FontWeight.Bold)
        }

        items(habit.bulletPoints) { bullet ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (bullet.isPositive) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (bullet.isPositive) Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(bullet.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(bullet.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// =========================================================================================
// TAB 5: ESSENTIAL VS DISCRETIONARY
// =========================================================================================
@Composable
fun EssentialDiscretionaryTab(analysis: EssentialDiscretionaryAnalysis, currency: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ESSENTIAL VS DISCRETIONARY", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                    Text(analysis.recommendation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("ESSENTIAL", style = MaterialTheme.typography.labelSmall)
                                Text(CurrencyFormatter.format(analysis.essentialTotal, currency), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("${analysis.essentialPct.toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("DISCRETIONARY", style = MaterialTheme.typography.labelSmall)
                                Text(CurrencyFormatter.format(analysis.discretionaryTotal, currency), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                Text("${analysis.discretionaryPct.toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }

        item { Text("Essential Categories (Needs)", fontWeight = FontWeight.Bold) }
        items(analysis.essentialCategories) { (cat, amt) ->
            TwinMetricRow(cat, CurrencyFormatter.format(amt, currency))
        }

        item { Text("Discretionary Categories (Wants)", fontWeight = FontWeight.Bold) }
        items(analysis.discretionaryCategories) { (cat, amt) ->
            TwinMetricRow(cat, CurrencyFormatter.format(amt, currency))
        }
    }
}

// =========================================================================================
// TAB 6: RECURRING MONEY MAP
// =========================================================================================
@Composable
fun RecurringMoneyMapTab(map: RecurringMoneyMap, currency: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("RECURRING MONEY MAP", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                    Text("Monthly: ${CurrencyFormatter.format(map.monthlyRecurringTotal, currency)}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Annual Projected: ${CurrencyFormatter.format(map.annualProjectedTotal, currency)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        items(map.items) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(item.name, fontWeight = FontWeight.Bold)
                        Text("${item.category} • ${item.frequency}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${CurrencyFormatter.format(item.monthlyAmount, currency)}/mo", fontWeight = FontWeight.Bold)
                        Text("${CurrencyFormatter.format(item.annualAmount, currency)}/yr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

// =========================================================================================
// TAB 7: SPENDING LEAKS
// =========================================================================================
@Composable
fun SpendingLeaksTab(leaks: List<SpendingLeak>, currency: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("SPENDING LEAK DETECTOR", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                    Text("Identifies repeated micro-purchases that accumulate into significant monthly amounts.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        items(leaks) { leak ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(leak.name, fontWeight = FontWeight.Bold)
                        Text("₹${leak.monthlyTotal.toInt()}/mo", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                    }
                    Text(leak.aiExplanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// =========================================================================================
// TAB 8: GOAL ROADMAP & PRIORITY
// =========================================================================================
@Composable
fun GoalRoadmapTab(goals: List<GoalRoadmapItem>, priority: MultiGoalPriorityDistribution, currency: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text("Active Goal Roadmap", fontWeight = FontWeight.Bold) }
        items(goals) { goal ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(goal.title, fontWeight = FontWeight.Bold)
                        Text("${goal.progressPct}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    LinearProgressIndicator(
                        progress = (goal.progressPct / 100f).coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Text("Target: ${goal.projectedCompletionDate}", style = MaterialTheme.typography.labelSmall)
                    Text(goal.aiSuggestion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (priority.allocations.isNotEmpty()) {
            item { Text("Multi-Goal Priority Allocations", fontWeight = FontWeight.Bold) }
            items(priority.allocations) { alloc ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(alloc.goalTitle, fontWeight = FontWeight.Bold)
                            Text("${CurrencyFormatter.format(alloc.recommendedMonthlyAmount, currency)}/mo (${alloc.allocationPercentage.toInt()}%)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(alloc.reasonWhy, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// =========================================================================================
// TAB 9: REVIEWS
// =========================================================================================
@Composable
fun ReviewsTab(weekly: WeeklyMoneyReview, monthEnd: ComprehensiveMonthEndReview, currency: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("WEEKLY MONEY REVIEW", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                    Text(weekly.aiSummary, style = MaterialTheme.typography.bodyMedium)
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surface) {
                        Text(
                            text = "Action for Next Week: ${weekly.oneActionForNextWeek}",
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("MONTH-END REVIEW: ${monthEnd.monthYear}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                    Text("What Went Well:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    monthEnd.whatWentWell.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }

                    Text("Next Month Plan:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Savings Target: ${CurrencyFormatter.format(monthEnd.nextMonthPlan.recommendedSavingsTarget, currency)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// =========================================================================================
// TAB 10: FORECAST
// =========================================================================================
@Composable
fun FinancialForecastTab(forecast: FinancialForecast, currency: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("EXPECTED NEXT MONTH (${forecast.nextMonthName})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(
                                text = "Confidence: ${forecast.confidenceLevel}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    TwinMetricRow("Expected Income", CurrencyFormatter.format(forecast.expectedIncome, currency))
                    TwinMetricRow("Expected Expenses", CurrencyFormatter.format(forecast.expectedExpenses, currency))
                    TwinMetricRow("Expected Net Savings", CurrencyFormatter.format(forecast.expectedSavings, currency))

                    Text(forecast.isEstimateDisclaimer, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

// =========================================================================================
// TAB 11: CALENDAR & JOURNEY
// =========================================================================================
@Composable
fun CalendarAndJourneyTab(calendar: List<CashflowCalendarEntry>, journey: List<MoneyJourneyMilestone>, currency: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Money Calendar (Upcoming & Historical)", fontWeight = FontWeight.Bold) }
        items(calendar) { entry ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${entry.dayOfMonth} ${entry.monthName} • ${entry.title}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(if (entry.isProjected) "Projected recurring" else "Logged record", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Text(
                        text = if (entry.type == CashflowEntryType.INCOME) "+${CurrencyFormatter.format(entry.amount, currency)}" else "-${CurrencyFormatter.format(entry.amount, currency)}",
                        fontWeight = FontWeight.Bold,
                        color = if (entry.type == CashflowEntryType.INCOME) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
            }
        }

        item { Text("Money Journey Visual Timeline", fontWeight = FontWeight.Bold) }
        items(journey) { milestone ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("${milestone.monthLabel} ${milestone.year}: ${milestone.title}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(milestone.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
