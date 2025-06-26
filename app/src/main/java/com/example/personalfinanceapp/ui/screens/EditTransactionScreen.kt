package com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.personalfinanceapp.Account
import com.example.personalfinanceapp.Category
import com.example.personalfinanceapp.TransactionViewModel
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.components.TimePickerDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    navController: NavController,
    viewModel: TransactionViewModel,
    transactionId: Int,
    // --- ADDED: New parameters to handle CSV editing ---
    isFromCsvImport: Boolean = false,
    csvLineNumber: Int = -1,
    initialCsvData: String? = null
) {
    val transaction by viewModel.getTransactionById(transactionId).collectAsState(initial = null)

    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var transactionType by remember { mutableStateOf("expense") }
    val transactionTypes = listOf("Expense", "Income")

    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var isAccountDropdownExpanded by remember { mutableStateOf(false) }

    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    // --- CORRECTED: Typo fixed from 'mutableState of' to 'mutableStateOf' ---
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val selectedDateTime = remember { Calendar.getInstance() }

    val snackbarHostState = remember { SnackbarHostState() }
    val validationError by viewModel.validationError.collectAsState()
    val transactionFromDb by viewModel.getTransactionById(transactionId).collectAsState(initial = null)
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    LaunchedEffect(transactionFromDb, initialCsvData, accounts, categories) {
        if (isFromCsvImport && initialCsvData != null) {
            val gson = Gson()
            val listType = object : TypeToken<List<String>>() {}.type
            val tokens: List<String> = gson.fromJson(initialCsvData, listType)

            try {
                selectedDateTime.time = dateFormat.parse(tokens[0]) ?: Date()
                description = tokens[1]
                amount = tokens[2]
                transactionType = tokens[3].lowercase()
                notes = tokens.getOrElse(6) { "" }
                selectedCategory = categories.find { it.name.equals(tokens[4], ignoreCase = true) }
                selectedAccount = accounts.find { it.name.equals(tokens[5], ignoreCase = true) }
            } catch (e: Exception) { /* Handle parsing error if needed */ }

        } else if (transactionFromDb != null) {
            transactionFromDb?.let { txn ->
                description = txn.description
                amount = txn.amount.toString()
                notes = txn.notes ?: ""
                selectedDateTime.timeInMillis = txn.date
                selectedAccount = accounts.find { it.id == txn.accountId }
                selectedCategory = categories.find { it.id == txn.categoryId }
                transactionType = txn.transactionType
            }
        }
    }

    LaunchedEffect(validationError) {
        validationError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isFromCsvImport) "Edit CSV Row" else "Edit Transaction") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = { if (!isFromCsvImport) { IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, "Delete") } } }
            )
        }
    ) { innerPadding ->
        val canShowForm = (!isFromCsvImport && transactionFromDb != null) || isFromCsvImport
        if (canShowForm) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    TabRow(selectedTabIndex = if (transactionType == "expense") 0 else 1) {
                        transactionTypes.forEachIndexed { index, title ->
                            Tab(
                                selected = (if (transactionType == "expense") 0 else 1) == index,
                                onClick = {
                                    transactionType = if (index == 0) "expense" else "income"
                                },
                                text = { Text(title) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Select Date"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(
                                    selectedDateTime.time
                                )
                            )
                        }
                        Button(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Select Time"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(
                                    selectedDateTime.time
                                )
                            )
                        }
                    }
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = isAccountDropdownExpanded,
                        onExpandedChange = {
                            isAccountDropdownExpanded = !isAccountDropdownExpanded
                        }) {
                        OutlinedTextField(
                            value = selectedAccount?.name ?: "Select Account",
                            onValueChange = {}, readOnly = true, label = { Text("Account") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAccountDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = isAccountDropdownExpanded,
                            onDismissRequest = { isAccountDropdownExpanded = false }) {
                            accounts.forEach { account ->
                                DropdownMenuItem(text = { Text(account.name) }, onClick = {
                                    selectedAccount = account
                                    isAccountDropdownExpanded = false
                                })
                            }
                        }
                    }
                }
                item {
                    ExposedDropdownMenuBox(
                        expanded = isCategoryDropdownExpanded,
                        onExpandedChange = {
                            isCategoryDropdownExpanded = !isCategoryDropdownExpanded
                        }) {
                        OutlinedTextField(
                            value = selectedCategory?.name ?: "Select Category",
                            onValueChange = {}, readOnly = true, label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = isCategoryDropdownExpanded,
                            onDismissRequest = { isCategoryDropdownExpanded = false }) {
                            categories.forEach { category ->
                                DropdownMenuItem(text = { Text(category.name) }, onClick = {
                                    selectedCategory = category
                                    isCategoryDropdownExpanded = false
                                })
                            }
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (isFromCsvImport) {
                                    val correctedData = listOf(
                                        dateFormat.format(selectedDateTime.time),
                                        description,
                                        amount,
                                        transactionType,
                                        selectedCategory?.name ?: "",
                                        selectedAccount?.name ?: "",
                                        notes
                                    )
                                    val gson = Gson()
                                    navController.previousBackStackEntry?.savedStateHandle?.set(
                                        "corrected_row",
                                        gson.toJson(correctedData)
                                    )
                                    navController.previousBackStackEntry?.savedStateHandle?.set(
                                        "corrected_row_line",
                                        csvLineNumber
                                    )
                                    navController.popBackStack()
                                } else {
                                    val updatedAmount = amount.toDoubleOrNull() ?: 0.0
                                    val currentTransaction = transactionFromDb
                                    if (currentTransaction != null && selectedAccount != null) {
                                        val updatedTransaction = currentTransaction.copy(
                                            description = description,
                                            amount = updatedAmount,
                                            accountId = selectedAccount!!.id,
                                            categoryId = selectedCategory?.id,
                                            notes = notes.takeIf { it.isNotBlank() },
                                            date = selectedDateTime.timeInMillis,
                                            transactionType = transactionType
                                        )
                                        if (viewModel.updateTransaction(updatedTransaction)) {
                                            navController.popBackStack()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = selectedAccount != null && selectedCategory != null
                        ) {
                            Text(if (isFromCsvImport) "Update Row" else "Update Transaction")
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateTime.timeInMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            val newCalendar = Calendar.getInstance().apply { timeInMillis = it }
                            selectedDateTime.set(Calendar.YEAR, newCalendar.get(Calendar.YEAR))
                            selectedDateTime.set(Calendar.MONTH, newCalendar.get(Calendar.MONTH))
                            selectedDateTime.set(Calendar.DAY_OF_MONTH, newCalendar.get(Calendar.DAY_OF_MONTH))
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = selectedDateTime.get(Calendar.HOUR_OF_DAY), initialMinute = selectedDateTime.get(
            Calendar.MINUTE))
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onConfirm = {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                selectedDateTime.set(Calendar.MINUTE, timePickerState.minute)
                showTimePicker = false
            }
        ) { TimePicker(state = timePickerState) }
    }

    if (showDeleteDialog) {
        val transactionToDelete = transactionFromDb
        if (transactionToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Confirm Deletion") },
                text = { Text("Are you sure you want to permanently delete this transaction?") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.deleteTransaction(transactionToDelete)
                        showDeleteDialog = false
                        navController.popBackStack()
                    }) { Text("Delete") }
                },
                dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
            )
        }
    }
}
