package io.pm.finlight.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.pm.finlight.AccountViewModel
import io.pm.finlight.ui.components.AccountTransactionItem

@Composable
fun AccountDetailScreen(
    navController: NavController,
    viewModel: AccountViewModel,
    accountId: Int,
) {
    val account by viewModel.getAccountById(accountId).collectAsState(initial = null)
    val balance by viewModel.getAccountBalance(accountId).collectAsState(initial = 0.0)
    val transactions by viewModel.getTransactionsForAccount(accountId).collectAsState(initial = emptyList())

    Column(
        modifier =
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(4.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Current Balance",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "₹${"%.2f".format(balance)}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Recent Transactions",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        if (transactions.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("No transactions for this account yet.")
            }
        } else {
            LazyColumn {
                items(transactions) { details ->
                    AccountTransactionItem(transactionDetails = details)
                    HorizontalDivider()
                }
            }
        }
    }
}
