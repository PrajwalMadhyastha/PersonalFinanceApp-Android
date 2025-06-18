package com.example.personalfinanceapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PersonalFinanceAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Call our new main screen composable
                    TransactionScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// Renamed from LoginScreen to TransactionScreen for clarity
@Composable
fun TransactionScreen(modifier: Modifier = Modifier) {
    // 1. Get an instance of our ViewModel
    val transactionViewModel: TransactionViewModel = viewModel()

    // 2. Observe the Flow and collect its values as State.
    // The 'transactions' variable will automatically update whenever the data in Room changes.
    val transactions by transactionViewModel.allTransactions.collectAsState(initial = emptyList())

    // State for the input fields
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Add New Transaction", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        // Input section
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                // Call the ViewModel function to add the transaction
                transactionViewModel.addTransaction(description, amount)
                // Clear the input fields after adding
                description = ""
                amount = ""
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Add")
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Display the list of transactions
        Text("History", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        TransactionList(transactions = transactions)
    }
}

// 3. New Composable to display the list
@Composable
fun TransactionList(transactions: List<Transaction>) {
    // LazyColumn is efficient for long lists. It only renders the items on screen.
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(transactions) { transaction ->
            TransactionItem(transaction = transaction)
            HorizontalDivider()
        }
    }
}

// New Composable for a single row in the list
@Composable
fun TransactionItem(transaction: Transaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = transaction.description, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            // Format the Long timestamp into a readable date string
            Text(text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(transaction.date)), fontSize = 12.sp, color = Color.Gray)
        }
        Text(text = "₹${"%.2f".format(transaction.amount)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}


@Preview(showBackground = true)
@Composable
fun TransactionScreenPreview() {
    PersonalFinanceAppTheme {
        // We can't fully preview the ViewModel, but we can preview the layout
        TransactionScreen()
    }
}