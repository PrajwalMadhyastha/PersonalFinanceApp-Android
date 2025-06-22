package com.example.personalfinanceapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.biometric.BiometricPrompt // Add this line


// --- Navigation Destinations ---
sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Dashboard : BottomNavItem("dashboard", Icons.Filled.Home, "Dashboard")
    object Transactions : BottomNavItem("transaction_list", Icons.Filled.Receipt, "History")
    object Reports : BottomNavItem("reports_screen", Icons.Filled.Assessment, "Reports")
    object Settings : BottomNavItem("settings_screen", Icons.Filled.Settings, "Settings")
}

class MainActivity : AppCompatActivity() { // Must be AppCompatActivity or FragmentActivity for Biometrics
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PersonalFinanceAppTheme {
                FinanceAppWithLockScreen()
            }
        }
    }
}

@Composable
fun FinanceAppWithLockScreen() {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }

    // Use a coroutine scope to read the initial value from SharedPreferences
    val scope = rememberCoroutineScope()
    val initialLockState = remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(key1 = Unit) {
        scope.launch {
            // Read the setting once on startup.
            initialLockState.value = settingsRepository.isAppLockEnabledBlocking()
        }
    }

    val isAppLockEnabled by settingsRepository.getAppLockEnabled().collectAsState(initial = initialLockState.value ?: false)
    var isLocked by remember(isAppLockEnabled) { mutableStateOf(isAppLockEnabled) }

    // This effect ensures that if the user toggles the lock off while the app is open, it unlocks.
    LaunchedEffect(isAppLockEnabled) {
        if (!isAppLockEnabled) {
            isLocked = false
        }
    }

    if (initialLockState.value == null) {
        // Show a loading screen while reading initial settings
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (isLocked) {
        LockScreen(
            onUnlock = {
                isLocked = false
            }
        )
    } else {
        FinanceApp()
    }
}


// --- Screen to display while app is locked ---
@Composable
fun LockScreen(onUnlock: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalContext.current as FragmentActivity
    val executor = remember { ContextCompat.getMainExecutor(context) }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("App Locked")
            .setSubtitle("Authenticate to access your finances")
            .setNegativeButtonText("Cancel")
            .build()
    }

    val biometricPrompt = remember {
        BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onUnlock() // This sets isLocked to false
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Don't show toast for user cancellation
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                        Toast.makeText(context, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Automatically trigger the prompt when the LockScreen is shown
    LaunchedEffect(Unit) {
        biometricPrompt.authenticate(promptInfo)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = { biometricPrompt.authenticate(promptInfo) }) {
            Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text("Unlock App")
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
            composable("approve_transaction_screen") {
                ApproveTransactionScreen(
                    navController = navController,
                    settingsViewModel = settingsViewModel,
                    transactionViewModel = transactionViewModel
                )
            }

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
