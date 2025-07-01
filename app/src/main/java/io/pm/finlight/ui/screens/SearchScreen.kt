// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/SearchScreen.kt
// REASON: Modernized the UI by implementing auto-focus for the keyword field
// and removing the now-obsolete "Apply Filter" button, creating a more
// dynamic and responsive search experience.
// UPDATE: Redesigned the layout to hide advanced filters in a collapsible
// section, providing a cleaner initial view and an indicator for active filters.
// =================================================================================
package io.pm.finlight.ui.screens

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.pm.finlight.*
import io.pm.finlight.ui.components.TransactionItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    val context = LocalContext.current
    val factory = SearchViewModelFactory(context.applicationContext as Application)
    val viewModel: SearchViewModel = viewModel(factory = factory)

    val searchUiState by viewModel.uiState.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showAdvancedFilters by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val areFiltersActive by remember(searchUiState) {
        derivedStateOf {
            searchUiState.selectedAccount != null ||
                    searchUiState.selectedCategory != null ||
                    searchUiState.transactionType != "All" ||
                    searchUiState.startDate != null ||
                    searchUiState.endDate != null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar and Filter Toggle
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchUiState.keyword,
                    onValueChange = { viewModel.onKeywordChange(it) },
                    label = { Text("Keyword (description, notes)") },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    singleLine = true
                )
                BadgedBox(
                    badge = {
                        if (areFiltersActive) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                ) {
                    IconButton(onClick = { showAdvancedFilters = !showAdvancedFilters }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Toggle Advanced Filters")
                    }
                }
            }

            // Collapsible Advanced Filters Section
            AnimatedVisibility(
                visible = showAdvancedFilters,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SearchableDropdown(
                        label = "Account",
                        options = searchUiState.accounts,
                        selectedOption = searchUiState.selectedAccount,
                        onOptionSelected = { viewModel.onAccountChange(it) },
                        getDisplayName = { it.name },
                    )
                    SearchableDropdown(
                        label = "Category",
                        options = searchUiState.categories,
                        selectedOption = searchUiState.selectedCategory,
                        onOptionSelected = { viewModel.onCategoryChange(it) },
                        getDisplayName = { it.name },
                    )
                    SearchableDropdown(
                        label = "Transaction Type",
                        options = listOf("All", "Income", "Expense"),
                        selectedOption = searchUiState.transactionType.replaceFirstChar { it.uppercase() },
                        onOptionSelected = { viewModel.onTypeChange(it) },
                        getDisplayName = { it },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DateTextField(
                            label = "Start Date",
                            date = searchUiState.startDate,
                            formatter = dateFormatter,
                            onClick = { showStartDatePicker = true },
                            onClear = { viewModel.onDateChange(start = null) },
                            modifier = Modifier.weight(1f),
                        )
                        DateTextField(
                            label = "End Date",
                            date = searchUiState.endDate,
                            formatter = dateFormatter,
                            onClick = { showEndDatePicker = true },
                            onClear = { viewModel.onDateChange(end = null) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.clearFilters() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Clear All Filters") }
                }
            }
        }

        HorizontalDivider()

        // Search Results
        if (searchResults.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    Text(
                        text = "Results (${searchResults.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                items(searchResults) { transactionDetails ->
                    TransactionItem(
                        transactionDetails = transactionDetails,
                        onClick = { navController.navigate("transaction_detail/${transactionDetails.transaction.id}") },
                    )
                }
            }
        } else if (searchUiState.hasSearched) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No transactions match your criteria.")
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
            },
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
            },
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
    getDisplayName: (T) -> String,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
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
            modifier =
                Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(getDisplayName(option)) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
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
    modifier: Modifier = Modifier,
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
        },
    )
}
