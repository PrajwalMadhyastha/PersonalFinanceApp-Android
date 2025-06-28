package io.pm.finlight.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import io.pm.finlight.TransactionViewModel
import io.pm.finlight.ui.components.TransactionList

@Composable
fun TransactionListScreen(
    navController: NavController,
    viewModel: TransactionViewModel,
    // --- NEW: Accept an optional filter type ---
    filterType: String?
) {
    // --- NEW: Apply the filter when the screen is composed ---
    LaunchedEffect(filterType) {
        viewModel.setTransactionTypeFilter(filterType)
    }

    // --- NEW: Reset the filter when the user navigates away ---
    DisposableEffect(Unit) {
        onDispose {
            viewModel.setTransactionTypeFilter(null)
        }
    }

    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    TransactionList(
        transactions = transactions,
        navController = navController,
    )
}
