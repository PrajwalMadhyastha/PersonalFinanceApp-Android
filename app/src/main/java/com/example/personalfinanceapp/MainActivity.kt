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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.map
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
    val budgetViewModel: BudgetViewModel = viewModel()
    val dashboardViewModel: DashboardViewModel = viewModel()

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(navController = navController, viewModel = dashboardViewModel)
        }
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
        composable(
            "account_detail/{accountId}",
            arguments = listOf(navArgument("accountId") { type = NavType.IntType })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getInt("accountId")
            if (accountId != null) {
                AccountDetailScreen(
                    navController = navController,
                    viewModel = accountViewModel,
                    accountId = accountId
                )
            }
        }
        composable("budget_screen") {
            BudgetScreen(navController = navController, viewModel = budgetViewModel)
        }
        composable("add_budget") {
            AddBudgetScreen(navController = navController, viewModel = budgetViewModel)
        }
    }
}

// =================================================================================
// Transaction-related Screens
// =================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(navController: NavController, viewModel: TransactionViewModel) {
    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                actions = {
                    // --- UPDATED: Now two icons are here for easy access ---

                    // 1. The button for Budgets
                    IconButton(onClick = { navController.navigate("budget_screen") }) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = "Budgets"
                        )
                    }

                    // 2. The button for Accounts
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
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add transaction")
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
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
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
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete")
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
                    text = SimpleDateFormat("dd MMM yy, h:mm a", Locale.getDefault()).format(Date(transactionWithAccount.transaction.date)),
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

// =================================================================================
// Account-related Screens
// =================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(navController: NavController, viewModel: AccountViewModel) {
    val accountsWithBalance by viewModel.accountsWithBalance.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Accounts") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("budget_screen") }) {
                        Icon(imageVector = Icons.Default.Assessment, contentDescription = "Budgets")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_account") }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Account")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            items(accountsWithBalance) { accountWithBalance ->
                Box(modifier = Modifier.clickable {
                    navController.navigate("account_detail/${accountWithBalance.account.id}")
                }) {
                    AccountItem(accountWithBalance = accountWithBalance)
                }
                Divider()
            }
        }
    }
}

