package com.spendwise.app.presentation.budget

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
import com.spendwise.app.domain.model.Budget
import com.spendwise.app.domain.model.ExpenseCategory
import com.spendwise.app.presentation.dashboard.BottomNavigationBar
import com.spendwise.app.presentation.dashboard.DashboardViewModel
import com.spendwise.app.utils.CurrencyFormatter
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(navController: NavController, viewModel: DashboardViewModel = viewModel()) {
    val budgets by viewModel.budgets.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val currency = userSettings.currency

    val coroutineScope = rememberCoroutineScope()
    val budgetRepo = remember { com.spendwise.app.di.AppModule.budgetRepository }

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.FOOD.displayName) }
    var budgetAmount by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val categories = ExpenseCategory.values().map { it.displayName }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets & Limits", fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = { BottomNavigationBar(navController) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Budget") },
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
                        Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Monthly Budget Guard", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("Alerts trigger when spending reaches 80% and exceeds 100% of category allocation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (budgets.isEmpty()) {
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
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text("No Category Budgets Created", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Set monthly spending limits for categories to stay in control.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { showCreateDialog = true }) {
                                Text("Create Budget")
                            }
                        }
                    }
                }
            } else {
                items(budgets) { budget ->
                    val spent = expenses.filter { it.category == budget.category }.sumOf { it.amount }
                    BudgetItemCard(
                        budget = budget,
                        spent = spent,
                        currency = currency,
                        onDelete = {
                            coroutineScope.launch {
                                budgetRepo.deleteBudget(budget.id)
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(60.dp)) }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Category Budget") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Category", style = MaterialTheme.typography.labelMedium)
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = budgetAmount,
                        onValueChange = { budgetAmount = it },
                        label = { Text("Monthly Limit (${CurrencyFormatter.getSymbol(currency)})") },
                        placeholder = { Text("e.g. 5000") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = budgetAmount.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            isSubmitting = true
                            coroutineScope.launch {
                                val cal = Calendar.getInstance()
                                budgetRepo.addBudget(
                                    Budget(
                                        category = selectedCategory,
                                        amount = amount,
                                        month = cal.get(Calendar.MONTH) + 1,
                                        year = cal.get(Calendar.YEAR)
                                    )
                                )
                                isSubmitting = false
                                showCreateDialog = false
                                budgetAmount = ""
                            }
                        }
                    },
                    enabled = !isSubmitting && budgetAmount.isNotEmpty()
                ) {
                    Text("Save Budget")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BudgetItemCard(
    budget: Budget,
    spent: Double,
    currency: String,
    onDelete: () -> Unit
) {
    val progress = if (budget.amount > 0) (spent / budget.amount).toFloat() else 0f
    val isExceeded = progress >= 1.0f
    val isWarning = progress >= 0.8f && !isExceeded

    val progressColor = when {
        isExceeded -> MaterialTheme.colorScheme.error
        isWarning -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }

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
                    Text(text = budget.category, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (isExceeded) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("EXCEEDED", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    } else if (isWarning) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("WARNING", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                }
            }

            LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent: ${CurrencyFormatter.format(spent, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Limit: ${CurrencyFormatter.format(budget.amount, currency)} (${(progress * 100).toInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
