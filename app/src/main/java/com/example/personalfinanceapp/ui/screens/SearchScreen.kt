package com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.personalfinanceapp.*
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.components.TransactionItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val factory = SearchViewModelFactory(context.applicationContext as Application)
    val viewModel: SearchViewModel = viewModel(factory = factory)

    val searchUiState by viewModel.uiState.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Transactions") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Filter Form
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Keyword Search
                item {
                    OutlinedTextField(
                        value = searchUiState.keyword,
                        onValueChange = { viewModel.onKeywordChange(it) },
                        label = { Text("Keyword (description, notes)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Account Dropdown
                item {
                    SearchableDropdown(
                        label = "Account",
                        options = searchUiState.accounts,
                        selectedOption = searchUiState.selectedAccount,
                        onOptionSelected = { viewModel.onAccountChange(it) },
                        getDisplayName = { it.name }
                    )
                }

                // Category Dropdown
                item {
                    SearchableDropdown(
                        label = "Category",
                        options = searchUiState.categories,
                        selectedOption = searchUiState.selectedCategory,
                        onOptionSelected = { viewModel.onCategoryChange(it) },
                        getDisplayName = { it.name }
                    )
                }

                // Transaction Type Dropdown
                item {
                    SearchableDropdown(
                        label = "Transaction Type",
                        options = listOf("All", "Income", "Expense"),
                        selectedOption = searchUiState.transactionType.replaceFirstChar { it.uppercase() },
                        onOptionSelected = { viewModel.onTypeChange(it) },
                        getDisplayName = { it }
                    )
                }

                // Date Range Pickers
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DateTextField(
                            label = "Start Date",
                            date = searchUiState.startDate,
                            formatter = dateFormatter,
                            onClick = { showStartDatePicker = true },
                            onClear = { viewModel.onDateChange(start = null) },
                            modifier = Modifier.weight(1f)
                        )
                        DateTextField(
                            label = "End Date",
                            date = searchUiState.endDate,
                            formatter = dateFormatter,
                            onClick = { showEndDatePicker = true },
                            onClear = { viewModel.onDateChange(end = null) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Search Results
                if (searchResults.isNotEmpty()) {
                    item {
                        Text(
                            text = "Results (${searchResults.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                    items(searchResults) { transactionDetails ->
                        TransactionItem(
                            transactionDetails = transactionDetails,
                            onClick = { navController.navigate("edit_transaction/${transactionDetails.transaction.id}") }
                        )
                    }
                } else if(searchUiState.hasSearched) {
                    item {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                            Text("No transactions match your criteria.")
                        }
                    }
                }
            }

            // Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.clearFilters() },
                    modifier = Modifier.weight(1f)
                ) { Text("Clear") }

                Button(
                    onClick = { viewModel.executeSearch() },
                    modifier = Modifier.weight(1f)
                ) { Text("Apply Filters") }
            }
        }
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = searchUiState.startDate)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDateChange(start = datePickerState.selectedDateMillis)
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = searchUiState.endDate)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDateChange(end = datePickerState.selectedDateMillis)
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SearchableDropdown(
    label: String,
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T?) -> Unit,
    getDisplayName: (T) -> String
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedOption?.let { getDisplayName(it) } ?: "All",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Row {
                    if (selectedOption != null) {
                        IconButton(onClick = { onOptionSelected(null) }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear selection")
                        }
                    }
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(getDisplayName(option)) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DateTextField(
    label: String,
    date: Long?,
    formatter: SimpleDateFormat,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = date?.let { formatter.format(Date(it)) } ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = modifier.clickable(onClick = onClick),
        trailingIcon = {
            if (date != null) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, "Clear Date")
                }
            } else {
                Icon(Icons.Default.DateRange, "Select Date")
            }
        }
    )
}