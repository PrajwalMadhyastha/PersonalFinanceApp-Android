package io.pm.finlight.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.pm.finlight.*
import java.util.*

@Composable
fun RecurringTransactionScreen(navController: NavController) {
    val viewModel: RecurringTransactionViewModel = viewModel()
    val recurringTransactions by viewModel.allRecurringTransactions.collectAsState(initial = emptyList())

    if (recurringTransactions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No recurring transactions set up. Tap the '+' to add one.")
        }
    } else {
        LazyColumn(
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
                HorizontalDivider()
            }
        }
    }
}
