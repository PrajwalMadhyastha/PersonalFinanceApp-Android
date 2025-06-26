package io.pm.finlight.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import io.pm.finlight.TransactionViewModel
import io.pm.finlight.ui.components.TransactionList

@Composable
fun TransactionListScreen(
    navController: NavController,
    viewModel: TransactionViewModel,
) {
    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    TransactionList(
        transactions = transactions,
        navController = navController,
    )
}
