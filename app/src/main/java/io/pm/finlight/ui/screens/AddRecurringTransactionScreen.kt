package io.pm.finlight.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.pm.finlight.Account
import io.pm.finlight.Category
import io.pm.finlight.RecurringTransactionViewModel
import io.pm.finlight.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringTransactionScreen(navController: NavController) {
    val recurringViewModel: RecurringTransactionViewModel = viewModel()
    val transactionViewModel: TransactionViewModel = viewModel()

    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("expense") }

    val recurrenceIntervals = listOf("Daily", "Weekly", "Monthly", "Yearly")
    var selectedInterval by remember { mutableStateOf(recurrenceIntervals[2]) }
    var intervalExpanded by remember { mutableStateOf(false) }

    val accounts by transactionViewModel.allAccounts.collectAsState(initial = emptyList())
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var accountExpanded by remember { mutableStateOf(false) }

    val categories by transactionViewModel.allCategories.collectAsState(initial = emptyList())
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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
            Row {
                FilterChip(selected = transactionType == "expense", onClick = {
                    transactionType = "expense"
                }, label = { Text("Expense") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = transactionType == "income", onClick = {
                    transactionType = "income"
                }, label = { Text("Income") }, modifier = Modifier.weight(1f))
            }
        }

        item {
            ExposedDropdownMenuBox(expanded = intervalExpanded, onExpandedChange = { intervalExpanded = !intervalExpanded }) {
                OutlinedTextField(value = selectedInterval, onValueChange = {
                }, readOnly = true, label = {
                    Text("Repeats")
                }, trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded)
                }, modifier = Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(expanded = intervalExpanded, onDismissRequest = { intervalExpanded = false }) {
                    recurrenceIntervals.forEach {
                            interval ->
                        DropdownMenuItem(text = { Text(interval) }, onClick = {
                            selectedInterval = interval
                            intervalExpanded = false
                        })
                    }
                }
            }
        }

        item {
            ExposedDropdownMenuBox(expanded = accountExpanded, onExpandedChange = { accountExpanded = !accountExpanded }) {
                OutlinedTextField(value = selectedAccount?.name ?: "Select Account", onValueChange = {
                }, readOnly = true, label = {
                    Text("Account")
                }, trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = accountExpanded,
                    )
                }, modifier = Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                    accounts.forEach {
                            account ->
                        DropdownMenuItem(text = { Text(account.name) }, onClick = {
                            selectedAccount = account
                            accountExpanded = false
                        })
                    }
                }
            }
        }

        item {
            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                OutlinedTextField(value = selectedCategory?.name ?: "Select Category", onValueChange = {
                }, readOnly = true, label = {
                    Text("Category")
                }, trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                }, modifier = Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    categories.forEach {
                            category ->
                        DropdownMenuItem(text = { Text(category.name) }, onClick = {
                            selectedCategory = category
                            categoryExpanded = false
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
                        val amountDouble = amount.toDoubleOrNull()
                        if (amountDouble != null && selectedAccount != null) {
                            recurringViewModel.addRecurringTransaction(
                                description,
                                amountDouble,
                                transactionType,
                                selectedInterval,
                                System.currentTimeMillis(),
                                selectedAccount!!.id,
                                selectedCategory?.id,
                            )
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = description.isNotBlank() && amount.isNotBlank() && selectedAccount != null,
                ) {
                    Text("Save Rule")
                }
            }
        }
    }
}
