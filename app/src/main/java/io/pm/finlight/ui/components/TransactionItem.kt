package io.pm.finlight.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.pm.finlight.TransactionDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionItem(
    transactionDetails: TransactionDetails,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        onClick = onClick,
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transactionDetails.transaction.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                if (!transactionDetails.transaction.notes.isNullOrBlank()) {
                    Text(
                        text = transactionDetails.transaction.notes!!,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${transactionDetails.categoryName ?: "Uncategorized"} • ${transactionDetails.accountName ?: "Unknown"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = SimpleDateFormat("dd MMM yy, h:mm a", Locale.getDefault()).format(Date(transactionDetails.transaction.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val isIncome = transactionDetails.transaction.transactionType == "income"
            val amountColor = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
            val icon = if (isIncome) Icons.Default.SouthWest else Icons.Default.NorthEast

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "₹${"%.2f".format(transactionDetails.transaction.amount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = amountColor,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = transactionDetails.transaction.transactionType,
                    tint = amountColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
fun AccountTransactionItem(transactionDetails: TransactionDetails) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = transactionDetails.transaction.description, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = SimpleDateFormat("dd MMM yy", Locale.getDefault()).format(Date(transactionDetails.transaction.date)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val isIncome = transactionDetails.transaction.transactionType == "income"
        val amountColor = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)

        Text(
            text = "₹${"%.2f".format(transactionDetails.transaction.amount)}",
            style = MaterialTheme.typography.bodyLarge,
            color = amountColor,
        )
    }
}

@Composable
fun TransactionList(
    transactions: List<TransactionDetails>,
    navController: NavController,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    if (transactions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text("No transactions yet. Add one!")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            items(transactions) { details ->
                TransactionItem(transactionDetails = details, onClick = {
                    navController.navigate("edit_transaction/${details.transaction.id}")
                })
            }
        }
    }
}
