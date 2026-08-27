package com.spendwise.app.presentation.expense

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.spendwise.app.domain.model.ExpenseCategory
import com.spendwise.app.domain.model.PaymentMethod
import com.spendwise.app.presentation.dashboard.DashboardViewModel
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseScreen(
    navController: NavController,
    expenseId: String,
    viewModel: ExpenseViewModel = viewModel(),
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val expenses by dashboardViewModel.expenses.collectAsState()
    val expense = expenses.find { it.id == expenseId }

    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExpenseCategory.FOOD.displayName) }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH.displayName) }

    // Synchronize form when expense is loaded/updated
    LaunchedEffect(expense) {
        if (expense != null) {
            amount = expense.amount.toString()
            description = expense.description
            category = expense.category
            paymentMethod = expense.paymentMethod
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Expense") },
            text = { Text("Are you sure you want to delete this expense?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteExpense(expenseId)
                    showDeleteDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Expense") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text("Category", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            ExpenseCategory.values().forEach { cat ->
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    RadioButton(
                        selected = category == cat.displayName,
                        onClick = { category = cat.displayName }
                    )
                    Text(cat.displayName)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Text("Payment Method", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            PaymentMethod.values().forEach { pm ->
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    RadioButton(
                        selected = paymentMethod == pm.displayName,
                        onClick = { paymentMethod = pm.displayName }
                    )
                    Text(pm.displayName)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && expense != null) {
                        viewModel.updateExpense(expense.copy(
                            amount = amt,
                            category = category,
                            description = description,
                            paymentMethod = paymentMethod,
                            updatedAt = Date()
                        ))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is ExpenseUiState.Loading
            ) {
                if (uiState is ExpenseUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Update Expense")
                }
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is ExpenseUiState.Success) {
            viewModel.resetState()
            navController.popBackStack()
        }
    }
}
