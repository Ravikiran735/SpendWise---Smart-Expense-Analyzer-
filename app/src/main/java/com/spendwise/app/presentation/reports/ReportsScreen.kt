package com.spendwise.app.presentation.reports

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.spendwise.app.presentation.dashboard.DashboardViewModel
import com.spendwise.app.utils.CsvExporter
import com.spendwise.app.utils.CurrencyFormatter
import com.spendwise.app.utils.ReportGenerator
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(navController: NavController, viewModel: DashboardViewModel = viewModel()) {
    val context = LocalContext.current
    val expenses by viewModel.expenses.collectAsState()
    val incomes by viewModel.incomes.collectAsState()
    val dashboardState by viewModel.dashboardState.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val currency = userSettings.currency

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Executive Financial Statement", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Statement Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Audit & Statement Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Text("Official consolidated statement of all recorded credits, debits, budgets, and savings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f))
                }
            }

            // Executive Metrics Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Financial Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                    ReportMetricRow("Total Incomes", "+${CurrencyFormatter.format(dashboardState.totalIncome, currency)}", MaterialTheme.colorScheme.secondary)
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ReportMetricRow("Total Expenditures", "-${CurrencyFormatter.format(dashboardState.totalExpenses, currency)}", MaterialTheme.colorScheme.error)
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ReportMetricRow("Net Savings", CurrencyFormatter.format(dashboardState.savings, currency), MaterialTheme.colorScheme.primary)
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ReportMetricRow("Savings Rate", "${dashboardState.savingsRate.toInt()}%", MaterialTheme.colorScheme.primary)
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ReportMetricRow("Total Transaction Count", "${expenses.size + incomes.size} entries", MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Category Breakdown Table
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Expense Breakdown by Category", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                    if (dashboardState.categoryTotals.isEmpty()) {
                        Text("No categorized expenses.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        dashboardState.categoryTotals.forEach { (cat, amount) ->
                            val pct = if (dashboardState.totalExpenses > 0) (amount / dashboardState.totalExpenses * 100).toInt() else 0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "$cat ($pct%)", style = MaterialTheme.typography.bodyMedium)
                                Text(text = CurrencyFormatter.format(amount, currency), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // Transaction Source Breakdown Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Transaction Source Audit", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                    val sources = listOf("MANUAL", "CSV", "EXCEL", "RECEIPT", "VOICE")
                    val allItems = expenses.map { it.source } + incomes.map { it.origin }
                    val totalCount = allItems.size.coerceAtLeast(1)

                    sources.forEach { src ->
                        val count = allItems.count { it.equals(src, ignoreCase = true) }
                        val pct = (count.toDouble() / totalCount * 100).toInt()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "$src ($pct%)", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "$count entries", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Export Actions
            Text("Export & Distribution", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

            Button(
                onClick = {
                    val file = ReportGenerator.generatePdfReport(context, expenses, dashboardState.totalIncome)
                    shareFile(context, file, "application/pdf")
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Printable PDF Statement")
            }

            OutlinedButton(
                onClick = {
                    val file = CsvExporter.exportToCsv(context, expenses, incomes)
                    shareFile(context, file, "text/csv")
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.TableChart, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Raw Ledger CSV")
            }
        }
    }
}

@Composable
fun ReportMetricRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontWeight = FontWeight.Bold, color = valueColor, style = MaterialTheme.typography.bodyMedium)
    }
}

fun shareFile(context: Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Report"))
}
