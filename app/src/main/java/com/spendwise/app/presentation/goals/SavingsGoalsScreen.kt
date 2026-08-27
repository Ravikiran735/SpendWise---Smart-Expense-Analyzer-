package com.spendwise.app.presentation.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.spendwise.app.domain.model.SavingsGoal
import com.spendwise.app.presentation.dashboard.DashboardViewModel
import com.spendwise.app.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    navController: NavController,
    viewModel: DashboardViewModel = viewModel(),
    goalViewModel: SavingsGoalViewModel = viewModel()
) {
    val goals by viewModel.savingsGoals.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val currency = userSettings.currency

    var showAddDialog by remember { mutableStateOf(false) }
    var addFundsTarget by remember { mutableStateOf<SavingsGoal?>(null) }
    var fundsAmount by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Savings Goals", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Goal") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.TrackChanges, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Target Milestones", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("Track progress towards major purchases, emergency funds, and investment targets.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (goals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.TrackChanges,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text("No Savings Goals Set", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Create a goal to track your milestone progress.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { showAddDialog = true }) {
                                Text("Create First Goal")
                            }
                        }
                    }
                }
            } else {
                items(goals) { goal ->
                    GoalCardItem(
                        goal = goal,
                        currency = currency,
                        onAddFunds = { addFundsTarget = goal },
                        onDelete = {
                            goalViewModel.deleteGoal(goal.id)
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(60.dp)) }
        }

        if (showAddDialog) {
            AddGoalDialog(
                currency = currency,
                onDismiss = { showAddDialog = false },
                onConfirm = { title, target ->
                    goalViewModel.addGoal(title, target, null)
                    showAddDialog = false
                }
            )
        }

        addFundsTarget?.let { targetGoal ->
            AlertDialog(
                onDismissRequest = { addFundsTarget = null },
                title = { Text("Deposit Funds into '${targetGoal.title}'") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Enter amount to add to current saved balance:", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = fundsAmount,
                            onValueChange = { fundsAmount = it },
                            label = { Text("Deposit Amount (${CurrencyFormatter.getSymbol(currency)})") },
                            placeholder = { Text("e.g. 5000") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val addVal = fundsAmount.toDoubleOrNull() ?: 0.0
                            if (addVal > 0) {
                                goalViewModel.updateGoal(targetGoal.copy(currentAmount = targetGoal.currentAmount + addVal))
                                addFundsTarget = null
                                fundsAmount = ""
                            }
                        },
                        enabled = fundsAmount.isNotEmpty()
                    ) {
                        Text("Deposit")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { addFundsTarget = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun GoalCardItem(
    goal: SavingsGoal,
    currency: String,
    onAddFunds: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
    val isComplete = progress >= 1.0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = goal.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (isComplete) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("COMPLETED", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isComplete) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Saved: ${CurrencyFormatter.format(goal.currentAmount, currency)} / ${CurrencyFormatter.format(goal.targetAmount, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
                    Text(
                        text = if (remaining > 0) "${CurrencyFormatter.format(remaining, currency)} remaining" else "Goal achieved! 🎉",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!isComplete) {
                    FilledTonalButton(
                        onClick = onAddFunds,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Funds", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AddGoalDialog(
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Savings Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title") },
                    placeholder = { Text("e.g. Emergency Fund, New Laptop") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Target Amount (${CurrencyFormatter.getSymbol(currency)})") },
                    placeholder = { Text("e.g. 50000") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, target.toDoubleOrNull() ?: 0.0) },
                enabled = title.isNotBlank() && target.isNotBlank()
            ) {
                Text("Create Goal")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
