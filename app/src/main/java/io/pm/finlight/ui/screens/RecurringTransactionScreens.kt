// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/RecurringTransactionScreens.kt
// REASON: MAJOR REFACTOR - The screen has been fully redesigned to align with
// the "Project Aurora" vision. The standard ListItem has been replaced with a
// custom GlassPanel component, and all text colors have been updated to be
// theme-aware, ensuring a consistent, high-contrast user experience.
// =================================================================================
package io.pm.finlight.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.pm.finlight.*
import io.pm.finlight.ui.components.GlassPanel
import java.text.NumberFormat
import java.util.*

@Composable
fun RecurringTransactionScreen(navController: NavController) {
    val viewModel: RecurringTransactionViewModel = viewModel()
    val recurringTransactions by viewModel.allRecurringTransactions.collectAsState(initial = emptyList())

    if (recurringTransactions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No recurring transactions set up. Tap the '+' to add one.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(recurringTransactions, key = { it.id }) { rule ->
                RecurringTransactionItem(rule = rule)
            }
        }
    }
}

@Composable
private fun RecurringTransactionItem(rule: RecurringTransaction) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val amountColor = if (rule.transactionType == "expense") {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Repeats ${rule.recurrenceInterval}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = currencyFormat.format(rule.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}
