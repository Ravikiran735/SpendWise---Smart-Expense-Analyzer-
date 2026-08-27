package com.spendwise.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.navigation.compose.currentBackStackEntryAsState
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.spendwise.app.domain.model.*
import com.spendwise.app.presentation.navigation.Screen
import com.spendwise.app.utils.CurrencyFormatter
import com.spendwise.app.utils.FinancialHealth
import com.spendwise.app.utils.SmartSnapshot
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, viewModel: DashboardViewModel = viewModel()) {
    val state by viewModel.dashboardState.collectAsState()
    val goals by viewModel.savingsGoals.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val incomes by viewModel.incomes.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val selectedTimeFilter by viewModel.timeFilter.collectAsState()

    val currency = state.currency
    val balance = state.totalIncome - state.totalExpenses

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val firstName = user?.name?.split(" ")?.firstOrNull()?.ifBlank { "there" } ?: "there"

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddExpense.route) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense", modifier = Modifier.size(24.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. PERSONALIZED COMMAND CENTER HEADER
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "$greeting, $firstName",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${state.financialMode} MODE",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            text = "Here's what matters about your money today.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                        IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // 2. NET BALANCE & DUAL SCORE (Health & Money Habit Score)
            item {
                DualScoreBalanceCard(
                    balance = balance,
                    totalIncome = state.totalIncome,
                    totalExpenses = state.totalExpenses,
                    savings = state.savings,
                    savingsRate = state.savingsRate,
                    currency = currency,
                    health = state.financialHealth,
                    habitScore = state.moneyHabitScore
                )
            }

            // 3. ESSENTIAL VS DISCRETIONARY SPENDING MINI-CARD
            item {
                EssentialVsDiscretionaryDashboardCard(
                    analysis = state.essentialVsDiscretionary,
                    currency = currency,
                    onViewDetails = { navController.navigate(Screen.AiAssistant.route) }
                )
            }

            // 4. TODAY'S MONEY INSIGHT & ONE ACTION TO CONSIDER (WITH [SIMULATE])
            item {
                TodayInsightAndActionCard(
                    insight = state.todayInsight,
                    actionText = state.oneActionToConsider,
                    onSimulateClick = { navController.navigate(Screen.AiAssistant.route) }
                )
            }

            // 5. SPENDWISE FINANCIAL COPILOT HERO
            item {
                FinancialCopilotHeroCard(
                    onAskCopilot = { navController.navigate(Screen.AiAssistant.route) },
                    onSimulate = { navController.navigate(Screen.AiAssistant.route) },
                    onAffordability = { navController.navigate(Screen.AiAssistant.route) }
                )
            }

            // 6. SMART IMPORT HERO CARD
            item {
                SmartImportHeroCard(
                    summary = state.importSummary,
                    onImportClick = { navController.navigate(Screen.SmartImport.route) },
                    onScanReceiptClick = { navController.navigate("camera") },
                    onVoiceClick = { navController.navigate(Screen.AddExpense.route) }
                )
            }

            // 7. UPCOMING MONEY (Salary, Rent, Subscriptions)
            if (state.upcomingRecurring.isNotEmpty()) {
                item {
                    UpcomingMoneyCard(
                        items = state.upcomingRecurring,
                        currency = currency,
                        onViewCalendar = { navController.navigate(Screen.AiAssistant.route) }
                    )
                }
            }

            // 8. GOAL ROADMAP SUMMARY
            if (state.goalRoadmap.isNotEmpty()) {
                item {
                    GoalRoadmapDashboardCard(
                        goals = state.goalRoadmap,
                        currency = currency,
                        onViewGoals = { navController.navigate(Screen.SavingsGoals.route) }
                    )
                }
            }

            // 9. SPENDING LEAKS ALERT CARD
            if (state.topSpendingLeaks.isNotEmpty()) {
                item {
                    SpendingLeaksDashboardCard(
                        leaks = state.topSpendingLeaks,
                        currency = currency,
                        onViewLeaks = { navController.navigate(Screen.AiAssistant.route) }
                    )
                }
            }

            // 10. QUICK ACTIONS ROW
            item {
                QuickActionsRow(
                    onAddExpense = { navController.navigate(Screen.AddExpense.route) },
                    onAddIncome = { navController.navigate(Screen.AddIncome.route) },
                    onScanReceipt = { navController.navigate("camera") },
                    onReports = { navController.navigate(Screen.Reports.route) }
                )
            }

            // 11. WHERE YOUR MONEY GOES
            item {
                SpendingFlowCard(
                    categoryTotals = state.categoryTotals,
                    totalExpenses = state.totalExpenses,
                    currency = currency
                )
            }

            // 12. INCOME VS EXPENSE TREND
            item {
                TrendChartCard(
                    dailySpending = state.dailySpending,
                    selectedFilter = selectedTimeFilter,
                    onFilterChange = { viewModel.setTimeFilter(it) }
                )
            }

            // 13. BUDGET HEALTH OVERVIEW
            item {
                BudgetHealthOverviewCard(
                    budgets = budgets,
                    expenses = expenses,
                    currency = currency,
                    onViewAll = { navController.navigate(Screen.Budget.route) }
                )
            }

            // 14. RECENT TRANSACTIONS
            item {
                RecentTransactionsCard(
                    expenses = expenses,
                    incomes = incomes,
                    currency = currency,
                    navController = navController
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// -----------------------------------------------------------------------------------------
// COMPONENT 1: Dual Score & Balance Card
// -----------------------------------------------------------------------------------------
@Composable
fun DualScoreBalanceCard(
    balance: Double,
    totalIncome: Double,
    totalExpenses: Double,
    savings: Double,
    savingsRate: Double,
    currency: String,
    health: FinancialHealth,
    habitScore: MoneyHabitScore
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NET BALANCE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                    ) {
                        Text(
                            text = "Health: ${health.score}/100",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                    ) {
                        Text(
                            text = "Habits: ${habitScore.score}/100",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Text(
                text = CurrencyFormatter.format(balance, currency),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 32.sp
            )

            Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BalanceMiniMetric(label = "Income", value = "+${CurrencyFormatter.format(totalIncome, currency)}", color = MaterialTheme.colorScheme.secondary)
                BalanceMiniMetric(label = "Expenses", value = "-${CurrencyFormatter.format(totalExpenses, currency)}", color = MaterialTheme.colorScheme.error)
                BalanceMiniMetric(label = "Savings", value = CurrencyFormatter.format(savings, currency), color = MaterialTheme.colorScheme.onPrimaryContainer)
                BalanceMiniMetric(label = "Savings Rate", value = "${savingsRate.toInt()}%", color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// COMPONENT 2: Essential vs Discretionary Dashboard Card
// -----------------------------------------------------------------------------------------
@Composable
fun EssentialVsDiscretionaryDashboardCard(
    analysis: EssentialDiscretionaryAnalysis,
    currency: String,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPENDING CLASSIFICATION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "View Breakdown →",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("ESSENTIAL (Needs)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = CurrencyFormatter.format(analysis.essentialTotal, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("${analysis.essentialPct.toInt()}% of total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("DISCRETIONARY (Wants)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = CurrencyFormatter.format(analysis.discretionaryTotal, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text("${analysis.discretionaryPct.toInt()}% of total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// COMPONENT 3: Today's Money Insight & One Action to Consider
// -----------------------------------------------------------------------------------------
@Composable
fun TodayInsightAndActionCard(
    insight: String,
    actionText: String,
    onSimulateClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                Text(
                    text = "TODAY'S MONEY INSIGHT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "\"$insight\"",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ONE ACTION TO CONSIDER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = actionText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onSimulateClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simulate", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// COMPONENT 4: Financial Copilot Hero Card
// -----------------------------------------------------------------------------------------
@Composable
fun FinancialCopilotHeroCard(
    onAskCopilot: () -> Unit,
    onSimulate: () -> Unit,
    onAffordability: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Column {
                        Text("SPENDWISE FINANCIAL COPILOT", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        Text("Understand your money. Plan your next move.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAskCopilot,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ask Copilot", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onSimulate,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("What-If", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onAffordability,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Can I Afford?", fontSize = 11.sp)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// COMPONENT 5: Upcoming Money Card
// -----------------------------------------------------------------------------------------
@Composable
fun UpcomingMoneyCard(
    items: List<RecurringMoneyItem>,
    currency: String,
    onViewCalendar: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewCalendar() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UPCOMING MONEY",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Money Calendar →",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            items.take(3).forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(item.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        text = "-${CurrencyFormatter.format(item.monthlyAmount, currency)}/mo",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// COMPONENT 6: Goal Roadmap Dashboard Card
// -----------------------------------------------------------------------------------------
@Composable
fun GoalRoadmapDashboardCard(
    goals: List<GoalRoadmapItem>,
    currency: String,
    onViewGoals: () -> Unit
) {
    val topGoal = goals.firstOrNull() ?: return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewGoals() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.TrackChanges, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text("GOAL ROADMAP: ${topGoal.title.uppercase()}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                }
                Text("${topGoal.progressPct}%", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            }

            LinearProgressIndicator(
                progress = (topGoal.progressPct / 100f).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${CurrencyFormatter.format(topGoal.currentAmount, currency)} saved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Target: ${topGoal.projectedCompletionDate}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// COMPONENT 7: Spending Leaks Dashboard Card
// -----------------------------------------------------------------------------------------
@Composable
fun SpendingLeaksDashboardCard(
    leaks: List<SpendingLeak>,
    currency: String,
    onViewLeaks: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewLeaks() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                    Text("SPENDING LEAKS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                }
                Text("Manage →", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }

            leaks.take(2).forEach { leak ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${leak.name} (${leak.frequencyCount}x)", style = MaterialTheme.typography.bodySmall)
                    Text("~${CurrencyFormatter.format(leak.monthlyTotal, currency)}/mo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = Color(0xFFEF4444))
                }
            }
        }
    }
}

@Composable
fun SmartImportHeroCard(
    summary: DashboardImportSummary,
    onImportClick: () -> Unit,
    onScanReceiptClick: () -> Unit,
    onVoiceClick: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.ROOT) }
    val lastImportText = if (summary.lastImportDate != null) {
        "${sdf.format(summary.lastImportDate)} (${summary.lastImportFileName.ifBlank { "Session" }})"
    } else {
        "No imports yet"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onImportClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "SMART IMPORT",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${summary.totalImportsCount} Imports",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text(
                text = "Bring your financial records into SpendWise and let SpendWise organize and analyze them automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onImportClick,
                    modifier = Modifier.weight(1.2f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import Files", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onScanReceiptClick,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Receipt", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onVoiceClick,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Voice", fontSize = 12.sp)
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Last Import", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontSize = 9.sp)
                    Text(lastImportText, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Imported", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontSize = 9.sp)
                    Text("${summary.transactionsImported}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF10B981))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Duplicates Blocked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontSize = 9.sp)
                    Text("${summary.duplicatesPrevented}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF6B7280))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Needs Review", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontSize = 9.sp)
                    Text("${summary.needsReviewCount}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFF59E0B))
                }
            }
        }
    }
}

@Composable
fun BalanceMiniMetric(label: String, value: String, color: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun QuickActionsRow(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onScanReceipt: () -> Unit,
    onReports: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onAddExpense,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Expense", fontSize = 12.sp)
        }

        OutlinedButton(
            onClick = onAddIncome,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Income", fontSize = 12.sp)
        }

        OutlinedButton(
            onClick = onScanReceipt,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Receipt", fontSize = 12.sp)
        }

        OutlinedButton(
            onClick = onReports,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reports", fontSize = 12.sp)
        }
    }
}

@Composable
fun SpendingFlowCard(
    categoryTotals: Map<String, Double>,
    totalExpenses: Double,
    currency: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WHERE YOUR MONEY GOES",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.format(totalExpenses, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (categoryTotals.isEmpty()) {
                Text(
                    text = "No expenses recorded for this period",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                categoryTotals.entries.sortedByDescending { it.value }.take(5).forEach { (category, amount) ->
                    val percentage = if (totalExpenses > 0) (amount / totalExpenses) * 100 else 0.0
                    CategoryFlowItem(
                        category = category,
                        amount = amount,
                        percentage = percentage,
                        currency = currency
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryFlowItem(
    category: String,
    amount: Double,
    percentage: Double,
    currency: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = "${CurrencyFormatter.format(amount, currency)} (${percentage.toInt()}%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        LinearProgressIndicator(
            progress = (percentage / 100f).toFloat().coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendChartCard(
    dailySpending: Map<Int, Double>,
    selectedFilter: String,
    onFilterChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPENDING TREND",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("7D", "30D", "3M").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { onFilterChange(filter) },
                            label = { Text(filter, fontSize = 10.sp) }
                        )
                    }
                }
            }

            if (dailySpending.isNotEmpty()) {
                val chartEntries = dailySpending.entries.sortedBy { it.key }.map { it.key.toFloat() to it.value.toFloat() }
                val safeEntries = if (chartEntries.size == 1) {
                    val single = chartEntries.first()
                    val anchorX = if (single.first > 1f) single.first - 1f else single.first + 1f
                    if (single.first > 1f) listOf(anchorX to 0f, single) else listOf(single, anchorX to 0f)
                } else {
                    chartEntries
                }

                val entryModel = entryModelOf(*safeEntries.map { it.first to it.second }.toTypedArray())

                Chart(
                    chart = lineChart(),
                    model = entryModel,
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No trend data available for this range", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun BudgetHealthOverviewCard(
    budgets: List<Budget>,
    expenses: List<Expense>,
    currency: String,
    onViewAll: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewAll() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BUDGET HEALTH OVERVIEW",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "View All →",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (budgets.isEmpty()) {
                Text("No budgets configured yet. Tap to set up category caps.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                budgets.take(3).forEach { budget ->
                    val spent = expenses.filter { it.category == budget.category }.sumOf { it.amount }
                    val progress = if (budget.amount > 0) (spent / budget.amount) else 0.0
                    val isExceeded = spent > budget.amount

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(budget.category, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${CurrencyFormatter.format(spent, currency)} / ${CurrencyFormatter.format(budget.amount, currency)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LinearProgressIndicator(
                            progress = progress.toFloat().coerceIn(0f, 1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentTransactionsCard(
    expenses: List<Expense>,
    incomes: List<Income>,
    currency: String,
    navController: NavController
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT ACTIVITY",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "See All →",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { navController.navigate(Screen.Transactions.route) }
                )
            }

            if (expenses.isEmpty() && incomes.isEmpty()) {
                Text("No recent activity found", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                expenses.sortedByDescending { it.date }.take(4).forEach { exp ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(exp.description.ifBlank { exp.category }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(exp.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = "-${CurrencyFormatter.format(exp.amount, currency)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        tonalElevation = 8.dp,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard", modifier = Modifier.size(22.dp)) },
            label = { Text("Dashboard", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
            selected = currentRoute == Screen.Dashboard.route,
            onClick = {
                if (currentRoute != Screen.Dashboard.route) {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Transactions", modifier = Modifier.size(22.dp)) },
            label = { Text("Ledger", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
            selected = currentRoute == Screen.Transactions.route,
            onClick = {
                if (currentRoute != Screen.Transactions.route) {
                    navController.navigate(Screen.Transactions.route)
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.CloudUpload, contentDescription = "Import", modifier = Modifier.size(22.dp)) },
            label = { Text("Import", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
            selected = currentRoute == Screen.SmartImport.route,
            onClick = {
                if (currentRoute != Screen.SmartImport.route) {
                    navController.navigate(Screen.SmartImport.route)
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Copilot", modifier = Modifier.size(22.dp)) },
            label = { Text("Copilot", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
            selected = currentRoute == Screen.AiAssistant.route,
            onClick = {
                if (currentRoute != Screen.AiAssistant.route) {
                    navController.navigate(Screen.AiAssistant.route)
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Budgets", modifier = Modifier.size(22.dp)) },
            label = { Text("Budgets", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
            selected = currentRoute == Screen.Budget.route,
            onClick = {
                if (currentRoute != Screen.Budget.route) {
                    navController.navigate(Screen.Budget.route)
                }
            }
        )
    }
}
