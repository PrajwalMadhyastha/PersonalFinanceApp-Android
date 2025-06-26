package io.pm.finlight

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import io.pm.finlight.ui.screens.*
import io.pm.finlight.ui.theme.PersonalFinanceAppTheme
import java.net.URLDecoder
import com.google.gson.Gson
import io.pm.finlight.*
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsRepository = SettingsRepository(this)
        val hasSeenOnboarding = settingsRepository.hasSeenOnboarding()

        setContent {
            PersonalFinanceAppTheme {
                var showOnboarding by remember { mutableStateOf(!hasSeenOnboarding) }

                if (showOnboarding) {
                    // --- UPDATED LOGIC TO LAUNCH NEW ONBOARDING FLOW ---
                    val onboardingViewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModelFactory(application))
                    OnboardingScreen(
                        viewModel = onboardingViewModel,
                        onOnboardingFinished = {
                            settingsRepository.setHasSeenOnboarding(true)
                            showOnboarding = false
                        }
                    )
                } else {
                    FinanceAppWithLockScreen(isInitiallyLocked = settingsRepository.isAppLockEnabledBlocking())
                }
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
        MainAppScreen()
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
fun MainAppScreen() {
    val navController = rememberNavController()
    val bottomNavItems = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Transactions,
        BottomNavItem.Reports,
        BottomNavItem.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentTitle = screenTitles[currentRoute] ?: "Finance App"

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    val fabRoutes = setOf(
        BottomNavItem.Dashboard.route,
        BottomNavItem.Transactions.route,
        "account_list",
        "budget_screen",
        "recurring_transactions"
    )
    val showFab = currentRoute in fabRoutes

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentTitle) },
                navigationIcon = {
                    if (!showBottomBar) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (currentRoute == BottomNavItem.Dashboard.route) {
                        IconButton(onClick = { navController.navigate("search_screen") }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentRoute == screen.route,
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
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(onClick = {
                    when (currentRoute) {
                        BottomNavItem.Dashboard.route, BottomNavItem.Transactions.route -> {
                            navController.navigate("add_transaction")
                        }
                        "account_list" -> {
                            navController.navigate("add_account")
                        }
                        "budget_screen" -> {
                            navController.navigate("add_budget")
                        }
                        "recurring_transactions" -> {
                            navController.navigate("add_recurring_transaction")
                        }
                    }
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }
            }
        }
    ) { innerPadding ->
        AppNavHost(navController = navController, modifier = Modifier.padding(innerPadding))
    }
}


@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    val settingsViewModel: SettingsViewModel = viewModel()
    val transactionViewModel: TransactionViewModel = viewModel()
    val accountViewModel: AccountViewModel = viewModel()
    val categoryViewModel: CategoryViewModel = viewModel()
    val budgetViewModel: BudgetViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Dashboard.route,
        modifier = modifier
    ) {
        composable(BottomNavItem.Dashboard.route) {
            val context = LocalContext.current.applicationContext as Application
            val dashboardViewModel: DashboardViewModel = viewModel(factory = DashboardViewModelFactory(context))
            DashboardScreen(navController, dashboardViewModel, budgetViewModel)
        }
        composable(BottomNavItem.Transactions.route) { TransactionListScreen(navController, transactionViewModel) }
        composable(BottomNavItem.Reports.route) { ReportsScreen(navController, viewModel()) }
        composable(BottomNavItem.Profile.route) { ProfileScreen(navController) }
        composable("settings_screen") { SettingsScreen(navController, settingsViewModel) }
        composable("csv_validation_screen") { CsvValidationScreen(navController, settingsViewModel) }

        composable("search_screen") { SearchScreen(navController) }
        composable(
            route = "review_sms_screen",
            deepLinks = listOf(navDeepLink { uriPattern = "app://finlight.pm.com/review_sms" })
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
            deepLinks = listOf(navDeepLink { uriPattern = "app://finlight.pm.com/approve?amount={amount}&type={type}&merchant={merchant}&smsId={smsId}&smsSender={smsSender}" })
        ) { backStackEntry ->
            val arguments = requireNotNull(backStackEntry.arguments)
            ApproveTransactionScreen(
                navController = navController,
                transactionViewModel = transactionViewModel,
                settingsViewModel = settingsViewModel,
                amount = arguments.getFloat("amount"),
                transactionType = arguments.getString("type") ?: "expense",
                merchant = URLDecoder.decode(arguments.getString("merchant") ?: "Unknown", "UTF-8"),
                smsId = arguments.getLong("smsId"),
                smsSender = arguments.getString("smsSender") ?: ""
            )
        }
        composable("add_transaction") { AddTransactionScreen(navController, transactionViewModel) }
        composable(
            route = "edit_transaction/{transactionId}?isFromCsv={isFromCsv}&lineNumber={lineNumber}&rowDataJson={rowDataJson}",
            arguments = listOf(
                navArgument("transactionId") { type = NavType.IntType; defaultValue = -1 },
                navArgument("isFromCsv") { type = NavType.BoolType; defaultValue = false },
                navArgument("lineNumber") { type = NavType.IntType; defaultValue = -1 },
                navArgument("rowDataJson") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val arguments = requireNotNull(backStackEntry.arguments)
            EditTransactionScreen(
                navController = navController,
                viewModel = transactionViewModel,
                transactionId = arguments.getInt("transactionId"),
                isFromCsvImport = arguments.getBoolean("isFromCsv"),
                csvLineNumber = arguments.getInt("lineNumber"),
                initialCsvData = arguments.getString("rowDataJson")?.let { URLDecoder.decode(it, "UTF-8") }
            )
        }
        composable("account_list") { AccountListScreen(navController, accountViewModel) }
        composable("add_account") { AddAccountScreen(navController, accountViewModel) }
        composable("edit_account/{accountId}", arguments = listOf(navArgument("accountId") { type = NavType.IntType })) { backStackEntry ->
            EditAccountScreen(navController, accountViewModel, backStackEntry.arguments!!.getInt("accountId"))
        }
        composable("account_detail/{accountId}", arguments = listOf(navArgument("accountId") { type = NavType.IntType })) { backStackEntry ->
            AccountDetailScreen(navController, accountViewModel, backStackEntry.arguments!!.getInt("accountId"))
        }
        composable("budget_screen") { BudgetScreen(navController, budgetViewModel) }
        composable("add_budget") { AddEditBudgetScreen(navController, budgetViewModel, null) }
        composable(
            "edit_budget/{budgetId}",
            arguments = listOf(navArgument("budgetId") { type = NavType.IntType })
        ) { backStackEntry ->
            AddEditBudgetScreen(navController, budgetViewModel, backStackEntry.arguments?.getInt("budgetId"))
        }
        composable("category_list") { CategoryListScreen(navController, categoryViewModel) }
        composable("recurring_transactions") { RecurringTransactionScreen(navController) }
        composable("add_recurring_transaction") { AddRecurringTransactionScreen(navController) }
    }
}
