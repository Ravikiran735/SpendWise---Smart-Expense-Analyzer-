package com.spendwise.app.presentation.income

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
import com.spendwise.app.domain.model.IncomeSource
import com.spendwise.app.presentation.dashboard.DashboardViewModel
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditIncomeScreen(
    navController: NavController,
    incomeId: String,
    viewModel: IncomeViewModel = viewModel(),
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val incomes by dashboardViewModel.incomes.collectAsState()
    val income = incomes.find { it.id == incomeId }

    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var source by remember { mutableStateOf(IncomeSource.SALARY.displayName) }

    // Synchronize form when income is loaded/updated
    LaunchedEffect(income) {
        if (income != null) {
            amount = income.amount.toString()
            description = income.description
            source = income.source
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Income") },
            text = { Text("Are you sure you want to delete this income entry?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteIncome(incomeId)
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
                title = { Text("Edit Income") },
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

            Text("Source", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            IncomeSource.values().forEach { src ->
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    RadioButton(
                        selected = source == src.displayName,
                        onClick = { source = src.displayName }
                    )
                    Text(src.displayName)
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
                    if (amt > 0 && income != null) {
                        viewModel.updateIncome(income.copy(
                            amount = amt,
                            source = source,
                            description = description,
                            updatedAt = Date()
                        ))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is IncomeUiState.Loading
            ) {
                if (uiState is IncomeUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Update Income")
                }
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is IncomeUiState.Success) {
            viewModel.resetState()
            navController.popBackStack()
        }
    }
}
