package com.spendwise.app.presentation.importdata

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.spendwise.app.domain.model.CandidateStatus
import com.spendwise.app.domain.model.ImportCandidate
import com.spendwise.app.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportReviewDialog(
    fileName: String,
    candidates: List<ImportCandidate>,
    isImporting: Boolean,
    onToggleSelection: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onUpdateCandidate: (ImportCandidate) -> Unit,
    onConfirmImport: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") } // "All", "New", "Needs Review", "Duplicates", "Invalid"

    val totalRecords = candidates.size
    val newCount = candidates.count { it.status == CandidateStatus.NEW }
    val dupCount = candidates.count { it.status == CandidateStatus.DUPLICATE }
    val reviewCount = candidates.count { it.status == CandidateStatus.NEEDS_REVIEW }
    val invalidCount = candidates.count { it.status == CandidateStatus.INVALID }
    val selectedCount = candidates.count { it.isSelected && it.status != CandidateStatus.DUPLICATE && it.status != CandidateStatus.INVALID }

    val filteredList = remember(candidates, selectedFilter) {
        when (selectedFilter) {
            "New" -> candidates.filter { it.status == CandidateStatus.NEW }
            "Needs Review" -> candidates.filter { it.status == CandidateStatus.NEEDS_REVIEW }
            "Duplicates" -> candidates.filter { it.status == CandidateStatus.DUPLICATE }
            "Invalid" -> candidates.filter { it.status == CandidateStatus.INVALID }
            else -> candidates
        }
    }

    Dialog(
        onDismissRequest = { if (!isImporting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "IMPORT REVIEW",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = fileName.ifBlank { "Smart Import Preview" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss, enabled = !isImporting) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Metric Counters Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricCounter(label = "TOTAL", count = totalRecords, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MetricCounter(label = "NEW", count = newCount, color = Color(0xFF10B981))
                    MetricCounter(label = "DUPLICATES", count = dupCount, color = Color(0xFF6B7280))
                    MetricCounter(label = "REVIEW", count = reviewCount, color = Color(0xFFF59E0B))
                    MetricCounter(label = "INVALID", count = invalidCount, color = Color(0xFFF43F5E))
                }

                // Filter Chips & Select All Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("All", "New", "Needs Review", "Duplicates").forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = { onSelectAll(true) },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Select All", fontSize = 11.sp)
                        }
                        TextButton(
                            onClick = { onSelectAll(false) },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Deselect", fontSize = 11.sp)
                        }
                    }
                }

                // Candidate Transaction Cards List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList, key = { it.id }) { candidate ->
                        CandidateRowItem(
                            candidate = candidate,
                            onToggleSelection = { onToggleSelection(candidate.id) },
                            onUpdate = onUpdateCandidate
                        )
                    }
                }

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isImporting
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onConfirmImport,
                        modifier = Modifier.weight(1.5f),
                        enabled = selectedCount > 0 && !isImporting,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Importing...")
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Selected ($selectedCount)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCounter(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$count", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = color.copy(alpha = 0.85f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CandidateRowItem(
    candidate: ImportCandidate,
    onToggleSelection: () -> Unit,
    onUpdate: (ImportCandidate) -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.ROOT) }
    var isEditing by remember { mutableStateOf(false) }

    val statusColor = when (candidate.status) {
        CandidateStatus.NEW -> Color(0xFF10B981)
        CandidateStatus.DUPLICATE -> Color(0xFF6B7280)
        CandidateStatus.NEEDS_REVIEW -> Color(0xFFF59E0B)
        CandidateStatus.INVALID -> Color(0xFFF43F5E)
    }

    val isSelectable = candidate.status != CandidateStatus.DUPLICATE && candidate.status != CandidateStatus.INVALID

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (candidate.isSelected && isSelectable) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(
                        checked = candidate.isSelected,
                        onCheckedChange = { if (isSelectable) onToggleSelection() },
                        enabled = isSelectable
                    )

                    Column {
                        Text(
                            text = candidate.description,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "${sdf.format(candidate.date)} • ${candidate.paymentMethod}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    val prefix = if (candidate.type.equals("Income", ignoreCase = true)) "+" else "-"
                    val amtColor = if (candidate.type.equals("Income", ignoreCase = true)) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                    Text(
                        text = "$prefix${CurrencyFormatter.format(candidate.amount, "INR")}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = amtColor
                    )

                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.15f),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = candidate.status.name.replace("_", " "),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            // Quick Category & Type tag + Edit button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = candidate.category,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    if (candidate.statusReason.isNotBlank()) {
                        Text(
                            text = candidate.statusReason,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontSize = 10.sp
                        )
                    }
                }

                IconButton(
                    onClick = { isEditing = !isEditing },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (isEditing) Icons.Default.ExpandLess else Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Expandable Edit Area
            if (isEditing) {
                Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                EditCandidateInlineForm(
                    candidate = candidate,
                    onSave = {
                        onUpdate(it)
                        isEditing = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCandidateInlineForm(
    candidate: ImportCandidate,
    onSave: (ImportCandidate) -> Unit
) {
    var desc by remember { mutableStateOf(candidate.description) }
    var amountStr by remember { mutableStateOf(candidate.amount.toString()) }
    var type by remember { mutableStateOf(candidate.type) }
    var category by remember { mutableStateOf(candidate.category) }
    var paymentMethod by remember { mutableStateOf(candidate.paymentMethod) }

    val categories = listOf(
        "Food", "Transport", "Shopping", "Rent", "Utilities", "Education",
        "Healthcare", "Entertainment", "Subscriptions", "Travel", "Investment",
        "Salary", "Freelance", "Business", "Gift", "Other"
    )

    val paymentMethods = listOf("UPI", "Credit Card", "Debit Card", "Bank Transfer", "Cash", "Other")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = desc,
            onValueChange = { desc = it },
            label = { Text("Description", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("Amount", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            // Type Segmented Switch
            Row(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = type.equals("Expense", ignoreCase = true),
                    onClick = { type = "Expense" },
                    label = { Text("Exp", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = type.equals("Income", ignoreCase = true),
                    onClick = { type = "Income" },
                    label = { Text("Inc", fontSize = 11.sp) }
                )
            }
        }

        // Category Selection
        var catExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = catExpanded,
            onExpandedChange = { catExpanded = !catExpanded }
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category", fontSize = 11.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = catExpanded,
                onDismissRequest = { catExpanded = false }
            ) {
                categories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = {
                            category = cat
                            catExpanded = false
                        }
                    )
                }
            }
        }

        Button(
            onClick = {
                val amt = amountStr.toDoubleOrNull() ?: candidate.amount
                val updated = candidate.copy(
                    description = desc,
                    amount = amt,
                    type = type,
                    category = category,
                    paymentMethod = paymentMethod,
                    status = CandidateStatus.NEW,
                    statusReason = "Manually verified",
                    isSelected = true
                )
                onSave(updated)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Update Record", fontSize = 12.sp)
        }
    }
}
