// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/AddRecurringTransactionScreen.kt
// REASON: MAJOR REFACTOR - The screen has been completely redesigned to align
// with the "Project Aurora" vision. It now mirrors the modern "Transaction
// Composer" layout, using GlassPanel components for all form elements and
// ensuring a cohesive, high-contrast, and visually appealing experience.
// BUG FIX - Replaced direct use of `GlassPanelFill` with the `GlassPanel`
// composable to ensure the component is theme-aware and to resolve the
// "Unresolved reference" build error.
// =================================================================================
package io.pm.finlight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.pm.finlight.Account
import io.pm.finlight.Category
import io.pm.finlight.RecurringTransactionViewModel
import io.pm.finlight.TransactionViewModel
import io.pm.finlight.ui.components.GlassPanel
import io.pm.finlight.ui.theme.GlassPanelBorder
import io.pm.finlight.ui.theme.PopupSurfaceDark
import io.pm.finlight.ui.theme.PopupSurfaceLight

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Recurring Rule") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                TransactionTypeToggle(
                    selectedType = transactionType,
                    onTypeSelected = { transactionType = it }
                )
            }

            item {
                GlassPanel {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = auroraTextFieldColors()
                        )
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Amount") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Text("₹") },
                            colors = auroraTextFieldColors()
                        )
                    }
                }
            }

            item {
                GlassPanel {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ExposedDropdownMenuBox(expanded = intervalExpanded, onExpandedChange = { intervalExpanded = !intervalExpanded }) {
                            OutlinedTextField(
                                value = selectedInterval,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Repeats") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = auroraTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = intervalExpanded,
                                onDismissRequest = { intervalExpanded = false },
                                modifier = Modifier.background(if (isSystemInDarkTheme()) PopupSurfaceDark else PopupSurfaceLight)
                            ) {
                                recurrenceIntervals.forEach { interval ->
                                    DropdownMenuItem(text = { Text(interval) }, onClick = {
                                        selectedInterval = interval
                                        intervalExpanded = false
                                    })
                                }
                            }
                        }

                        ExposedDropdownMenuBox(expanded = accountExpanded, onExpandedChange = { accountExpanded = !accountExpanded }) {
                            OutlinedTextField(
                                value = selectedAccount?.name ?: "Select Account",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Account") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = auroraTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = accountExpanded,
                                onDismissRequest = { accountExpanded = false },
                                modifier = Modifier.background(if (isSystemInDarkTheme()) PopupSurfaceDark else PopupSurfaceLight)
                            ) {
                                accounts.forEach { account ->
                                    DropdownMenuItem(text = { Text(account.name) }, onClick = {
                                        selectedAccount = account
                                        accountExpanded = false
                                    })
                                }
                            }
                        }

                        ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                            OutlinedTextField(
                                value = selectedCategory?.name ?: "Select Category",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = auroraTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false },
                                modifier = Modifier.background(if (isSystemInDarkTheme()) PopupSurfaceDark else PopupSurfaceLight)
                            ) {
                                categories.forEach { category ->
                                    DropdownMenuItem(text = { Text(category.name) }, onClick = {
                                        selectedCategory = category
                                        categoryExpanded = false
                                    })
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
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
                        enabled = description.isNotBlank() && amount.isNotBlank() && selectedAccount != null && selectedCategory != null,
                    ) {
                        Text("Save Rule")
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionTypeToggle(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    val glassFillColor = if (isSystemInDarkTheme()) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.Black.copy(alpha = 0.04f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(glassFillColor)
            .border(1.dp, GlassPanelBorder, CircleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val expenseSelected = selectedType == "expense"
        val incomeSelected = selectedType == "income"

        Button(
            onClick = { onTypeSelected("expense") },
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (expenseSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (expenseSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            ),
            elevation = null
        ) {
            Text("Expense", fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = { onTypeSelected("income") },
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (incomeSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (incomeSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            ),
            elevation = null
        ) {
            Text("Income", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun auroraTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
)
