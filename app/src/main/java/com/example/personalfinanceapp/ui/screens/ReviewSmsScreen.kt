package com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.personalfinanceapp.Account
import com.example.personalfinanceapp.Category
import com.example.personalfinanceapp.SettingsViewModel
import com.example.personalfinanceapp.TransactionViewModel
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.components.PotentialTransactionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSmsScreen(navController: NavController, viewModel: SettingsViewModel) {
    val potentialTransactions by viewModel.potentialTransactions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Potential Transactions") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (potentialTransactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No transactions to review.", style = MaterialTheme.typography.titleMedium)
                    Text("Go back to Settings to scan again.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "${potentialTransactions.size} potential transactions found.",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(potentialTransactions) { pt ->
                    PotentialTransactionItem(
                        transaction = pt,
                        onDismiss = { viewModel.dismissPotentialTransaction(it) },
                        onApprove = {
                            // --- UPDATED: Set the transaction for approval and navigate ---
                            viewModel.selectTransactionForApproval(it)
                            navController.navigate("approve_transaction_screen")
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsDebugScreen(navController: NavController, viewModel: SettingsViewModel) {
    val smsMessages by viewModel.smsMessages.collectAsState()
    val context = LocalContext.current
    val hasSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(hasSmsPermission) {
        if (hasSmsPermission) {
            viewModel.loadAndParseSms()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS Debug Log") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Button(
                onClick = {
                    if (hasSmsPermission) {
                        viewModel.loadAndParseSms()
                    } else {
                        Toast.makeText(context, "Grant SMS permission in settings first.", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh SMS Messages")
            }

            Spacer(Modifier.height(16.dp))

            if (smsMessages.isEmpty() && hasSmsPermission) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No messages found.")
                }
            } else if (!hasSmsPermission) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Permission not granted.", textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(smsMessages) { sms ->
                        Card {
                            Column(Modifier.padding(8.dp)) {
                                Text(sms.sender, fontWeight = FontWeight.Bold)
                                Text(sms.body, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproveTransactionScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel,
    transactionViewModel: TransactionViewModel
) {
    val potentialTransaction by settingsViewModel.selectedTransactionForApproval.collectAsState()

    // This screen is only useful if there's a transaction to approve.
    // If not, it will just show a loading state or nothing, then navigate back.
    if (potentialTransaction == null) {
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // State for this screen's form
    var description by remember { mutableStateOf(potentialTransaction?.merchantName ?: "Unknown Transaction") }
    val amount = potentialTransaction?.amount?.toString() ?: "0.0"
    var notes by remember { mutableStateOf("") }

    val accounts by transactionViewModel.allAccounts.collectAsState(initial = emptyList())
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var isAccountDropdownExpanded by remember { mutableStateOf(false) }

    val categories by transactionViewModel.allCategories.collectAsState(initial = emptyList())
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Approve Transaction") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = amount, onValueChange = {}, readOnly = true, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth()) }

            // Account Dropdown
            item {
                ExposedDropdownMenuBox(expanded = isAccountDropdownExpanded, onExpandedChange = { isAccountDropdownExpanded = !isAccountDropdownExpanded }) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "Select Account",
                        onValueChange = {}, readOnly = true, label = { Text("Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAccountDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = isAccountDropdownExpanded, onDismissRequest = { isAccountDropdownExpanded = false }) {
                        accounts.forEach { account ->
                            DropdownMenuItem(text = { Text(account.name) }, onClick = {
                                selectedAccount = account
                                isAccountDropdownExpanded = false
                            })
                        }
                    }
                }
            }

            // Category Dropdown
            item {
                ExposedDropdownMenuBox(expanded = isCategoryDropdownExpanded, onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded }) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Select Category",
                        onValueChange = {}, readOnly = true, label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = isCategoryDropdownExpanded, onDismissRequest = { isCategoryDropdownExpanded = false }) {
                        categories.forEach { category ->
                            DropdownMenuItem(text = { Text(category.name) }, onClick = {
                                selectedCategory = category
                                isCategoryDropdownExpanded = false
                            })
                        }
                    }
                }
            }

            item { OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (Optional)") }, modifier = Modifier.fillMaxWidth()) }

            item {
                Button(
                    onClick = {
                        val trx = potentialTransaction!!
                        val finalNotes = if (notes.isNotBlank()) {
                            "$notes\n\n(sms_id:${trx.sourceSmsId})"
                        } else {
                            "(sms_id:${trx.sourceSmsId})"
                        }
                        settingsViewModel.saveMerchantMapping(trx.smsSender, description)
                        val success = transactionViewModel.addTransaction(
                            description = description,
                            categoryId = selectedCategory?.id,
                            amountStr = trx.amount.toString(),
                            accountId = selectedAccount!!.id,
                            notes = notes.takeIf { it.isNotBlank() },
                            date = System.currentTimeMillis(), // Use current time for simplicity
                            transactionType = trx.transactionType
                        )
                        if (success) {
                            settingsViewModel.dismissPotentialTransaction(trx)
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedAccount != null && selectedCategory != null
                ) {
                    Text("Save Transaction")
                }
            }
        }
    }
}