@Composable
fun AccountItem(accountWithBalance: AccountWithBalance) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = accountWithBalance.account.name, style = MaterialTheme.typography.bodyLarge)
            Text(text = accountWithBalance.account.type, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        Text(
            text = "₹${"%.2f".format(accountWithBalance.balance)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (accountWithBalance.balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
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
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
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
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete Account")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    navController: NavController,
    viewModel: AccountViewModel,
    accountId: Int
) {
    val account by viewModel.getAccountById(accountId).collectAsState(initial = null)
    val balance by viewModel.getAccountBalance(accountId).collectAsState(initial = 0.0)
    val transactions by viewModel.getTransactionsForAccount(accountId).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(account?.name ?: "Account Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Current Balance",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${"%.2f".format(balance)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (transactions.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("No transactions for this account yet.")
                }
            } else {
                LazyColumn {
                    items(transactions) { transaction ->
                        AccountTransactionItem(transaction = transaction)
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun AccountTransactionItem(transaction: Transaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = transaction.description, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = SimpleDateFormat("dd MMM yy", Locale.getDefault()).format(Date(transaction.date)),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        Text(
            text = "₹${"%.2f".format(transaction.amount)}",
            style = MaterialTheme.typography.bodyLarge,
            color = if (transaction.amount < 0) Color.Red else Color(0xFF006400)
        )
    }
}


// =================================================================================
// Budget-related Screens
// =================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(navController: NavController, viewModel: BudgetViewModel) {
    val budgets by viewModel.budgetsForCurrentMonth.collectAsState(initial = emptyList())
    val monthYear = viewModel.getCurrentMonthYearString()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets for $monthYear") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_budget") }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Budget")
            }
        }
    ) { innerPadding ->
        if (budgets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No budgets set for this month. Add one!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(budgets) { budget ->
                    // Here we use our new, more powerful BudgetItem, passing the ViewModel down
                    BudgetItem(budget = budget, viewModel = viewModel)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun BudgetItem(budget: Budget, viewModel: BudgetViewModel) {
    // 1. Create a flow that transforms the raw spending data into the value we need (a positive Double).
    // We use remember() so this flow is not recreated on every recomposition.
    val spendingFlow = remember(budget.categoryName) {
        viewModel.getActualSpending(budget.categoryName).map { spending ->
            // If spending is null (no transactions), default to 0.0.
            // Use Math.abs() because expenses are negative, but progress is positive.
            Math.abs(spending ?: 0.0)
        }
    }
    // 2. Collect the transformed flow as a state that the UI can react to.
    val actualSpending by spendingFlow.collectAsState(initial = 0.0)

    // 3. Calculate progress and remaining amount.
    val progress = if (budget.amount > 0) {
        (actualSpending / budget.amount).toFloat()
    } else {
        0f
    }
    val amountRemaining = budget.amount - actualSpending

    // 4. Determine the color of the progress bar.
    val progressColor = when {
        progress > 1f -> MaterialTheme.colorScheme.error
        progress > 0.8f -> Color(0xFFFBC02D) // Amber
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = budget.categoryName,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "₹${"%.2f".format(budget.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = progressColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent: ₹${"%.2f".format(actualSpending)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Remaining: ₹${"%.2f".format(amountRemaining)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (amountRemaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, viewModel: DashboardViewModel) {
    val netWorth by viewModel.netWorth.collectAsState(initial = 0.0)
    val monthlyIncome by viewModel.monthlyIncome.collectAsState(initial = 0.0)
    val monthlyExpenses by viewModel.monthlyExpenses.collectAsState(initial = 0.0)
    val recentTransactions by viewModel.recentTransactions.collectAsState(initial = emptyList())
    val budgetStatus by viewModel.budgetStatus.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = { navController.navigate("account_list") }) {
                        Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = "Accounts")
                    }
                    IconButton(onClick = { navController.navigate("budget_screen") }) {
                        Icon(imageVector = Icons.Default.Assessment, contentDescription = "Budgets")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_transaction") }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Transaction")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { NetWorthCard(netWorth = netWorth) }
            item { MonthlySummaryCard(income = monthlyIncome, expenses = monthlyExpenses) }
            item { BudgetWatchCard(budgetStatus = budgetStatus, navController) }
            item { RecentActivityCard(transactions = recentTransactions, navController) }
        }
    }
}

@Composable
fun NetWorthCard(netWorth: Double) {
    Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Net Worth", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "₹${"%.2f".format(netWorth)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MonthlySummaryCard(income: Double, expenses: Double) {
    Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("This Month's Summary", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Income", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = "₹${"%.2f".format(income)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF006400)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Expenses", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = "₹${"%.2f".format(expenses)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetWatchCard(budgetStatus: List<BudgetWithSpending>, navController: NavController) {
    Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Budget Watch", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (budgetStatus.isEmpty()) {
                Text("No budgets set for this month.", style = MaterialTheme.typography.bodyMedium)
            } else {
                budgetStatus.forEach { budgetWithSpending ->
                    BudgetStatusItem(budgetWithSpending)
                }
            }
        }
    }
}

@Composable
fun BudgetStatusItem(item: BudgetWithSpending) {
    val progress = if (item.budget.amount > 0) (item.spent / item.budget.amount).toFloat() else 0f
    Column {
        Row {
            Text(item.budget.categoryName, modifier = Modifier.weight(1f))
            Text("₹${"%.2f".format(item.spent)} / ₹${"%.2f".format(item.budget.amount)}")
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            color = if(progress > 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun RecentActivityCard(transactions: List<TransactionWithAccount>, navController: NavController) {
    Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Activity", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { navController.navigate("transaction_list") }) {
                    Text("View All")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            transactions.forEach { transaction ->
                TransactionItem(transactionWithAccount = transaction, onClick = {
                    navController.navigate("edit_transaction/${transaction.transaction.id}")
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetScreen(navController: NavController, viewModel: BudgetViewModel) {
    var categoryName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Budget") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
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
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text("Category Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Budget Amount") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (categoryName.isNotBlank() && amount.isNotBlank()) {
                        viewModel.addBudget(categoryName, amount)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.align(Alignment.End),
                enabled = categoryName.isNotBlank() && amount.isNotBlank()
            ) {
                Text("Save Budget")
            }
        }
    }
}