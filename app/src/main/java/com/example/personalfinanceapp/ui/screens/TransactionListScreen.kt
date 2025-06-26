package com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import com.example.personalfinanceapp.TransactionViewModel
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.components.TransactionList

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
