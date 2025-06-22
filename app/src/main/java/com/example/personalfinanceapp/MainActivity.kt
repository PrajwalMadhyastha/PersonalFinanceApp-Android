package com.example.personalfinanceapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import kotlinx.coroutines.flow.map
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.PieData
import androidx.navigation.navDeepLink
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import androidx.activity.compose.rememberLauncherForActivityResult
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin

// --- Helper function for compact number formatting ---
private fun formatAmountCompact(amount: Float): String {
    return when {
        amount >= 1_000_000 -> "₹${"%.1f".format(amount / 1_000_000)}M"
        amount >= 1_000 -> "₹${"%.1f".format(amount / 1_000)}k"
        else -> "₹${"%.0f".format(amount)}"
    }
}

// --- Navigation Destinations ---
sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Dashboard : BottomNavItem("dashboard", Icons.Filled.Home, "Dashboard")
    object Transactions : BottomNavItem("transaction_list", Icons.Filled.Receipt, "History")
    object Reports : BottomNavItem("reports_screen", Icons.Filled.Assessment, "Reports")
    object Settings : BottomNavItem("settings_screen", Icons.Filled.Settings, "Settings")
}

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

// --- Root Composable for the App ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceApp() {
    val navController = rememberNavController()
    val dashboardViewModel: DashboardViewModel = viewModel()
    val budgetViewModel: BudgetViewModel = viewModel()
    val transactionViewModel: TransactionViewModel = viewModel()
    val accountViewModel: AccountViewModel = viewModel()
    val categoryViewModel: CategoryViewModel = viewModel()
    val reportsViewModel: ReportsViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val bottomNavItems = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Transactions,
        BottomNavItem.Reports,
        BottomNavItem.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Primary Screens from Bottom Nav
            composable(BottomNavItem.Dashboard.route) { DashboardScreen(navController, dashboardViewModel, budgetViewModel) }
            composable(BottomNavItem.Transactions.route) { TransactionListScreen(navController, transactionViewModel) }
            composable(BottomNavItem.Reports.route) { ReportsScreen(navController, reportsViewModel) }
            composable(BottomNavItem.Settings.route) { SettingsScreen(navController, settingsViewModel) }
            composable("review_sms_screen") { ReviewSmsScreen(navController, settingsViewModel) }

            // --- THIS WAS THE MISSING ROUTE CAUSING THE CRASH ---
            composable("sms_debug_screen") { SmsDebugScreen(navController, settingsViewModel) }

            // All other secondary screens
            composable("add_transaction") { AddTransactionScreen(navController, transactionViewModel) }
            composable("edit_transaction/{transactionId}", arguments = listOf(navArgument("transactionId") { type = NavType.IntType })) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getInt("transactionId")
                if (transactionId != null) { EditTransactionScreen(navController, transactionViewModel, transactionId) }
            }
            composable("account_list") { AccountListScreen(navController, accountViewModel) }
            composable("add_account") { AddAccountScreen(navController, accountViewModel) }
            composable(
                "edit_account/{accountId}",
                arguments = listOf(navArgument("accountId") { type = NavType.IntType })
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getInt("accountId")
                if (accountId != null) { EditAccountScreen(navController, accountViewModel, accountId) }
            }
            composable(
                "account_detail/{accountId}",
                arguments = listOf(navArgument("accountId") { type = NavType.IntType })
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getInt("accountId")
                if (accountId != null) { AccountDetailScreen(navController, accountViewModel, accountId) }
            }
            composable("budget_screen") { BudgetScreen(navController, budgetViewModel) }
            composable("add_budget") { AddBudgetScreen(navController, budgetViewModel) }
            composable("category_list") { CategoryListScreen(navController, categoryViewModel) }
            composable(
                "edit_category/{categoryId}",
                arguments = listOf(navArgument("categoryId") { type = NavType.IntType })
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getInt("categoryId")
                if (categoryId != null) { EditCategoryScreen(navController, categoryViewModel, categoryId) }
            }
        }
    }
}

