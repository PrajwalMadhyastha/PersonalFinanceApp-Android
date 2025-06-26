package com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.personalfinanceapp.*
import java.net.URLEncoder

@Composable
fun ReviewSmsScreen(navController: NavController, viewModel: SettingsViewModel = viewModel()) {
    val potentialTransactions by viewModel.potentialTransactions.collectAsState()
    val smsScanStartDate by viewModel.smsScanStartDate.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.rescanSms(smsScanStartDate)
    }

    if (potentialTransactions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("No new transactions to review.", style = MaterialTheme.typography.titleMedium)
                Text("Go back to Settings and tap 'Scan' to find transactions.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
        }
    } else {
        LazyColumn(
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
                    onApprove = { transaction ->
                        val merchant = URLEncoder.encode(transaction.merchantName ?: "Unknown", "UTF-8")
                        val route = "approve_transaction_screen/${transaction.amount}/${transaction.transactionType}/${merchant}/${transaction.sourceSmsId}/${transaction.smsSender}"
                        navController.navigate(route)
                    }
                )
            }
        }
    }
}

@Composable
fun PotentialTransactionItem(
    transaction: PotentialTransaction,
    onDismiss: (PotentialTransaction) -> Unit,
    onApprove: (PotentialTransaction) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            val amountColor = if (transaction.transactionType == "expense") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = transaction.merchantName ?: "Unknown Merchant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(text = "₹${"%.2f".format(transaction.amount)}", style = MaterialTheme.typography.titleLarge, color = amountColor, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Text(text = "Type: ${transaction.transactionType.replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(text = "Original Message: ${transaction.originalMessage}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = { onDismiss(transaction) }) { Text("Dismiss") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onApprove(transaction) }) { Text("Approve") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproveTransactionScreen(
    navController: NavController,
    transactionViewModel: TransactionViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    amount: Float,
    transactionType: String,
    merchant: String,
    smsId: Long,
    smsSender: String
) {
    var description by remember { mutableStateOf(merchant) }
    var notes by remember { mutableStateOf("") }
    var selectedTransactionType by remember(transactionType) { mutableStateOf(transactionType) }
    val transactionTypes = listOf("Expense", "Income")

    val accounts by transactionViewModel.allAccounts.collectAsState(initial = emptyList())
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var isAccountDropdownExpanded by remember { mutableStateOf(false) }

    val categories by transactionViewModel.allCategories.collectAsState(initial = emptyList())
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description / Merchant") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = amount.toString(), onValueChange = {}, readOnly = true, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth()) }
        item {
            TabRow(selectedTabIndex = if (selectedTransactionType == "expense") 0 else 1) {
                transactionTypes.forEachIndexed { index, title ->
                    Tab(selected = (if (selectedTransactionType == "expense") 0 else 1) == index, onClick = { selectedTransactionType = if (index == 0) "expense" else "income" }, text = { Text(title) })
                }
            }
        }
        item {
            ExposedDropdownMenuBox(expanded = isAccountDropdownExpanded, onExpandedChange = { isAccountDropdownExpanded = !isAccountDropdownExpanded }) {
                OutlinedTextField(value = selectedAccount?.name ?: "Select Account", onValueChange = {}, readOnly = true, label = { Text("Account") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAccountDropdownExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(expanded = isAccountDropdownExpanded, onDismissRequest = { isAccountDropdownExpanded = false }) {
                    accounts.forEach { account ->
                        DropdownMenuItem(text = { Text(account.name) }, onClick = { selectedAccount = account; isAccountDropdownExpanded = false })
                    }
                }
            }
        }
        item {
            ExposedDropdownMenuBox(expanded = isCategoryDropdownExpanded, onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded }) {
                OutlinedTextField(value = selectedCategory?.name ?: "Select Category", onValueChange = {}, readOnly = true, label = { Text("Category") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(expanded = isCategoryDropdownExpanded, onDismissRequest = { isCategoryDropdownExpanded = false }) {
                    categories.forEach { category ->
                        DropdownMenuItem(text = { Text(category.name) }, onClick = { selectedCategory = category; isCategoryDropdownExpanded = false })
                    }
                }
            }
        }
        item { OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (Optional)") }, modifier = Modifier.fillMaxWidth()) }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(onClick = {
                    settingsViewModel.saveMerchantMapping(smsSender, description)
                    val success = transactionViewModel.addTransaction(description = description, categoryId = selectedCategory?.id, amountStr = amount.toString(), accountId = selectedAccount!!.id, notes = notes.takeIf { it.isNotBlank() }, date = System.currentTimeMillis(), transactionType = selectedTransactionType, sourceSmsId = smsId)
                    if (success) {
                        navController.popBackStack()
                    }
                }, modifier = Modifier.weight(1f), enabled = selectedAccount != null && selectedCategory != null) { Text("Save Transaction") }
            }
        }
    }
}

@Composable
fun SmsDebugScreen(navController: NavController, viewModel: SettingsViewModel = viewModel()) {
    val smsMessages by viewModel.potentialTransactions.collectAsState()
    val context = LocalContext.current
    val hasSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(hasSmsPermission) {
        if (hasSmsPermission) {
            viewModel.rescanSms(null)
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Button(
            onClick = {
                if (hasSmsPermission) {
                    viewModel.rescanSms(null)
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
                            Text(sms.smsSender, fontWeight = FontWeight.Bold)
                            Text(sms.originalMessage, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
