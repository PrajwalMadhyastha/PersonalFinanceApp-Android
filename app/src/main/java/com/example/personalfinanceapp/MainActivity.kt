package com.example.personalfinanceapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PersonalFinanceAppTheme {
                FinanceApp()
            }
        }
    }
}

@Composable
fun FinanceApp() {
    val navController = rememberNavController()
    val transactionViewModel: TransactionViewModel = viewModel()
    val accountViewModel: AccountViewModel = viewModel()

    NavHost(navController = navController, startDestination = "transaction_list") {
        composable("transaction_list") {
            TransactionListScreen(navController = navController, viewModel = transactionViewModel)
        }
        composable("add_transaction") {
            AddTransactionScreen(navController = navController, viewModel = transactionViewModel)
        }
        composable(
            "edit_transaction/{transactionId}",
            arguments = listOf(navArgument("transactionId") { type = NavType.IntType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getInt("transactionId")
            if (transactionId != null) {
                EditTransactionScreen(
                    navController = navController,
                    viewModel = transactionViewModel,
                    transactionId = transactionId
                )
            }
        }
        composable("account_list") {
            AccountListScreen(navController = navController, viewModel = accountViewModel)
        }
        composable("add_account") {
            AddAccountScreen(navController = navController, viewModel = accountViewModel)
        }

        // --- ADDED: The route for editing an account ---
        composable(
            "edit_account/{accountId}",
            arguments = listOf(navArgument("accountId") { type = NavType.IntType })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getInt("accountId")
            if (accountId != null) {
                EditAccountScreen(
                    navController = navController,
                    viewModel = accountViewModel,
                    accountId = accountId
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(navController: NavController, viewModel: TransactionViewModel) {
    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                actions = {
                    IconButton(onClick = { navController.navigate("account_list") }) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Manage Accounts"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_transaction") }) {
                Icon(Icons.Filled.Add, contentDescription = "Add transaction")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TransactionList(transactions = transactions, navController = navController)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(navController: NavController, viewModel: TransactionViewModel) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var isAccountDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Transaction") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = isAccountDropdownExpanded,
                onExpandedChange = { isAccountDropdownExpanded = !isAccountDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedAccount?.name ?: "Select an Account",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Account") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAccountDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = isAccountDropdownExpanded,
                    onDismissRequest = { isAccountDropdownExpanded = false }
                ) {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                selectedAccount = account
                                isAccountDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    selectedAccount?.let { account ->
                        viewModel.addTransaction(description, amount, account.id)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.align(Alignment.End),
                enabled = selectedAccount != null
            ) {
                Text("Save Transaction")
            }
        }
    }
}

// --- UPDATED: Complete overhaul of this function ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(navController: NavController, viewModel: TransactionViewModel, transactionId: Int) {
    val transaction by viewModel.getTransactionById(transactionId).collectAsState(initial = null)
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var isAccountDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(transaction, accounts) {
        transaction?.let {
            description = it.description
            amount = it.amount.toString()
            if (accounts.isNotEmpty()) {
                selectedAccount = accounts.find { acc -> acc.id == it.accountId }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Transaction") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { innerPadding ->
        transaction?.let { currentTransaction ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            ) {
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = isAccountDropdownExpanded,
                    onExpandedChange = { isAccountDropdownExpanded = !isAccountDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "Select an Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAccountDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isAccountDropdownExpanded,
                        onDismissRequest = { isAccountDropdownExpanded = false }
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    selectedAccount = account
                                    isAccountDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val updatedAmount = amount.toDoubleOrNull() ?: currentTransaction.amount
                        val updatedTransaction = currentTransaction.copy(
                            description = description,
                            amount = updatedAmount,
                            accountId = selectedAccount?.id ?: currentTransaction.accountId
                        )
                        viewModel.updateTransaction(updatedTransaction)
                        navController.popBackStack()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Update Transaction")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to permanently delete this transaction?") },
            confirmButton = {
                Button(
                    onClick = {
                        transaction?.let {
                            viewModel.deleteTransaction(it)
                            showDeleteDialog = false
                            navController.popBackStack()
                        }
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}


@Composable
fun TransactionList(transactions: List<TransactionWithAccount>, navController: NavController) {
    if (transactions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No transactions yet. Add one!")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(transactions) { transactionWithAccount ->
                TransactionItem(
                    transactionWithAccount = transactionWithAccount,
                    onClick = {
                        navController.navigate("edit_transaction/${transactionWithAccount.transaction.id}")
                    }
                )
            }
        }
    }
}

// --- UPDATED: To correctly use TransactionWithAccount ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionItem(transactionWithAccount: TransactionWithAccount, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transactionWithAccount.transaction.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = transactionWithAccount.accountName ?: "Unassigned",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).format(Date(transactionWithAccount.transaction.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Text(
                text = "₹${"%.2f".format(transactionWithAccount.transaction.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun TransactionListScreenPreview() {
    PersonalFinanceAppTheme {
        // Preview might not work perfectly with real ViewModels, this is okay.
        // TransactionListScreen(rememberNavController(), viewModel())
    }
}

// --- UPDATED: To make items clickable ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(navController: NavController, viewModel: AccountViewModel) {
    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Accounts") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_account") }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Account")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            items(accounts) { account ->
                Box(modifier = Modifier.clickable {
                    navController.navigate("edit_account/${account.id}")
                }) {
                    AccountItem(account = account)
                }
                Divider()
            }
        }
    }
}

@Composable
fun AccountItem(account: Account) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = account.name, style = MaterialTheme.typography.bodyLarge)
            Text(text = account.type, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        Text(
            text = "₹${"%.2f".format(account.balance)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(navController: NavController, viewModel: AccountViewModel) {
    var accountName by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Account") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = accountName,
                onValueChange = { accountName = it },
                label = { Text("Account Name (e.g., Savings, Credit Card)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = accountType,
                onValueChange = { accountType = it },
                label = { Text("Account Type (e.g., Bank, Wallet)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (accountName.isNotBlank() && accountType.isNotBlank()) {
                        viewModel.addAccount(accountName, accountType)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.align(Alignment.End),
                enabled = accountName.isNotBlank() && accountType.isNotBlank()
            ) {
                Text("Save Account")
            }
        }
    }
}

// --- ADDED: The missing EditAccountScreen composable ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountScreen(
    navController: NavController,
    viewModel: AccountViewModel,
    accountId: Int
) {
    val account by viewModel.getAccountById(accountId).collectAsState(initial = null)
    var accountName by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(account) {
        account?.let {
            accountName = it.name
            accountType = it.type
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Account") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, "Delete Account")
                    }
                }
            )
        }
    ) { innerPadding ->
        account?.let { currentAccount ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("Account Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = accountType,
                    onValueChange = { accountType = it },
                    label = { Text("Account Type") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val updatedAccount = currentAccount.copy(
                            name = accountName,
                            type = accountType
                        )
                        viewModel.updateAccount(updatedAccount)
                        navController.popBackStack()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Update Account")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this account? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        account?.let {
                            viewModel.deleteAccount(it)
                            showDeleteDialog = false
                            navController.popBackStack()
                        }
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}