// --- Dashboard Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel,
    budgetViewModel: BudgetViewModel
) {
    val netWorth by viewModel.netWorth.collectAsState()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlyExpenses by viewModel.monthlyExpenses.collectAsState()
    val overallBudget by viewModel.overallMonthlyBudget.collectAsState()
    val safeToSpend by viewModel.safeToSpendPerDay.collectAsState()
    val budgetStatus by viewModel.budgetStatus.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = { navController.navigate(BottomNavItem.Settings.route) }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
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
            item {
                OverallBudgetCard(
                    totalBudget = overallBudget,
                    amountSpent = monthlyExpenses.toFloat()
                )
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(label = "Monthly Income", amount = monthlyIncome.toFloat(), modifier = Modifier.weight(1f))
                    StatCard(label = "Total Budget", amount = overallBudget, modifier = Modifier.weight(1f))
                    StatCard(label = "Safe to Spend", amount = safeToSpend, modifier = Modifier.weight(1f), isPerDay = true)
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilledTonalButton(
                        onClick = { navController.navigate(BottomNavItem.Reports.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }},
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Trends")
                    }
                    FilledTonalButton(
                        onClick = { navController.navigate(BottomNavItem.Reports.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }},
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Categories")
                    }
                }
            }
            item { NetWorthCard(netWorth) }
            item { RecentActivityCard(recentTransactions, navController) }
            item { BudgetWatchCard(budgetStatus, budgetViewModel) }
        }
    }
}

// --- Settings Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    var hasSmsPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasSmsPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "SMS Permission Granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "SMS Permission Denied.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val currentBudget by viewModel.overallBudget.collectAsState()
            var budgetInput by remember(currentBudget) { mutableStateOf(if (currentBudget > 0) currentBudget.toString() else "") }

            Text("General", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            OutlinedTextField(
                value = budgetInput,
                onValueChange = { budgetInput = it },
                label = { Text("Overall Monthly Budget") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Text("₹") }
            )
            Button(
                onClick = { viewModel.saveOverallBudget(budgetInput); Toast.makeText(context, "Budget Saved!", Toast.LENGTH_SHORT).show() },
                modifier = Modifier.align(Alignment.End)
            ) { Text("Save Budget") }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Permissions", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            ListItem(
                headlineContent = { Text("Read SMS for Transactions") },
                supportingContent = { Text("Allow the app to automatically detect transactions.") },
                leadingContent = { Icon(Icons.Default.Message, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = hasSmsPermission,
                        onCheckedChange = { if (!hasSmsPermission) { permissionLauncher.launch(Manifest.permission.READ_SMS) } }
                    )
                }
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text("SMS Automation", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (hasSmsPermission) {
                            viewModel.loadAndParseSms()
                            navController.navigate("review_sms_screen")
                        } else {
                            Toast.makeText(context, "Please grant SMS permission first.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Review SMS")
                }
                OutlinedButton(
                    onClick = { navController.navigate("sms_debug_screen") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SMS Debug")
                }
            }
        }
    }
}

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
                        onApprove = { /* Logic to be added later */ }
                    )
                }
            }
        }
    }
}

