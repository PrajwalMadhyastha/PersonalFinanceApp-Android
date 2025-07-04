package io.pm.finlight.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.pm.finlight.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.net.URLEncoder

@Composable
fun CsvValidationScreen(
    navController: NavController,
    viewModel: SettingsViewModel
) {
    val report by viewModel.csvValidationReport.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val backStackEntry = navController.currentBackStackEntry
    val updatedRowJsonState = backStackEntry?.savedStateHandle?.getLiveData<String>("corrected_row")?.observeAsState()

    LaunchedEffect(updatedRowJsonState?.value) {
        val json = updatedRowJsonState?.value
        val line = backStackEntry?.savedStateHandle?.get<Int>("corrected_row_line")
        if (json != null && line != null) {
            val gson = Gson()
            val correctedData: List<String> = gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
            viewModel.updateAndRevalidateRow(line, correctedData)
            backStackEntry.savedStateHandle.remove<String>("corrected_row")
            backStackEntry.savedStateHandle.remove<Int>("corrected_row_line")
        }
    }

    val currentReport = report
    if (currentReport == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Validation Complete", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Tap a row to edit it, or use the trash icon to ignore it.")
                }

                items(currentReport.reviewableRows, key = { it.lineNumber }) { row ->
                    EditableRowItem(
                        row = row,
                        onEditClick = {
                            // --- UPDATED: Navigate to the AddTransactionScreen in edit mode ---
                            val gson = Gson()
                            val rowDataJson = gson.toJson(row.rowData)
                            val encodedJson = URLEncoder.encode(rowDataJson, "UTF-8")
                            navController.navigate("add_transaction?isCsvEdit=true&csvLineNumber=${row.lineNumber}&initialDataJson=$encodedJson")
                        },
                        onDeleteClick = {
                            viewModel.removeRowFromReport(row)
                        }
                    )
                }
            }
            val importableRowCount = report?.reviewableRows?.count { it.status == CsvRowStatus.VALID || it.status == CsvRowStatus.NEEDS_ACCOUNT_CREATION || it.status == CsvRowStatus.NEEDS_CATEGORY_CREATION || it.status == CsvRowStatus.NEEDS_BOTH_CREATION } ?: 0
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = {
                    viewModel.clearCsvValidationReport()
                    navController.popBackStack()
                }, modifier = Modifier.weight(1f)) { Text("Cancel") }

                Button(
                    onClick = {
                        scope.launch {
                            val rowsToImport = report?.reviewableRows?.filter { it.status != CsvRowStatus.INVALID_AMOUNT && it.status != CsvRowStatus.INVALID_DATE && it.status != CsvRowStatus.INVALID_COLUMN_COUNT }
                            if (!rowsToImport.isNullOrEmpty()) {
                                viewModel.commitCsvImport(rowsToImport)
                                Toast.makeText(context, "$importableRowCount transactions imported!", Toast.LENGTH_LONG).show()
                                navController.navigate("dashboard") { popUpTo(0) }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = importableRowCount > 0
                ) { Text("Import $importableRowCount Rows") }
            }
        }
    }
}

@Composable
fun EditableRowItem(row: ReviewableRow, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    val backgroundColor = when (row.status) {
        CsvRowStatus.VALID -> MaterialTheme.colorScheme.surfaceVariant
        CsvRowStatus.NEEDS_ACCOUNT_CREATION, CsvRowStatus.NEEDS_CATEGORY_CREATION, CsvRowStatus.NEEDS_BOTH_CREATION -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val icon = when (row.status) {
        CsvRowStatus.VALID -> Icons.Default.CheckCircle
        CsvRowStatus.NEEDS_ACCOUNT_CREATION, CsvRowStatus.NEEDS_CATEGORY_CREATION, CsvRowStatus.NEEDS_BOTH_CREATION -> Icons.Default.AddCircle
        else -> Icons.Default.Warning
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = "Status", modifier = Modifier.padding(end = 12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onEditClick)
                    .padding(vertical = 16.dp)
            ) {
                Text("Line ${row.lineNumber}: ${row.rowData.getOrNull(1) ?: "N/A"}", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(row.statusMessage, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Ignore this row")
            }
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Row")
            }
        }
    }
}
