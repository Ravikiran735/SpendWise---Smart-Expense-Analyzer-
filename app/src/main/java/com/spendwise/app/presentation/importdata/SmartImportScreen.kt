package com.spendwise.app.presentation.importdata

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.spendwise.app.domain.model.ImportHistory
import com.spendwise.app.presentation.dashboard.BottomNavigationBar
import com.spendwise.app.presentation.navigation.Screen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartImportScreen(
    navController: NavController,
    viewModel: SmartImportViewModel = viewModel()
) {
    val context = LocalContext.current
    val history by viewModel.importHistory.collectAsState()
    val stats by viewModel.importStats.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val showReview by viewModel.showReviewDialog.collectAsState()
    val showMapping by viewModel.showColumnMappingDialog.collectAsState()
    val candidates by viewModel.candidates.collectAsState()
    val rawTable by viewModel.rawTable.collectAsState()
    val currentFileName by viewModel.currentFileName.collectAsState()
    val detectedMapping by viewModel.detectedMapping.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    // File Picker for CSV and Excel
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            var name = "import_file"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
            viewModel.processFile(uri, context, name)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Smart Import", fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Bring your financial data into one place",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HERO COMMAND CENTER ACTION CARDS (2x2 Grid)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "IMPORT SOURCES",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ImportActionHubCard(
                            modifier = Modifier.weight(1f),
                            title = "IMPORT FILES",
                            subtitle = "CSV / Excel statements",
                            icon = Icons.Default.UploadFile,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onClick = {
                                filePickerLauncher.launch("*/*")
                            }
                        )

                        ImportActionHubCard(
                            modifier = Modifier.weight(1f),
                            title = "SCAN RECEIPT",
                            subtitle = "Extract details automatically",
                            icon = Icons.Default.DocumentScanner,
                            accentColor = Color(0xFF10B981),
                            onClick = { navController.navigate("camera") }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ImportActionHubCard(
                            modifier = Modifier.weight(1f),
                            title = "VOICE ENTRY",
                            subtitle = "Describe expense naturally",
                            icon = Icons.Default.Mic,
                            accentColor = Color(0xFFF59E0B),
                            onClick = {
                                navController.navigate(Screen.AddExpense.route)
                            }
                        )

                        ImportActionHubCard(
                            modifier = Modifier.weight(1f),
                            title = "MANUAL ENTRY",
                            subtitle = "Add transaction yourself",
                            icon = Icons.Default.EditNote,
                            accentColor = Color(0xFF06B6D4),
                            onClick = { navController.navigate(Screen.AddExpense.route) }
                        )
                    }
                }
            }

            // 2. SMART IMPORT ANALYTICS
            item {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Smart Import Analytics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${stats.totalImports} Imports Total",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Metrics Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ImportStatMetric(label = "Imported", value = "${stats.totalTransactionsImported}", color = Color(0xFF10B981))
                            ImportStatMetric(label = "Duplicates Blocked", value = "${stats.totalDuplicatesPrevented}", color = Color(0xFF6B7280))
                            ImportStatMetric(label = "Needs Review", value = "${stats.totalNeedsReview}", color = Color(0xFFF59E0B))
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Source Breakdown Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            SourceCountBadge("CSV", stats.csvImportsCount)
                            SourceCountBadge("Excel", stats.excelImportsCount)
                            SourceCountBadge("Receipt", stats.receiptImportsCount)
                            SourceCountBadge("Voice", stats.voiceImportsCount)
                            SourceCountBadge("Manual", stats.manualImportsCount)
                        }
                    }
                }
            }

            // 3. RECENT IMPORT HISTORY
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "IMPORT HISTORY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${history.size} sessions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (history.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.outline)
                            Text("No imports recorded yet", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Import a CSV or Excel file to get started.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            } else {
                items(history, key = { it.importId }) { item ->
                    ImportHistoryCard(item)
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Loading Dialog when Analyzing File
    if (isAnalyzing) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Analyzing File...") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text("Parsing transactions and checking duplicates...")
                }
            },
            confirmButton = {}
        )
    }

    // Column Mapping Dialog (if needed)
    if (showMapping && rawTable.isNotEmpty()) {
        ColumnMappingDialog(
            headers = rawTable[0],
            initialMapping = detectedMapping,
            onConfirm = { customMapping ->
                viewModel.applyCustomMapping(customMapping)
            },
            onDismiss = { viewModel.closeReview() }
        )
    }

    // Full Review & Confirmation Dialog
    if (showReview && candidates.isNotEmpty()) {
        ImportReviewDialog(
            fileName = currentFileName,
            candidates = candidates,
            isImporting = isImporting,
            onToggleSelection = { viewModel.toggleCandidateSelection(it) },
            onSelectAll = { viewModel.selectAll(it) },
            onUpdateCandidate = { viewModel.updateCandidate(it) },
            onConfirmImport = { viewModel.commitImport() },
            onDismiss = { viewModel.closeReview() }
        )
    }
}

@Composable
private fun ImportActionHubCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(20.dp))
            }

            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ImportStatMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SourceCountBadge(source: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$count", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        Text(text = source, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ImportHistoryCard(history: ImportHistory) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ROOT) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                    Icon(
                        when (history.sourceType.uppercase(Locale.ROOT)) {
                            "CSV" -> Icons.Default.InsertDriveFile
                            "EXCEL" -> Icons.Default.TableChart
                            "RECEIPT" -> Icons.Default.Receipt
                            "VOICE" -> Icons.Default.Mic
                            else -> Icons.Default.EditNote
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = history.fileName.ifBlank { "Import Session" },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (history.status.uppercase(Locale.ROOT)) {
                        "COMPLETED" -> Color(0xFF10B981).copy(alpha = 0.15f)
                        "FAILED" -> Color(0xFFF43F5E).copy(alpha = 0.15f)
                        else -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = history.status,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (history.status.uppercase(Locale.ROOT)) {
                            "COMPLETED" -> Color(0xFF10B981)
                            "FAILED" -> Color(0xFFF43F5E)
                            else -> Color(0xFFF59E0B)
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${history.totalRecords} records • ${history.newRecords} imported • ${history.duplicateRecords} dupes",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = sdf.format(history.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
