// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/LinkTransactionScreen.kt
// REASON: NEW FILE - This screen provides the user interface for linking a
// parsed SMS to an existing manually-entered transaction. It displays the
// details of the SMS and a list of potential matching transactions that the
// user can select from.
// =================================================================================
package io.pm.finlight.ui.screens

import android.app.Application
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.gson.Gson
import io.pm.finlight.LinkTransactionViewModel
import io.pm.finlight.LinkTransactionViewModelFactory
import io.pm.finlight.PotentialTransaction
import io.pm.finlight.Transaction
import io.pm.finlight.ui.components.TransactionItem
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LinkTransactionScreen(
    navController: NavController,
    potentialTransactionJson: String
) {
    val potentialTxn = remember(potentialTransactionJson) {
        Gson().fromJson(URLDecoder.decode(potentialTransactionJson, "UTF-8"), PotentialTransaction::class.java)
    }

    val application = LocalContext.current.applicationContext as Application
    val factory = LinkTransactionViewModelFactory(application, potentialTxn)
    val viewModel: LinkTransactionViewModel = viewModel(factory = factory)

    val candidates by viewModel.linkableTransactions.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SmsDetailsCard(potentialTxn)

        Text("Select a transaction to link:", style = MaterialTheme.typography.titleMedium)

        if (candidates.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No potential matches found.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(candidates, key = { it.id }) { transaction ->
                    LinkCandidateItem(
                        transaction = transaction,
                        onClick = {
                            viewModel.linkTransaction(transaction.id) {
                                // Pop back to the screen before the review screen
                                navController.popBackStack("review_sms_screen", inclusive = true)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SmsDetailsCard(pt: PotentialTransaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SMS Details", style = MaterialTheme.typography.titleLarge)
            HorizontalDivider()
            Text("Amount: ₹${"%,.2f".format(pt.amount)}", fontWeight = FontWeight.Bold)
            Text("Type: ${pt.transactionType.replaceFirstChar { it.uppercase() }}")
            Text("Original Message: ${pt.originalMessage}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LinkCandidateItem(transaction: Transaction, onClick: () -> Unit) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.description, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(dateFormatter.format(Date(transaction.date)), style = MaterialTheme.typography.bodySmall)
            }
            Text("₹${"%,.2f".format(transaction.amount)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}
