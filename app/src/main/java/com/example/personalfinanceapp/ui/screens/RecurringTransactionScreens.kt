package com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.personalfinanceapp.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionScreen(navController: NavController) {
    val viewModel: RecurringTransactionViewModel = viewModel()
    val recurringTransactions by viewModel.allRecurringTransactions.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurring Transactions") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_recurring_transaction") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Recurring Transaction")
            }
        }
    ) { innerPadding ->
        if (recurringTransactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No recurring transactions set up.")
            }
        } else {
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                items(recurringTransactions) { rule ->
                    ListItem(
                        headlineContent = { Text(rule.description) },
                        supportingContent = { Text("₹${rule.amount} every ${rule.recurrenceInterval.lowercase(Locale.getDefault())}") },
                        trailingContent = {
                            Text(
                                text = rule.transactionType.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                                color = if (rule.transactionType == "expense") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    Divider()
                }
            }
        }
    }
}