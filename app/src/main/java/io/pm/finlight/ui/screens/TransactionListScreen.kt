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
    filterType: String?
) {
    LaunchedEffect(filterType) {
        viewModel.setTransactionTypeFilter(filterType)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.setTransactionTypeFilter(null)
        }
    }

    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    TransactionList(
        transactions = transactions,
        navController = navController
        // --- FIX: Removed the 'modifier' parameter that was causing the compile error ---
    )
}
