package io.pm.finlight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.pm.finlight.CategoryIconHelper
import io.pm.finlight.TransactionDetails
import java.text.SimpleDateFormat
import java.util.*

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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CategoryIconHelper.getIconBackgroundColor(transactionDetails.categoryColorKey ?: "gray_light")),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryIconHelper.getIcon(transactionDetails.categoryIconKey ?: "category"),
                    contentDescription = transactionDetails.categoryName,
                    tint = Color.Black
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = transactionDetails.transaction.description, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                if (!transactionDetails.transaction.notes.isNullOrBlank()) {
                    Text(text = transactionDetails.transaction.notes!!, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(text = "${transactionDetails.categoryName ?: "Uncategorized"} • ${transactionDetails.accountName ?: "Unknown"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Text(text = SimpleDateFormat("dd MMM yy, h:mm a", Locale.getDefault()).format(Date(transactionDetails.transaction.date)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            val isIncome = transactionDetails.transaction.transactionType == "income"
            val amountColor = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
            val icon = if (isIncome) Icons.Default.SouthWest else Icons.Default.NorthEast

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "₹${"%.2f".format(transactionDetails.transaction.amount)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = amountColor)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(imageVector = icon, contentDescription = transactionDetails.transaction.transactionType, tint = amountColor, modifier = Modifier.size(20.dp))
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
) {
    if (transactions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("No transactions yet. Add one!")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            items(transactions, key = { it.transaction.id }) { details ->
                TransactionItem(
                    transactionDetails = details,
                    onClick = {
                        // --- FIX: Ensure navigation is to the detail screen ---
                        navController.navigate("transaction_detail/${details.transaction.id}")
                    }
                )
            }
        }
    }
}
