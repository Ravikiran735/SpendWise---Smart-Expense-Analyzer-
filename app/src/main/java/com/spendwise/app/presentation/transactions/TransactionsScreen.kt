package com.spendwise.app.presentation.transactions

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
import com.spendwise.app.domain.model.Expense
import com.spendwise.app.domain.model.Income
import com.spendwise.app.presentation.dashboard.BottomNavigationBar
import com.spendwise.app.presentation.dashboard.DashboardViewModel
import com.spendwise.app.presentation.navigation.Screen
import com.spendwise.app.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(navController: NavController, viewModel: DashboardViewModel = viewModel()) {
    val expenses by viewModel.expenses.collectAsState()
    val incomes by viewModel.incomes.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val currency = userSettings.currency

    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("All") } // "All", "Expenses", "Income"
    var selectedSource by remember { mutableStateOf("ALL") } // "ALL", "CSV", "EXCEL", "RECEIPT", "VOICE", "MANUAL"
    var selectedCategory by remember { mutableStateOf("All") }
    var sortOrder by remember { mutableStateOf("Newest") } // "Newest", "Oldest", "Highest", "Lowest"

    val categories = listOf("All", "Food", "Transport", "Shopping", "Rent", "Utilities", "Salary", "Freelance", "Investment", "Other")
    val sources = listOf("ALL", "CSV", "EXCEL", "RECEIPT", "VOICE", "MANUAL")

    val allTransactions = remember(expenses, incomes, searchQuery, selectedType, selectedSource, selectedCategory, sortOrder) {
        val list = mutableListOf<Pair<Any, Boolean>>() // Item to isExpense
        if (selectedType != "Income") {
            list.addAll(expenses.map { it to true })
        }
        if (selectedType != "Expenses") {
            list.addAll(incomes.map { it to false })
        }

        list.filter { (item, isExp) ->
            val cat = if (isExp) (item as Expense).category else (item as Income).source
            val desc = if (isExp) (item as Expense).description else (item as Income).description
            val src = if (isExp) (item as Expense).source else (item as Income).origin

            val matchesQuery = searchQuery.isEmpty() || cat.contains(searchQuery, ignoreCase = true) || desc.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == "All" || cat.equals(selectedCategory, ignoreCase = true)
            val matchesSource = selectedSource == "ALL" || src.equals(selectedSource, ignoreCase = true)
            matchesQuery && matchesCat && matchesSource
        }.sortedWith(Comparator { a, b ->
            val dateA = if (a.second) (a.first as Expense).date else (a.first as Income).date
            val dateB = if (b.second) (b.first as Expense).date else (b.first as Income).date
            val amountA = if (a.second) (a.first as Expense).amount else (a.first as Income).amount
            val amountB = if (b.second) (b.first as Expense).amount else (b.first as Income).amount

            when (sortOrder) {
                "Oldest" -> dateA.compareTo(dateB)
                "Highest" -> amountB.compareTo(amountA)
                "Lowest" -> amountA.compareTo(amountB)
                else -> dateB.compareTo(dateA) // Newest
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions Ledger", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.AddExpense.route) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Transaction")
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by description or category...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Source Filter Chips Row (CSV / EXCEL / RECEIPT / VOICE / MANUAL)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(sources) { src ->
                    FilterChip(
                        selected = selectedSource.equals(src, ignoreCase = true),
                        onClick = { selectedSource = src },
                        label = { Text(src, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            // Type Filter Chips: All / Expenses / Income
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Expenses", "Income").forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type, fontSize = 12.sp) }
                    )
                }
            }

            // Category Filter Scrollable Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 11.sp) }
                    )
                }
            }

            // Results count & Sort toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${allTransactions.size} transactions recorded",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sort: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(
                        onClick = {
                            sortOrder = when (sortOrder) {
                                "Newest" -> "Oldest"
                                "Oldest" -> "Highest"
                                "Highest" -> "Lowest"
                                else -> "Newest"
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(sortOrder, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (allTransactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            "No transactions found",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Add your first transaction or adjust your search filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { navController.navigate(Screen.AddExpense.route) }) {
                            Text("Add Transaction")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allTransactions) { (item, isExpense) ->
                        TransactionRowCard(item, isExpense, currency, navController)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRowCard(item: Any, isExpense: Boolean, currency: String, navController: NavController) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val (id, desc, amount, category, date, paymentMethod, source) = if (isExpense) {
        val exp = item as Expense
        Tuple7(exp.id, exp.description, exp.amount, exp.category, exp.date, exp.paymentMethod, exp.source)
    } else {
        val inc = item as Income
        Tuple7(inc.id, inc.description, inc.amount, inc.source, inc.date, inc.paymentMethod, inc.origin)
    }

    val sourceBadgeColor = when (source.uppercase(Locale.ROOT)) {
        "CSV" -> Color(0xFF6366F1)
        "EXCEL" -> Color(0xFF06B6D4)
        "RECEIPT" -> Color(0xFF10B981)
        "VOICE" -> Color(0xFFF59E0B)
        else -> Color(0xFF6B7280)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isExpense) {
                    navController.navigate(Screen.EditExpense.createRoute(id))
                } else {
                    navController.navigate(Screen.EditIncome.createRoute(id))
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isExpense) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isExpense) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = desc.ifEmpty { category },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        // Source Badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = sourceBadgeColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = source.uppercase(Locale.ROOT).ifBlank { "MANUAL" },
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = sourceBadgeColor
                            )
                        }
                    }
                    Text(
                        text = "$category • ${dateFormat.format(date)}${if (paymentMethod.isNotEmpty()) " • $paymentMethod" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "${if (isExpense) "- " else "+ "}${CurrencyFormatter.format(amount, currency)}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
            )
        }
    }
}

private data class Tuple7<A, B, C, D, E, F, G>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G)
