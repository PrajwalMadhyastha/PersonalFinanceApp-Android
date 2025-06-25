package com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.personalfinanceapp.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvValidationScreen(
    navController: NavController,
    viewModel: SettingsViewModel
) {
    val report by viewModel.csvValidationReport.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val reviewableRows = remember { mutableStateListOf<ReviewableRow>() }
    var reportInitialized by remember { mutableStateOf(false) }

    val backStackEntry = navController.currentBackStackEntry
    // --- CORRECTED: Use standard assignment for nullable LiveData state ---
    val updatedRowJsonState = backStackEntry?.savedStateHandle?.getLiveData<String>("corrected_row")?.observeAsState()
    val updatedRowLineState = backStackEntry?.savedStateHandle?.getLiveData<Int>("corrected_row_line")?.observeAsState()

    // Access the value of the state object
    val updatedRowJson = updatedRowJsonState?.value
    val updatedRowLine = updatedRowLineState?.value

    LaunchedEffect(updatedRowJson) {
        if (updatedRowJson != null && updatedRowLine != null) {
            val gson = Gson()
            val correctedData: List<String> = gson.fromJson(updatedRowJson, object : TypeToken<List<String>>() {}.type)
            val indexToUpdate = reviewableRows.indexOfFirst { it.lineNumber == updatedRowLine }
            if (indexToUpdate != -1) {
                // TODO: Re-validate the corrected row
                val originalRow = reviewableRows[indexToUpdate]
                reviewableRows[indexToUpdate] = originalRow.copy(rowData = correctedData)
            }
            backStackEntry?.savedStateHandle?.remove<String>("corrected_row")
            backStackEntry?.savedStateHandle?.remove<Int>("corrected_row_line")
        }
    }

    LaunchedEffect(report) {
        if (report != null && !reportInitialized) {
            reviewableRows.clear()
            reviewableRows.addAll(report!!.reviewableRows)
            reportInitialized = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CSV Import Preview") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            val importableRowCount = reviewableRows.count { it.status != CsvRowStatus.INVALID_AMOUNT && it.status != CsvRowStatus.INVALID_DATE && it.status != CsvRowStatus.INVALID_COLUMN_COUNT }
            if (report != null) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(onClick = {
                        viewModel.clearCsvValidationReport()
                        navController.popBackStack()
                    }, modifier = Modifier.weight(1f)) { Text("Cancel") }

                    Button(
                        onClick = {
                            scope.launch {
                                val rowsToImport = reviewableRows.filter { it.status != CsvRowStatus.INVALID_AMOUNT && it.status != CsvRowStatus.INVALID_DATE && it.status != CsvRowStatus.INVALID_COLUMN_COUNT }
                                viewModel.commitCsvImport(rowsToImport)
                                Toast.makeText(context, "$importableRowCount transactions imported successfully!", Toast.LENGTH_LONG).show()
                                navController.navigate("dashboard") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = importableRowCount > 0
                    ) { Text("Import $importableRowCount Rows") }
                }
            }
        }
    ) { innerPadding ->
        val currentReport = report
        if (currentReport == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("Analyzing file...")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Validation Complete", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Review the rows below. Tap any row to edit it.")
                }

                items(reviewableRows) { row ->
                    EditableRowItem(
                        row = row,
                        onClick = {
                            val gson = Gson()
                            val rowDataJson = gson.toJson(row.rowData)
                            val encodedJson = URLEncoder.encode(rowDataJson, "UTF-8")
                            navController.navigate("edit_imported_transaction/${row.lineNumber}/$encodedJson")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EditableRowItem(row: ReviewableRow, onClick: () -> Unit) {
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

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = "Status", modifier = Modifier.padding(end = 12.dp))
            Column(Modifier.weight(1f)) {
                Text("Line ${row.lineNumber}: ${row.rowData.getOrNull(1) ?: "N/A"}", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(row.statusMessage, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.Edit, contentDescription = "Edit Row")
        }
    }
}