// --- NEW: Composable for a single potential transaction item ---
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
                Text(
                    text = transaction.merchantName ?: "Unknown Merchant",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "₹${"%.2f".format(transaction.amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = amountColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Type: ${transaction.transactionType.replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Original Message: ${transaction.originalMessage}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = { onDismiss(transaction) }) {
                    Text("Dismiss")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onApprove(transaction) }) {
                    Text("Approve")
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



// --- StatCard Composable (UPDATED) ---
@Composable
fun StatCard(
    label: String,
    amount: Float,
    modifier: Modifier = Modifier,
    isPerDay: Boolean = false
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            // --- UPDATED: Call the top-level helper function directly ---
            Text(
                text = "${formatAmountCompact(amount)}${if (isPerDay) "/day" else ""}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
fun OverallBudgetCard(totalBudget: Float, amountSpent: Float) {
    if (totalBudget <= 0) {
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text("Monthly Budget", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // The Liquid Tumbler visualization
                LiquidTumbler(
                    progress = (amountSpent / totalBudget),
                    modifier = Modifier.size(120.dp)
                )

                // The text summary
                Column {
                    Text("Spent", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = "₹${"%.2f".format(amountSpent)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Remaining", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = "₹${"%.2f".format(totalBudget - amountSpent)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// --- LiquidTumbler Composable (CORRECTED) ---
@Composable
fun LiquidTumbler(progress: Float, modifier: Modifier = Modifier) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "LiquidFillAnimation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "WaveAnimation")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing)
        ), label = "WaveOffset"
    )

    // --- CORRECTED: Resolve colors and pixel values in the Composable context ---
    val waterColor = when {
        clampedProgress >= 1f -> MaterialTheme.colorScheme.error
        clampedProgress > 0.8f -> Color(0xFFFBC02D) // Amber
        else -> MaterialTheme.colorScheme.primary
    }
    val glassColor = MaterialTheme.colorScheme.onSurfaceVariant
    val strokeWidthPx = with(LocalDensity.current) { 4.dp.toPx() }


    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val glassPath = Path().apply {
                moveTo(width * 0.1f, height * 0.05f)
                lineTo(width * 0.2f, height * 0.95f)
                quadraticBezierTo(width * 0.5f, height * 1.05f, width * 0.8f, height * 0.95f)
                lineTo(width * 0.9f, height * 0.05f)
                close()
            }

            // --- CORRECTED: Use the pre-resolved values ---
            drawPath(
                path = glassPath,
                color = glassColor,
                style = Stroke(width = strokeWidthPx)
            )

            clipPath(glassPath) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(waterColor.copy(alpha = 0.5f), waterColor),
                        startY = height * (1 - animatedProgress),
                        endY = height
                    ),
                    topLeft = Offset(0f, height * (1 - animatedProgress)),
                    size = size
                )

                val wavePath = Path().apply {
                    moveTo(-width, height * (1 - animatedProgress))
                    for (i in 0..width.toInt() * 2) {
                        lineTo(
                            i.toFloat(),
                            height * (1 - animatedProgress) + sin((i * 0.03f) + (waveOffset * Math.PI.toFloat())) * 5f
                        )
                    }
                    lineTo(width * 2, height)
                    lineTo(-width, height)
                    close()
                }
                drawPath(path = wavePath, color = waterColor)
            }
        }
        Text(
            text = "${(clampedProgress * 100).toInt()}%",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// --- NEW: Composable to host the MPAndroidChart PieChart ---
//@Composable
//fun BudgetPieChart(totalBudget: Float, amountSpent: Float) {
//    val remaining = totalBudget - amountSpent
//    val percentageSpent = if (totalBudget > 0) (amountSpent / totalBudget) * 100 else 0f
//
//    val spentColor = MaterialTheme.colorScheme.error.toArgb()
//    val remainingColor = MaterialTheme.colorScheme.primaryContainer.toArgb()
//
//    // --- CORRECTED: Get the color in the Composable context ---
//    val centerTextColor = MaterialTheme.colorScheme.onSurface.toArgb()
//
//    AndroidView(
//        factory = { context ->
//            PieChart(context).apply {
//                description.isEnabled = false
//                legend.isEnabled = false
//                isDrawHoleEnabled = true
//                setHoleColor(AndroidColor.TRANSPARENT)
//                setUsePercentValues(true)
//                setEntryLabelColor(AndroidColor.BLACK)
//                setEntryLabelTypeface(Typeface.DEFAULT_BOLD)
//            }
//        },
//        update = { chart ->
//            val entries = mutableListOf<PieEntry>()
//            if (amountSpent > 0) {
//                entries.add(PieEntry(amountSpent, "Spent"))
//            }
//            if (remaining > 0) {
//                entries.add(PieEntry(remaining, "Remaining"))
//            }
//
//            val dataSet = PieDataSet(entries, "Budget").apply {
//                colors = listOf(spentColor, remainingColor)
//                setDrawValues(false)
//            }
//
//            chart.data = PieData(dataSet)
//
//            chart.centerText = "%.1f%%".format(percentageSpent)
//            chart.setCenterTextSize(24f)
//            chart.setCenterTextTypeface(Typeface.DEFAULT_BOLD)
//            // --- CORRECTED: Use the color variable here ---
//            chart.setCenterTextColor(centerTextColor)
//
//            chart.invalidate()
//        },
//        modifier = Modifier.fillMaxSize()
//    )
//}

// --- NEW: A "More" screen for navigating to management pages ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("More Options") })
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            ListItem(
                headlineContent = { Text("Manage Accounts") },
                leadingContent = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                modifier = Modifier.clickable { navController.navigate("account_list") }
            )
            Divider()
            ListItem(
                headlineContent = { Text("Manage Categories") },
                leadingContent = { Icon(Icons.Default.Category, contentDescription = null) },
                modifier = Modifier.clickable { navController.navigate("category_list") }
            )
            Divider()
            ListItem(
                headlineContent = { Text("Manage Budgets") },
                leadingContent = { Icon(Icons.Default.Assessment, contentDescription = null) },
                modifier = Modifier.clickable { navController.navigate("budget_screen") }
            )
            Divider()
        }
    }
}

