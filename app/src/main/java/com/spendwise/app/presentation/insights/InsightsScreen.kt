package com.spendwise.app.presentation.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.spendwise.app.domain.model.MoneyAlert
import com.spendwise.app.domain.model.MoneyAlertType
import com.spendwise.app.presentation.dashboard.BottomNavigationBar
import com.spendwise.app.presentation.dashboard.DashboardViewModel
import com.spendwise.app.presentation.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(navController: NavController, viewModel: DashboardViewModel = viewModel()) {
    val state by viewModel.dashboardState.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }

    val filterOptions = listOf("All", "Budget Risk", "Opportunities", "Milestones", "Anomalies", "Recurring")

    val filteredAlerts = state.activeMoneyAlerts.filter { alert ->
        when (selectedFilter) {
            "Budget Risk" -> alert.type == MoneyAlertType.BUDGET_RISK
            "Opportunities" -> alert.type == MoneyAlertType.SPENDING_OPPORTUNITY
            "Milestones" -> alert.type == MoneyAlertType.GOAL_MILESTONE
            "Anomalies" -> alert.type == MoneyAlertType.UNUSUAL_SPENDING
            "Recurring" -> alert.type == MoneyAlertType.RECURRING_EXPENSE
            else -> true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("MONEY ALERTS & INTELLIGENCE", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Text("Real-time proactive money alerts based on actual data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            // Filter Pills
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { opt ->
                    FilterChip(
                        selected = selectedFilter == opt,
                        onClick = { selectedFilter = opt },
                        label = { Text(opt, fontSize = 12.sp) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredAlerts.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(36.dp))
                                Text("No Active Alerts", fontWeight = FontWeight.Bold)
                                Text("All monitored category budgets, recurring expenses, and goals are within healthy thresholds.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                items(filteredAlerts) { alert ->
                    MoneyAlertCard(
                        alert = alert,
                        onDismiss = { viewModel.dismissAlert(alert.id) },
                        onAction = {
                            when (alert.actionRoute) {
                                "budget" -> navController.navigate(Screen.Budget.route)
                                "savings_goals" -> navController.navigate(Screen.SavingsGoals.route)
                                "transactions" -> navController.navigate(Screen.Transactions.route)
                                "ai_assistant" -> navController.navigate(Screen.AiAssistant.route)
                                else -> navController.navigate(Screen.AiAssistant.route)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MoneyAlertCard(
    alert: MoneyAlert,
    onDismiss: () -> Unit,
    onAction: () -> Unit
) {
    val severityColor = when (alert.severity) {
        "danger" -> Color(0xFFEF4444)
        "warning" -> Color(0xFFF59E0B)
        "success" -> Color(0xFF10B981)
        else -> Color(0xFF3B82F6)
    }

    val icon = when (alert.type) {
        MoneyAlertType.BUDGET_RISK -> Icons.Default.Warning
        MoneyAlertType.SPENDING_OPPORTUNITY -> Icons.Default.Lightbulb
        MoneyAlertType.GOAL_MILESTONE -> Icons.Default.TrackChanges
        MoneyAlertType.UNUSUAL_SPENDING -> Icons.Default.TrendingUp
        MoneyAlertType.RECURRING_EXPENSE -> Icons.Default.Repeat
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = severityColor.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(icon, contentDescription = null, tint = severityColor, modifier = Modifier.size(20.dp))
                    Text(alert.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = severityColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = alert.type.name.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = severityColor,
                        fontSize = 9.sp
                    )
                }
            }

            Text(alert.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(alert.actionLabel, fontSize = 12.sp)
                }
            }
        }
    }
}
