package com.spendwise.app.presentation.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.spendwise.app.presentation.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.userSettings.collectAsState()
    val scrollState = rememberScrollState()

    var showLogoutDialog by remember { mutableStateOf(false) }

    val currencies = listOf(
        "INR" to "INR (₹) - Indian Rupee",
        "USD" to "USD ($) - US Dollar",
        "EUR" to "EUR (€) - Euro",
        "GBP" to "GBP (£) - British Pound"
    )

    val financialModes = listOf("BUILD", "BALANCE", "SAVE", "CONTROL")
    val primaryGoals = listOf("Save Money", "Control Spending", "Build Emergency Fund", "Buy Something", "Travel", "Education")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Intelligence Preferences", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
            // 1. FINANCIAL MODE & COPILOT PRIORITIZATION
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Financial Mode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    Text("Controls how the Financial Copilot prioritizes recommendations.", style = MaterialTheme.typography.bodySmall)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        financialModes.forEach { mode ->
                            val isSelected = settings.financialMode.equals(mode, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setFinancialMode(mode) },
                                label = { Text(mode, fontSize = 11.sp) }
                            )
                        }
                    }

                    val modeDesc = when (settings.financialMode.uppercase()) {
                        "SAVE" -> "SAVE MODE: Prioritizes savings rate, goal milestones, and expense cutbacks."
                        "CONTROL" -> "CONTROL MODE: Prioritizes budget adherence, spending leaks, and anomaly detection."
                        "BUILD" -> "BUILD MODE: Prioritizes long-term asset building and emergency reserves."
                        else -> "BALANCE MODE: Balances 50/30/20 lifestyle and sustainable goal contributions."
                    }
                    Text(modeDesc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }

            // 2. PRIMARY FINANCIAL GOAL
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.TrackChanges, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Primary Financial Goal", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    Text("The AI Copilot aligns recommendations with your primary objective.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        primaryGoals.chunked(2).forEach { rowGoals ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                rowGoals.forEach { goal ->
                                    val isSelected = settings.primaryGoal.equals(goal, ignoreCase = true)
                                    FilterChip(
                                        modifier = Modifier.weight(1f),
                                        selected = isSelected,
                                        onClick = { viewModel.setPrimaryGoal(goal) },
                                        label = { Text(goal, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. APPEARANCE & THEME
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Appearance & Theme", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dark Theme", fontWeight = FontWeight.SemiBold)
                            Text("Synchronized across Android & Web", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.theme == "dark",
                            onCheckedChange = { isDark ->
                                viewModel.setTheme(if (isDark) "dark" else "light")
                            }
                        )
                    }
                }
            }

            // 4. CURRENCY SELECTION
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Primary Currency", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    Text("Selected currency updates reports and dashboard across devices.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currencies.forEach { (code, label) ->
                            val isSelected = settings.currency.equals(code, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setCurrency(code) },
                                label = { Text(code) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            // 5. SMART IMPORT PREFERENCES
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Smart Import Preferences", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    NotificationToggleRow(
                        title = "Automatic Categorization",
                        subtitle = "Smart AI keyword matching for statements and receipts",
                        checked = settings.autoCategorization,
                        onCheckedChange = { viewModel.setNotificationPreference("autoCategorization", it) }
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    NotificationToggleRow(
                        title = "Duplicate Detection",
                        subtitle = "Prevent re-importing already logged transactions",
                        checked = settings.duplicateDetection,
                        onCheckedChange = { viewModel.setNotificationPreference("duplicateDetection", it) }
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    NotificationToggleRow(
                        title = "Import Notifications",
                        subtitle = "Notify upon completing batch file imports",
                        checked = settings.importNotifications,
                        onCheckedChange = { viewModel.setNotificationPreference("importNotifications", it) }
                    )
                }
            }

            // 6. NOTIFICATION PREFERENCES
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Alerts & Reminders", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    NotificationToggleRow(
                        title = "Budget Threshold Alerts",
                        subtitle = "Alert when spending reaches 80% or exceeds budget",
                        checked = settings.budgetAlerts,
                        onCheckedChange = { viewModel.setNotificationPreference("budgetAlerts", it) }
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    NotificationToggleRow(
                        title = "Savings Goals Reminders",
                        subtitle = "Periodic updates on goal targets & milestones",
                        checked = settings.savingsReminders,
                        onCheckedChange = { viewModel.setNotificationPreference("savingsReminders", it) }
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    NotificationToggleRow(
                        title = "Smart Financial Insights",
                        subtitle = "AI-assisted spending analysis and savings tips",
                        checked = settings.financialInsights,
                        onCheckedChange = { viewModel.setNotificationPreference("financialInsights", it) }
                    )
                }
            }

            // 7. ACCOUNT & LOGOUT
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Account", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    OutlinedButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out of SpendWise?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
