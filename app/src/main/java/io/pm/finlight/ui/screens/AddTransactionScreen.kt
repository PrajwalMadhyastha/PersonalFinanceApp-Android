package io.pm.finlight.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
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
import io.pm.finlight.Account
import io.pm.finlight.Category
import io.pm.finlight.TransactionViewModel
import io.pm.finlight.ui.components.TimePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavController,
    viewModel: TransactionViewModel,
) {
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

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val selectedDateTime by remember { mutableStateOf(Calendar.getInstance()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val validationError by viewModel.validationError.collectAsState()

    LaunchedEffect(validationError) {
        validationError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // A Box to host the Snackbar, as Scaffold is removed
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                TabRow(selectedTabIndex = if (transactionType == "expense") 0 else 1) {
                    transactionTypes.forEachIndexed { index, title ->
                        Tab(
                            selected = (if (transactionType == "expense") 0 else 1) == index,
                            onClick = { transactionType = if (index == 0) "expense" else "income" },
                            text = { Text(title) },
                        )
                    }
                }
            }
            item {
                OutlinedTextField(value = description, onValueChange = {
                    description = it
                }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(value = amount, onValueChange = {
                    amount = it
                }, label = {
                    Text(
                        "Amount",
                    )
                }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Select Date")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDateTime.time))
                    }
                    Button(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Default.AccessTime, contentDescription = "Select Time")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(selectedDateTime.time))
                    }
                }
            }
            item {
                ExposedDropdownMenuBox(expanded = isAccountDropdownExpanded, onExpandedChange = {
                    isAccountDropdownExpanded = !isAccountDropdownExpanded
                }) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "Select Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAccountDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = isAccountDropdownExpanded, onDismissRequest = { isAccountDropdownExpanded = false }) {
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
                ExposedDropdownMenuBox(expanded = isCategoryDropdownExpanded, onExpandedChange = {
                    isCategoryDropdownExpanded = !isCategoryDropdownExpanded
                }) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = isCategoryDropdownExpanded, onDismissRequest = { isCategoryDropdownExpanded = false }) {
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
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val success =
                                viewModel.addTransaction(
                                    description = description,
                                    categoryId = selectedCategory?.id,
                                    amountStr = amount,
                                    accountId = selectedAccount!!.id,
                                    notes = notes.takeIf { it.isNotBlank() },
                                    date = selectedDateTime.timeInMillis,
                                    transactionType = transactionType,
                                    sourceSmsId = null,
                                )
                            if (success) {
                                navController.popBackStack()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedAccount != null && amount.isNotBlank() && description.isNotBlank(),
                    ) {
                        Text("Save Transaction")
                    }
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
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
                    },
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timePickerState =
            rememberTimePickerState(
                initialHour = selectedDateTime.get(Calendar.HOUR_OF_DAY),
                initialMinute = selectedDateTime.get(Calendar.MINUTE),
            )
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onConfirm = {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                selectedDateTime.set(Calendar.MINUTE, timePickerState.minute)
                showTimePicker = false
            },
        ) { TimePicker(state = timePickerState) }
    }
}
