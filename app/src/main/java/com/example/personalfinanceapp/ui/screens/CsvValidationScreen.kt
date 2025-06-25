package com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.personalfinanceapp.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvValidationScreen(
    navController: NavController,
    viewModel: SettingsViewModel
) {
    val report by viewModel.csvValidationReport.collectAsState()

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(onClick = {
                    viewModel.clearCsvValidationReport()
                    navController.popBackStack()
                }, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(onClick = { /* TODO: Implement import logic */ }, modifier = Modifier.weight(1f)) {
                    Text("Import Valid Rows")
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("Validation Complete", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("${currentReport.totalRowCount} rows found. ${currentReport.validRows.size + currentReport.rowsWithNewEntities.size} will be imported. ${currentReport.invalidRows.size} will be skipped.")
                }

                if (currentReport.invalidRows.isNotEmpty()) {
                    item { SettingSectionHeader("Skipped Rows (with errors)") }
                    items(currentReport.invalidRows) { invalidRow -> InvalidRowItem(invalidRow) }
                }

                if (currentReport.rowsWithNewEntities.isNotEmpty()) {
                    item { SettingSectionHeader("Rows Requiring New Categories/Accounts") }
                    items(currentReport.rowsWithNewEntities) { rowForCreation ->
                        RowForCreationItem(rowForCreation)
                    }
                }

                if (currentReport.validRows.isNotEmpty()) {
                    item { SettingSectionHeader("Valid Rows (using existing data)") }
                    items(currentReport.validRows) { validRow -> ValidRowItem(validRow) }
                }
            }
        }
    }
}

// --- ADDED: The missing composable for invalid rows ---
@Composable
fun InvalidRowItem(row: InvalidRow) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Line ${row.lineNumber}: ${row.errorMessage}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(4.dp))
            Text("Original Data: ${row.rowData}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

// --- ADDED: The missing composable for valid rows ---
@Composable
fun ValidRowItem(row: ValidatedRow) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(row.transaction.description, style = MaterialTheme.typography.titleMedium)
            Text("Amount: ₹${row.transaction.amount}, Type: ${row.transaction.transactionType}")
            Text("Account: ${row.accountName}, Category: ${row.categoryName}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun RowForCreationItem(row: RowForCreation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Line ${row.lineNumber}: ${row.rawData.getOrNull(1) ?: "N/A"}", fontWeight = FontWeight.Bold)
            Text(row.message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
