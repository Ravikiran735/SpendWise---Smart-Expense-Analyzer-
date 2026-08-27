package com.spendwise.app.presentation.importdata

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendwise.app.utils.ImportAnalyzer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnMappingDialog(
    headers: List<String>,
    initialMapping: ImportAnalyzer.ColumnMapping,
    onConfirm: (ImportAnalyzer.ColumnMapping) -> Unit,
    onDismiss: () -> Unit
) {
    var dateIdx by remember { mutableStateOf(if (initialMapping.dateIdx >= 0) initialMapping.dateIdx else 0) }
    var descIdx by remember { mutableStateOf(if (initialMapping.descIdx >= 0) initialMapping.descIdx else 1.coerceAtMost(headers.size - 1)) }
    var amountIdx by remember { mutableStateOf(if (initialMapping.amountIdx >= 0) initialMapping.amountIdx else 2.coerceAtMost(headers.size - 1)) }
    var categoryIdx by remember { mutableStateOf(initialMapping.categoryIdx) }
    var paymentMethodIdx by remember { mutableStateOf(initialMapping.paymentMethodIdx) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AltRoute, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Map File Columns", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "We need help matching your file headers to SpendWise transaction fields.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ColumnDropdownSelector(
                    label = "Transaction Date *",
                    headers = headers,
                    selectedIndex = dateIdx,
                    onSelected = { dateIdx = it }
                )

                ColumnDropdownSelector(
                    label = "Description / Merchant *",
                    headers = headers,
                    selectedIndex = descIdx,
                    onSelected = { descIdx = it }
                )

                ColumnDropdownSelector(
                    label = "Amount / Net Value *",
                    headers = headers,
                    selectedIndex = amountIdx,
                    onSelected = { amountIdx = it }
                )

                ColumnDropdownSelector(
                    label = "Category (Optional)",
                    headers = headers,
                    selectedIndex = categoryIdx,
                    allowNone = true,
                    onSelected = { categoryIdx = it }
                )

                ColumnDropdownSelector(
                    label = "Payment Mode (Optional)",
                    headers = headers,
                    selectedIndex = paymentMethodIdx,
                    allowNone = true,
                    onSelected = { paymentMethodIdx = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mapping = ImportAnalyzer.ColumnMapping(
                        dateIdx = dateIdx,
                        descIdx = descIdx,
                        amountIdx = amountIdx,
                        categoryIdx = categoryIdx,
                        paymentMethodIdx = paymentMethodIdx
                    )
                    onConfirm(mapping)
                }
            ) {
                Text("Analyze Records")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnDropdownSelector(
    label: String,
    headers: List<String>,
    selectedIndex: Int,
    allowNone: Boolean = false,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = if (selectedIndex >= 0 && selectedIndex < headers.size) {
        "${headers[selectedIndex]} (Col ${selectedIndex + 1})"
    } else {
        if (allowNone) "— None / Not in file —" else "Select Column"
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = currentLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (allowNone) {
                    DropdownMenuItem(
                        text = { Text("— None / Not in file —") },
                        onClick = {
                            onSelected(-1)
                            expanded = false
                        }
                    )
                }
                headers.forEachIndexed { idx, headerName ->
                    DropdownMenuItem(
                        text = { Text("$headerName (Col ${idx + 1})") },
                        onClick = {
                            onSelected(idx)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