// --- UPDATED: TransactionListScreen now has a simpler TopAppBar ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(navController: NavController, viewModel: TransactionViewModel) {
    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Transaction History") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_transaction") }) {
                Icon(Icons.Filled.Add, contentDescription = "Add transaction")
            }
        }
    ) { innerPadding ->
        TransactionList(transactions = transactions, navController = navController)
    }
}

// --- UPDATED: ReportsScreen now has a simpler TopAppBar ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(navController: NavController, viewModel: ReportsViewModel = viewModel()) {
    val pieData by viewModel.spendingByCategoryPieData.collectAsState(initial = null)
    val trendDataPair by viewModel.monthlyTrendData.collectAsState(initial = null)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Reports") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Pie Chart Card (Unchanged) ---
            item {
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Spending by Category for ${viewModel.monthYear}", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        if (pieData == null || pieData?.entryCount == 0) {
                            Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                                Text("No expense data for this month.")
                            }
                        } else {
                            AndroidView(
                                factory = { context ->
                                    PieChart(context).apply {
                                        description.isEnabled = false; isDrawHoleEnabled = true; setHoleColor(AndroidColor.TRANSPARENT); setEntryLabelColor(AndroidColor.BLACK); setEntryLabelTextSize(12f); legend.isEnabled = false
                                    }
                                },
                                update = { chart -> chart.data = pieData; chart.invalidate() },
                                modifier = Modifier.fillMaxWidth().height(300.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            ChartLegend(pieData)
                        }
                    }
                }
            }

            // --- Bar Chart Card (REFINED) ---
            item {
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Income vs. Expense Trend", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        if (trendDataPair != null && trendDataPair!!.first.entryCount > 0) {
                            // This is the refined BarChart implementation
                            GroupedBarChart(trendDataPair!!)
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                                Text("Not enough data for trend analysis.")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupedBarChart(trendDataPair: Pair<BarData, List<String>>) {
    val (barData, labels) = trendDataPair

    AndroidView(
        factory = { context ->
            // FACTORY: For one-time, data-independent setup
            BarChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = true
                setDrawGridBackground(false)

                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.granularity = 1f // Essential for labels to align with bars

                axisLeft.axisMinimum = 0f
                axisLeft.setDrawGridLines(true)

                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            // UPDATE: For applying data and data-dependent properties

            // 1. Define the widths and spacing for the grouped bars
            val barWidth = 0.25f
            val barSpace = 0.05f
            val groupSpace = 0.4f
            barData.barWidth = barWidth

            // 2. Apply the data to the chart
            chart.data = barData

            // 3. Set the labels for the X-Axis
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)

            // 4. Set the visible range of the x-axis
            // This is crucial for groupBars to work correctly
            chart.xAxis.axisMinimum = 0f
            chart.xAxis.axisMaximum = labels.size.toFloat()

            // 5. Center the labels under the groups
            chart.xAxis.setCenterAxisLabels(true)

            // 6. Group the bars. The 'fromX' (first param) should be the starting point.
            chart.groupBars(0f, groupSpace, barSpace)

            // 7. Refresh the chart to apply all changes
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
    )
}


// ... The rest of your file is unchanged, but included below for completeness ...
@Composable
fun ChartLegend(pieData: PieData?) {
    // Safely get the dataset from the PieData object.
    val dataSet = pieData?.dataSet as? PieDataSet ?: return

    // Use a classic for loop for maximum compatibility with the Java library.
    // This explicitly gets each entry and its corresponding color by index.
    Column {
        for (i in 0 until dataSet.entryCount) {
            val entry = dataSet.getEntryForIndex(i)
            val color = dataSet.getColor(i)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(color)) // Convert the Android integer color to a Compose Color
                )
                Spacer(modifier = Modifier.width(8.dp))
                // The 'label' property of PieEntry holds the category name.
                Text(text = "${entry.label} - ₹${"%.2f".format(entry.value)}")
            }
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
                        color = MaterialTheme.colorScheme.primary
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
fun BudgetWatchCard(budgetStatus: List<BudgetWithSpending>, viewModel: BudgetViewModel) {
    Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Budget Watch", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (budgetStatus.isEmpty()) {
                Text("No budgets set for this month.", style = MaterialTheme.typography.bodyMedium)
            } else {
                budgetStatus.forEach { budgetWithSpendingItem ->
                    BudgetItem(budget = budgetWithSpendingItem.budget, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun BudgetStatusItem(item: BudgetWithSpending) {
    val progress = if (item.budget.amount > 0) (item.spent / item.budget.amount).toFloat() else 0f
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row {
            Text(item.budget.categoryName, modifier = Modifier.weight(1f))
            Text("₹${"%.2f".format(item.spent)} / ₹${"%.2f".format(item.budget.amount)}")
        }
        // CORRECTED: The 'progress' parameter for LinearProgressIndicator is a lambda
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            color = if (progress > 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun RecentActivityCard(transactions: List<TransactionDetails>, navController: NavController) {
    Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Transactions", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { navController.navigate("transaction_list") }) {
                    Text("View All")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            transactions.forEach { details ->
                TransactionItem(transactionDetails = details, onClick = {
                    navController.navigate("edit_transaction/${details.transaction.id}")
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(navController: NavController, viewModel: TransactionViewModel) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var transactionType by remember { mutableStateOf("expense") }
    val transactionTypes = listOf("Expense", "Income")

    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var isAccountDropdownExpanded by remember { mutableStateOf(false) }

    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val selectedDateTime by remember { mutableStateOf(Calendar.getInstance()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val validationError by viewModel.validationError.collectAsState()

    LaunchedEffect(validationError) {
        validationError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Add New Transaction") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                TabRow(selectedTabIndex = if (transactionType == "expense") 0 else 1) {
                    transactionTypes.forEachIndexed { index, title ->
                        Tab(
                            selected = (if (transactionType == "expense") 0 else 1) == index,
                            onClick = { transactionType = if (index == 0) "expense" else "income" },
                            text = { Text(title) }
                        )
                    }
                }
            }

            item { OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }
            item { OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (Optional)") }, modifier = Modifier.fillMaxWidth()) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Select Date")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDateTime.time))
                    }
                    Button(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Default.AccessTime, contentDescription = "Select Time")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(selectedDateTime.time))
                    }
                }
            }

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

            item {
                Button(
                    onClick = {
                        val success = viewModel.addTransaction(
                            description = description,
                            categoryId = selectedCategory?.id,
                            amountStr = amount,
                            accountId = selectedAccount!!.id,
                            notes = notes.takeIf { it.isNotBlank() },
                            date = selectedDateTime.timeInMillis,
                            transactionType = transactionType
                        )
                        if (success) {
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedAccount != null && amount.isNotBlank() && description.isNotBlank()
                ) {
                    Text("Save Transaction")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateTime.timeInMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            val newCalendar = Calendar.getInstance().apply { timeInMillis = it }
                            selectedDateTime.set(Calendar.YEAR, newCalendar.get(Calendar.YEAR))
                            selectedDateTime.set(Calendar.MONTH, newCalendar.get(Calendar.MONTH))
                            selectedDateTime.set(Calendar.DAY_OF_MONTH, newCalendar.get(Calendar.DAY_OF_MONTH))
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = selectedDateTime.get(Calendar.HOUR_OF_DAY), initialMinute = selectedDateTime.get(Calendar.MINUTE))
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onConfirm = {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                selectedDateTime.set(Calendar.MINUTE, timePickerState.minute)
                showTimePicker = false
            }
        ) { TimePicker(state = timePickerState) }
    }
}

// --- EditTransactionScreen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(navController: NavController, viewModel: TransactionViewModel, transactionId: Int) {
    val transaction by viewModel.getTransactionById(transactionId).collectAsState(initial = null)

    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var transactionType by remember { mutableStateOf("expense") }
    val transactionTypes = listOf("Expense", "Income")

    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var isAccountDropdownExpanded by remember { mutableStateOf(false) }

    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    // --- CORRECTED: Typo fixed from 'mutableState of' to 'mutableStateOf' ---
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val selectedDateTime = remember { Calendar.getInstance() }

    val snackbarHostState = remember { SnackbarHostState() }
    val validationError by viewModel.validationError.collectAsState()

    LaunchedEffect(validationError) {
        validationError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(transaction, accounts, categories) {
        transaction?.let { txn ->
            description = txn.description
            amount = txn.amount.toString()
            notes = txn.notes ?: ""
            selectedDateTime.timeInMillis = txn.date
            selectedAccount = accounts.find { it.id == txn.accountId }
            selectedCategory = categories.find { it.id == txn.categoryId }
            transactionType = txn.transactionType
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit Transaction") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Transaction")
                    }
                }
            )
        }
    ) { innerPadding ->
        transaction?.let { currentTransaction ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    TabRow(selectedTabIndex = if (transactionType == "expense") 0 else 1) {
                        transactionTypes.forEachIndexed { index, title ->
                            Tab(
                                selected = (if (transactionType == "expense") 0 else 1) == index,
                                onClick = { transactionType = if (index == 0) "expense" else "income" },
                                text = { Text(title) }
                            )
                        }
                    }
                }

                item { OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }
                item { OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (Optional)") }, modifier = Modifier.fillMaxWidth()) }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = "Select Date")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDateTime.time))
                        }
                        Button(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                            Icon(imageVector = Icons.Default.AccessTime, contentDescription = "Select Time")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(selectedDateTime.time))
                        }
                    }
                }

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

                item {
                    Button(
                        onClick = {
                            val updatedAmount = amount.toDoubleOrNull() ?: 0.0
                            val updatedTransaction = currentTransaction.copy(
                                description = description,
                                amount = updatedAmount,
                                accountId = selectedAccount?.id ?: currentTransaction.accountId,
                                categoryId = selectedCategory?.id,
                                notes = notes.takeIf { it.isNotBlank() },
                                date = selectedDateTime.timeInMillis,
                                transactionType = transactionType
                            )
                            val success = viewModel.updateTransaction(updatedTransaction)
                            if (success) {
                                navController.popBackStack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Update Transaction")
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateTime.timeInMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            val newCalendar = Calendar.getInstance().apply { timeInMillis = it }
                            selectedDateTime.set(Calendar.YEAR, newCalendar.get(Calendar.YEAR))
                            selectedDateTime.set(Calendar.MONTH, newCalendar.get(Calendar.MONTH))
                            selectedDateTime.set(Calendar.DAY_OF_MONTH, newCalendar.get(Calendar.DAY_OF_MONTH))
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = selectedDateTime.get(Calendar.HOUR_OF_DAY), initialMinute = selectedDateTime.get(Calendar.MINUTE))
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onConfirm = {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                selectedDateTime.set(Calendar.MINUTE, timePickerState.minute)
                showTimePicker = false
            }
        ) { TimePicker(state = timePickerState) }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to permanently delete this transaction?") },
            confirmButton = {
                Button(onClick = {
                    transaction?.let {
                        viewModel.deleteTransaction(it)
                        showDeleteDialog = false
                        navController.popBackStack()
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun TransactionList(transactions: List<TransactionDetails>, navController: NavController) {
    if (transactions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No transactions yet. Add one!")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            items(transactions) { details ->
                TransactionItem(transactionDetails = details, onClick = {
                    navController.navigate("edit_transaction/${details.transaction.id}")
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionItem(transactionDetails: TransactionDetails, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transactionDetails.transaction.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                if (!transactionDetails.transaction.notes.isNullOrBlank()) {
                    Text(
                        text = transactionDetails.transaction.notes!!,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${transactionDetails.categoryName ?: "Uncategorized"} • ${transactionDetails.accountName ?: "Unknown"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = SimpleDateFormat("dd MMM yy, h:mm a", Locale.getDefault()).format(Date(transactionDetails.transaction.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "₹${"%.2f".format(transactionDetails.transaction.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (transactionDetails.transaction.transactionType == "expense") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(navController: NavController, viewModel: AccountViewModel) {
    // CORRECTED: Changed 'allAccounts' to 'accountsWithBalance'. This assumes your
    // AccountViewModel has a property like: val accountsWithBalance: Flow<List<AccountWithBalance>>
    val accounts by viewModel.accountsWithBalance.collectAsState(initial = emptyList())
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
        LazyColumn(contentPadding = innerPadding) {
            items(accounts) { account ->
                ListItem(
                    headlineContent = { Text(account.account.name) },
                    supportingContent = { Text("Balance: ₹${"%.2f".format(account.balance)}") },
                    trailingContent = {
                        IconButton(onClick = { navController.navigate("edit_account/${account.account.id}") }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Account")
                        }
                    },
                    modifier = Modifier.clickable { navController.navigate("account_detail/${account.account.id}") }
                )
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
    // --- CORRECTED: This now correctly collects the list of TransactionDetails ---
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
                    // --- CORRECTED: We now pass `details.transaction` to the item composable ---
                    items(transactions) { details ->
                        AccountTransactionItem(transaction = details.transaction)
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "₹${"%.2f".format(transaction.amount)}",
            style = MaterialTheme.typography.bodyLarge,
            color = if (transaction.transactionType == "expense") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}


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
                },
                actions = {
                    IconButton(onClick = { navController.navigate("dashboard") }) {
                        Icon(imageVector = Icons.Filled.Home, contentDescription = "Dashboard")
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(budgets) { budget ->
                    BudgetItem(budget = budget, viewModel = viewModel)
                }
            }
        }
    }
}


@Composable
fun BudgetItem(budget: Budget, viewModel: BudgetViewModel) {
    val spendingFlow = remember(budget.categoryName) {
        viewModel.getActualSpending(budget.categoryName).map { spending ->
            Math.abs(spending ?: 0.0)
        }
    }
    val actualSpending by spendingFlow.collectAsState(initial = 0.0)

    val progress = if (budget.amount > 0) (actualSpending / budget.amount).toFloat() else 0f
    val amountRemaining = budget.amount - actualSpending

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
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = progressColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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

// --- Category Screens ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(navController: NavController, viewModel: CategoryViewModel) {
    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
    var newCategoryName by remember { mutableStateOf("") }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = viewModel.uiEvent) {
        viewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories") },
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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("New Category Name") },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addCategory(newCategoryName)
                            newCategoryName = ""
                        }
                    },
                    enabled = newCategoryName.isNotBlank()
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()

            LazyColumn {
                items(categories) { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = category.name, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            selectedCategory = category
                            showEditDialog = true
                        }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Category")
                        }
                        IconButton(onClick = {
                            selectedCategory = category
                            showDeleteDialog = true
                        }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Category", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    Divider()
                }
            }
        }
    }

    if (showEditDialog) {
        selectedCategory?.let {
            EditCategoryDialog(
                category = it,
                onDismiss = { showEditDialog = false },
                onConfirm = { updatedCategory ->
                    viewModel.updateCategory(updatedCategory)
                    showEditDialog = false
                }
            )
        }
    }

    if (showDeleteDialog) {
        selectedCategory?.let {
            DeleteCategoryDialog(
                category = it,
                onDismiss = { showDeleteDialog = false },
                onConfirm = {
                    viewModel.deleteCategory(it)
                    showDeleteDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategoryDialog(
    category: Category,
    onDismiss: () -> Unit,
    onConfirm: (Category) -> Unit
) {
    var updatedName by remember { mutableStateOf(category.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Category") },
        text = {
            OutlinedTextField(
                value = updatedName,
                onValueChange = { updatedName = it },
                label = { Text("Category Name") }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (updatedName.isNotBlank()) {
                        onConfirm(category.copy(name = updatedName))
                    }
                },
                enabled = updatedName.isNotBlank()
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteCategoryDialog(
    category: Category,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Category") },
        text = { Text("Are you sure you want to delete the category '${category.name}'? This cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryScreen(navController: NavController, viewModel: CategoryViewModel) {
    var categoryName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Category") },
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
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (categoryName.isNotBlank()) {
                        viewModel.addCategory(categoryName)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.align(Alignment.End),
                enabled = categoryName.isNotBlank()
            ) {
                Text("Save Category")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategoryScreen(
    navController: NavController,
    viewModel: CategoryViewModel,
    categoryId: Int
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var categoryName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(key1 = categoryId) {
        val category = viewModel.getCategoryById(categoryId)
        if (category != null) {
            categoryName = category.name
        }
    }

    LaunchedEffect(key1 = viewModel.uiEvent) {
        viewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit Category") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        categoryToDelete = Category(id = categoryId, name = categoryName)
                        showDeleteDialog = true
                    }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Category")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text("Category Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (categoryName.isNotBlank()) {
                        viewModel.updateCategory(Category(id = categoryId, name = categoryName))
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.align(Alignment.End),
                enabled = categoryName.isNotBlank()
            ) {
                Text("Update Category")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this category? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        categoryToDelete?.let {
                            viewModel.deleteCategory(it)
                        }
                        showDeleteDialog = false
                        navController.popBackStack()
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
fun TimePickerDialog(
    title: String = "Select Time",
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                content()
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}