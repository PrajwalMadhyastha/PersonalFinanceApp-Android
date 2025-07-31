// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/AccountDetailScreen.kt
// REASON: MAJOR REFACTOR - This screen has been completely redesigned to align
// with the "Project Aurora" vision. The standard Card header has been replaced
// with a more dynamic GlassPanel header that prominently features the bank's
// logo and balance. The transaction list items have also been converted to
// GlassPanels, ensuring a cohesive, modern, and high-contrast user experience.
// =================================================================================
package io.pm.finlight.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.pm.finlight.Account
import io.pm.finlight.AccountViewModel
import io.pm.finlight.utils.BankLogoHelper
import io.pm.finlight.TransactionDetails
import io.pm.finlight.ui.components.GlassPanel
import io.pm.finlight.ui.theme.ExpenseRedDark
import io.pm.finlight.ui.theme.ExpenseRedLight
import io.pm.finlight.ui.theme.IncomeGreenDark
import io.pm.finlight.ui.theme.IncomeGreenLight
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AccountDetailScreen(
    navController: NavController,
    viewModel: AccountViewModel,
    accountId: Int,
) {
    val account by viewModel.getAccountById(accountId).collectAsState(initial = null)
    val balance by viewModel.getAccountBalance(accountId).collectAsState(initial = 0.0)
    val transactions by viewModel.getTransactionsForAccount(accountId).collectAsState(initial = emptyList())

    val currentAccount = account ?: return // Don't compose if account is not loaded yet

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AccountDetailHeader(
                account = currentAccount,
                balance = balance
            )
        }

        if (transactions.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            items(transactions, key = { it.transaction.id }) { details ->
                AccountDetailTransactionItem(transactionDetails = details)
            }
        } else {
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    ) {
                        Text(
                            "No transactions for this account yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountDetailHeader(account: Account, balance: Double) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val balanceColor = when {
        balance > 0 -> MaterialTheme.colorScheme.primary
        balance < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    GlassPanel {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = BankLogoHelper.getLogoForAccount(account.name)),
                contentDescription = "${account.name} Logo",
                modifier = Modifier.size(50.dp)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Current Balance",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = currencyFormat.format(balance),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor
                )
            }
        }
    }
}

@Composable
private fun AccountDetailTransactionItem(transactionDetails: TransactionDetails) {
    val contentAlpha = if (transactionDetails.transaction.isExcluded) 0.5f else 1f
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transactionDetails.transaction.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
                Text(
                    text = dateFormatter.format(Date(transactionDetails.transaction.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                )
            }
            Spacer(Modifier.width(16.dp))

            val isIncome = transactionDetails.transaction.transactionType == "income"
            val amountColor = if (isSystemInDarkTheme()) {
                if (isIncome) IncomeGreenDark else ExpenseRedDark
            } else {
                if (isIncome) IncomeGreenLight else ExpenseRedLight
            }.copy(alpha = contentAlpha)

            Text(
                text = currencyFormat.format(transactionDetails.transaction.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = amountColor,
            )
        }
    }
}
