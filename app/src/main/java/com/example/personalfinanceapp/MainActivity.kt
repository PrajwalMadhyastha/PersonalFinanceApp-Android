package com.example.personalfinanceapp

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens.*
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import java.net.URLDecoder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.Executor

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Dashboard : BottomNavItem("dashboard", Icons.Filled.Home, "Dashboard")
    object Transactions : BottomNavItem("transaction_list", Icons.Filled.Receipt, "History")
    object Reports : BottomNavItem("reports_screen", Icons.Filled.Assessment, "Reports")
    object Settings : BottomNavItem("settings_screen", Icons.Filled.Settings, "Settings")
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsRepository = SettingsRepository(this)
        val initialLockStatus = settingsRepository.isAppLockEnabledBlocking()

        setContent {
            PersonalFinanceAppTheme {
                FinanceAppWithLockScreen(isInitiallyLocked = initialLockStatus)
            }
        }
    }
}

@Composable
fun FinanceAppWithLockScreen(isInitiallyLocked: Boolean) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }

    var isLocked by remember { mutableStateOf(isInitiallyLocked) }
    val appLockEnabled by settingsRepository.getAppLockEnabled().collectAsState(initial = isInitiallyLocked)

    val permissionsToRequest = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.POST_NOTIFICATIONS
    )
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val allPermissionsGranted = perms.all { it.value }
        if (!allPermissionsGranted) {
            Toast.makeText(context, "Some permissions were denied. The app may not function fully.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(key1 = true) {
        val areAllPermissionsGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!areAllPermissionsGranted) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    LaunchedEffect(appLockEnabled) {
        if (!appLockEnabled) {
            isLocked = false
        }
    }

    if (isLocked) {
        LockScreen(onUnlock = { isLocked = false })
    } else {
        FinanceApp()
    }
}


@Composable
fun LockScreen(onUnlock: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalContext.current as FragmentActivity
    val executor: Executor = remember { ContextCompat.getMainExecutor(context) }

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
                    onUnlock()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceApp() {
    val navController = rememberNavController()
    val bottomNavItems = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Transactions,
        BottomNavItem.Reports,
        BottomNavItem.Settings
    )

    // --- CORRECTED: ViewModel is now created once and shared between settings-related screens ---
    val settingsViewModel: SettingsViewModel = viewModel()


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
            composable(BottomNavItem.Dashboard.route) {
                val context = LocalContext.current.applicationContext as Application
                val dashboardViewModel: DashboardViewModel = viewModel(factory = DashboardViewModelFactory(context))
                val budgetViewModel: BudgetViewModel = viewModel()
                DashboardScreen(navController, dashboardViewModel, budgetViewModel)
            }
            composable(BottomNavItem.Transactions.route) { TransactionListScreen(navController, viewModel()) }
            composable(BottomNavItem.Reports.route) { ReportsScreen(navController, viewModel()) }
            // Pass the shared ViewModel instance to both screens
            composable(BottomNavItem.Settings.route) { SettingsScreen(navController, settingsViewModel) }
            composable("csv_validation_screen") { CsvValidationScreen(navController, settingsViewModel) }

            composable("search_screen") { SearchScreen(navController) }
            composable(
                route = "review_sms_screen",
                deepLinks = listOf(navDeepLink { uriPattern = "app://personalfinanceapp.example.com/review_sms" })
            ) { ReviewSmsScreen(navController, settingsViewModel) }
            composable("sms_debug_screen") { SmsDebugScreen(navController, settingsViewModel) }
            composable(
                route = "approve_transaction_screen/{amount}/{type}/{merchant}/{smsId}/{smsSender}",
                arguments = listOf(
                    navArgument("amount") { type = NavType.FloatType },
                    navArgument("type") { type = NavType.StringType },
                    navArgument("merchant") { type = NavType.StringType },
                    navArgument("smsId") { type = NavType.LongType },
                    navArgument("smsSender") { type = NavType.StringType }
                ),
                deepLinks = listOf(navDeepLink { uriPattern = "app://personalfinanceapp.example.com/approve?amount={amount}&type={type}&merchant={merchant}&smsId={smsId}&smsSender={smsSender}" })
            ) { backStackEntry ->
                val arguments = requireNotNull(backStackEntry.arguments)
                ApproveTransactionScreen(
                    navController = navController,
                    amount = arguments.getFloat("amount"),
                    transactionType = arguments.getString("type") ?: "expense",
                    merchant = URLDecoder.decode(arguments.getString("merchant") ?: "Unknown", "UTF-8"),
                    smsId = arguments.getLong("smsId"),
                    smsSender = arguments.getString("smsSender") ?: ""
                )
            }
            composable("add_transaction") { AddTransactionScreen(navController, viewModel()) }
            composable("edit_transaction/{transactionId}", arguments = listOf(navArgument("transactionId") { type = NavType.IntType })) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getInt("transactionId")
                if (transactionId != null) { EditTransactionScreen(navController, viewModel(), transactionId) }
            }
            composable("account_list") { AccountListScreen(navController, viewModel()) }
            composable("add_account") { AddAccountScreen(navController, viewModel()) }
            composable("edit_account/{accountId}", arguments = listOf(navArgument("accountId") { type = NavType.IntType })) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getInt("accountId")
                if (accountId != null) { EditAccountScreen(navController, viewModel(), accountId) }
            }
            composable("account_detail/{accountId}", arguments = listOf(navArgument("accountId") { type = NavType.IntType })) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getInt("accountId")
                if (accountId != null) { AccountDetailScreen(navController, viewModel(), accountId) }
            }
            composable("budget_screen") { BudgetScreen(navController, viewModel()) }
            composable(
                "edit_imported_transaction/{lineNumber}/{rowDataJson}",
                arguments = listOf(
                    navArgument("lineNumber") { type = NavType.IntType },
                    navArgument("rowDataJson") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val lineNumber = backStackEntry.arguments?.getInt("lineNumber") ?: 0
                val rowDataJson = backStackEntry.arguments?.getString("rowDataJson") ?: "[]"

                val gson = Gson()
                val listType = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                val initialData: List<String> = gson.fromJson(rowDataJson, listType)

                EditImportedTransactionScreen(
                    navController = navController,
                    lineNumber = lineNumber,
                    initialData = initialData
                )
            }
            composable("recurring_transactions") { RecurringTransactionScreen(navController) }
            composable("add_recurring_transaction") { AddRecurringTransactionScreen(navController) }
            composable("add_budget") { AddBudgetScreen(navController, viewModel()) }
            composable("category_list") { CategoryListScreen(navController, viewModel()) }
            composable("edit_category/{categoryId}", arguments = listOf(navArgument("categoryId") { type = NavType.IntType })) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getInt("categoryId")
                if (categoryId != null) { EditCategoryScreen(navController, viewModel(), categoryId) }
            }
        }
    }
}
