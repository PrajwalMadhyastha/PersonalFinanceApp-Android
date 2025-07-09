// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/MainActivity.kt
// REASON: FIX - The "dashboard" route has been removed from the `fabRoutes` set.
// This prevents the main Floating Action Button from appearing on the
// Dashboard screen, as it is being replaced by a contextual "+ Add" button
// inside the "Recent Activity" card.
// FEATURE - A new "+" icon button has been added to the TopAppBar's actions.
// This button is only visible on the "Transactions" screen and provides a
// contextual way to add a new transaction, replacing the FAB on that screen.
// BUG FIX: Added a `deepLinks` parameter to the `time_period_report_screen`
// navigation destination. This maps the URI pattern from the report
// notifications (e.g., "app://finlight.pm.io/report/DAILY") to the correct
// screen, resolving the crash when tapping on a report notification.
// =================================================================================
package io.pm.finlight

import android.Manifest
import android.app.Activity
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import coil.compose.AsyncImage
import com.google.gson.Gson
import io.pm.finlight.ui.components.AuroraAnimatedBackground
import io.pm.finlight.ui.components.DaybreakAnimatedBackground
import io.pm.finlight.ui.screens.*
import io.pm.finlight.ui.theme.AppTheme
import io.pm.finlight.ui.theme.PersonalFinanceAppTheme
import java.net.URLDecoder
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsRepository = SettingsRepository(this)
        val hasSeenOnboarding = settingsRepository.hasSeenOnboarding()

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val selectedTheme by settingsViewModel.selectedTheme.collectAsState()

            PersonalFinanceAppTheme(selectedTheme = selectedTheme) {
                var showOnboarding by remember { mutableStateOf(!hasSeenOnboarding) }

                if (showOnboarding) {
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

    val dashboardViewModel: DashboardViewModel = viewModel(factory = DashboardViewModelFactory(LocalContext.current.applicationContext as Application))
    val settingsViewModel: SettingsViewModel = viewModel()
    val transactionViewModel: TransactionViewModel = viewModel()
    val accountViewModel: AccountViewModel = viewModel()
    val categoryViewModel: CategoryViewModel = viewModel()
    val budgetViewModel: BudgetViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val incomeViewModel: IncomeViewModel = viewModel()

    val userName by dashboardViewModel.userName.collectAsState()
    val profilePictureUri by dashboardViewModel.profilePictureUri.collectAsState()
    val filterState by transactionViewModel.filterState.collectAsState()
    val isCustomizationMode by dashboardViewModel.isCustomizationMode.collectAsState()
    val selectedTheme by settingsViewModel.selectedTheme.collectAsState()


    val bottomNavItems = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Transactions,
        BottomNavItem.Reports
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val baseCurrentRoute = currentRoute?.split("?")?.firstOrNull()?.split("/")?.firstOrNull()

    val showBottomBar = bottomNavItems.any { it.route == baseCurrentRoute }

    val showMainTopBar = baseCurrentRoute !in setOf(
        "transaction_detail",
        "income_screen",
        "splash_screen",
        "add_transaction",
        "time_period_report_screen"
    )

    val currentTitle = if (showBottomBar) {
        if (isCustomizationMode) "Customize Dashboard" else "Hi, $userName!"
    } else {
        screenTitles[currentRoute] ?: screenTitles[baseCurrentRoute] ?: "Finance App"
    }
    val showProfileIcon = showBottomBar && !isCustomizationMode

    val fabRoutes = setOf(
        "account_list",
        "recurring_transactions"
    )
    val showFab = baseCurrentRoute in fabRoutes && !isCustomizationMode

    val activity = LocalContext.current as AppCompatActivity

    Box(modifier = Modifier.fillMaxSize()) {
        when (selectedTheme) {
            AppTheme.AURORA -> {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
                AuroraAnimatedBackground()
            }
            AppTheme.DAYBREAK -> {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
                DaybreakAnimatedBackground()
            }
            else -> {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {}
            }
        }

        Scaffold(
            topBar = {
                if (showMainTopBar) {
                    TopAppBar(
                        title = { Text(currentTitle) },
                        navigationIcon = {
                            if (showProfileIcon) {
                                AsyncImage(
                                    model = profilePictureUri,
                                    contentDescription = "User Profile Picture",
                                    placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                                    error = painterResource(id = R.drawable.ic_launcher_foreground),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .padding(start = 16.dp)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { navController.navigate("profile") }
                                )
                            } else if (!showBottomBar) {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        },
                        actions = {
                            if (isCustomizationMode) {
                                IconButton(onClick = { dashboardViewModel.onAddCardClick() }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Card")
                                }
                                TextButton(onClick = { dashboardViewModel.exitCustomizationModeAndSave() }) {
                                    Text("Done")
                                }
                            } else {
                                when (baseCurrentRoute) {
                                    BottomNavItem.Dashboard.route -> {
                                        IconButton(onClick = { navController.navigate("search_screen") }) {
                                            Icon(Icons.Default.Search, contentDescription = "Search")
                                        }
                                    }
                                    BottomNavItem.Transactions.route -> {
                                        val areFiltersActive by remember(filterState) {
                                            derivedStateOf {
                                                filterState.keyword.isNotBlank() || filterState.account != null || filterState.category != null
                                            }
                                        }
                                        // --- NEW: Add Transaction Icon Button ---
                                        IconButton(onClick = { navController.navigate("add_transaction") }) {
                                            Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                                        }
                                        BadgedBox(
                                            badge = {
                                                if (areFiltersActive) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.primary)
                                                    )
                                                }
                                            }
                                        ) {
                                            IconButton(onClick = { transactionViewModel.onFilterClick() }) {
                                                Icon(Icons.Default.FilterList, contentDescription = "Filter Transactions")
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            },
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = Color.Transparent
                    ) {
                        bottomNavItems.forEach { screen ->
                            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.label) },
                                label = { Text(screen.label) },
                                selected = isSelected,
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
                        when (baseCurrentRoute) {
                            "account_list" -> {
                                navController.navigate("add_account")
                            }
                            "recurring_transactions" -> {
                                navController.navigate("add_recurring_transaction")
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Transaction or Account")
                    }
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            AppNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
                activity = activity,
                dashboardViewModel = dashboardViewModel,
                settingsViewModel = settingsViewModel,
                transactionViewModel = transactionViewModel,
                accountViewModel = accountViewModel,
                categoryViewModel = categoryViewModel,
                budgetViewModel = budgetViewModel,
                profileViewModel = profileViewModel,
                incomeViewModel = incomeViewModel
            )
        }
    }
}


@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    activity: AppCompatActivity,
    dashboardViewModel: DashboardViewModel,
    settingsViewModel: SettingsViewModel,
    transactionViewModel: TransactionViewModel,
    accountViewModel: AccountViewModel,
    categoryViewModel: CategoryViewModel,
    budgetViewModel: BudgetViewModel,
    profileViewModel: ProfileViewModel,
    incomeViewModel: IncomeViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "splash_screen",
        modifier = modifier
    ) {
        composable("splash_screen") {
            SplashScreen(navController = navController, activity = activity)
        }

        composable("manage_parse_rules") {
            ManageParseRulesScreen(navController)
        }
        composable("manage_ignore_rules") {
            ManageIgnoreRulesScreen()
        }

        composable(BottomNavItem.Dashboard.route) {
            DashboardScreen(navController, dashboardViewModel)
        }
        composable(
            route = BottomNavItem.Transactions.route
        ) {
            TransactionListScreen(
                navController = navController,
                viewModel = transactionViewModel
            )
        }
        composable(
            route = BottomNavItem.Reports.route,
            deepLinks = listOf(navDeepLink { uriPattern = "app://finlight.pm.io/reports" })
        ) { ReportsScreen(navController, viewModel()) }
        composable("profile") {
            ProfileScreen(
                navController = navController,
                profileViewModel = profileViewModel,
                settingsViewModel = settingsViewModel
            )
        }
        composable("edit_profile") { EditProfileScreen(navController, profileViewModel) }
        composable("csv_validation_screen") { CsvValidationScreen(navController, settingsViewModel) }
        composable("search_screen") { SearchScreen(navController) }
        composable(
            route = "review_sms_screen",
            deepLinks = listOf(navDeepLink { uriPattern = "app://finlight.pm.io/review_sms" })
        ) { ReviewSmsScreen(navController, settingsViewModel) }

        composable("income_screen") {
            IncomeScreen(navController, incomeViewModel)
        }

        composable(
            route = "approve_transaction_screen?potentialTxnJson={potentialTxnJson}",
            arguments = listOf(
                navArgument("potentialTxnJson") { type = NavType.StringType }
            ),
            deepLinks = listOf(navDeepLink { uriPattern = "app://finlight.pm.io/approve_sms?potentialTxnJson={potentialTxnJson}" })
        ) { backStackEntry ->
            val json = backStackEntry.arguments?.getString("potentialTxnJson")
            val potentialTxn = Gson().fromJson(URLDecoder.decode(json, "UTF-8"), PotentialTransaction::class.java)

            ApproveTransactionScreen(
                navController = navController,
                transactionViewModel = transactionViewModel,
                settingsViewModel = settingsViewModel,
                potentialTxn = potentialTxn
            )
        }

        composable(
            "add_transaction?isCsvEdit={isCsvEdit}&csvLineNumber={csvLineNumber}&initialDataJson={initialDataJson}",
            arguments = listOf(
                navArgument("isCsvEdit") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("csvLineNumber") {
                    type = NavType.IntType
                    defaultValue = -1
                },
                navArgument("initialDataJson") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val arguments = requireNotNull(backStackEntry.arguments)
            val isCsvEdit = arguments.getBoolean("isCsvEdit")
            val csvLineNumber = arguments.getInt("csvLineNumber")
            val initialDataJson = arguments.getString("initialDataJson")

            AddTransactionScreen(
                navController = navController,
                viewModel = transactionViewModel,
                isCsvEdit = isCsvEdit,
                csvLineNumber = csvLineNumber,
                initialDataJson = initialDataJson?.let { URLDecoder.decode(it, "UTF-8") }
            )
        }

        composable(
            route = "transaction_detail/{transactionId}",
            arguments = listOf(navArgument("transactionId") { type = NavType.IntType }),
            deepLinks = listOf(navDeepLink { uriPattern = "app://finlight.pm.io/transaction_detail/{transactionId}" })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments!!.getInt("transactionId")
            TransactionDetailScreen(
                navController = navController,
                transactionId = transactionId,
                viewModel = transactionViewModel,
                accountViewModel = accountViewModel,
                onSaveRenameRule = { original, new ->
                    settingsViewModel.saveMerchantRenameRule(original, new)
                }
            )
        }

        composable("account_list") { AccountListScreen(navController, accountViewModel) }

        composable("add_account") {
            AddEditAccountScreen(navController, accountViewModel, null)
        }
        composable(
            "edit_account/{accountId}",
            arguments = listOf(navArgument("accountId") { type = NavType.IntType })
        ) { backStackEntry ->
            AddEditAccountScreen(
                navController,
                accountViewModel,
                backStackEntry.arguments!!.getInt("accountId")
            )
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
        composable("tag_management") { TagManagementScreen() }
        composable("recurring_transactions") { RecurringTransactionScreen(navController) }
        composable("add_recurring_transaction") { AddRecurringTransactionScreen(navController) }

        composable(
            "rule_creation_screen?potentialTransactionJson={potentialTransactionJson}&ruleId={ruleId}",
            arguments = listOf(
                navArgument("potentialTransactionJson") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("ruleId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val json = backStackEntry.arguments?.getString("potentialTransactionJson")
            val ruleId = backStackEntry.arguments?.getInt("ruleId")
            RuleCreationScreen(
                navController = navController,
                potentialTransactionJson = json?.let { URLDecoder.decode(it, "UTF-8") },
                ruleId = if (ruleId == -1) null else ruleId
            )
        }

        composable(
            "link_transaction_screen/{potentialTransactionJson}",
            arguments = listOf(
                navArgument("potentialTransactionJson") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val json = backStackEntry.arguments?.getString("potentialTransactionJson") ?: ""
            LinkTransactionScreen(
                navController = navController,
                potentialTransactionJson = json
            )
        }

        composable(
            "time_period_report_screen/{timePeriod}",
            arguments = listOf(navArgument("timePeriod") {
                type = NavType.EnumType(TimePeriod::class.java)
            }),
            // --- BUG FIX: Added the deep link mapping ---
            deepLinks = listOf(navDeepLink { uriPattern = "app://finlight.pm.io/report/{timePeriod}" })
        ) { backStackEntry ->
            val timePeriod = backStackEntry.arguments?.getSerializable("timePeriod", TimePeriod::class.java)

            if (timePeriod != null) {
                TimePeriodReportScreen(navController = navController, timePeriod = timePeriod)
            }
        }
    }
}

@Composable
fun SplashScreen(navController: NavHostController, activity: Activity) {
    LaunchedEffect(key1 = Unit) {
        val deepLinkUri = activity.intent?.data
        if (deepLinkUri != null) {
            // BUG FIX: Correctly build the back stack for deep links.
            // First, navigate to the home screen (dashboard) and clear the splash screen.
            navController.navigate(BottomNavItem.Dashboard.route) {
                popUpTo("splash_screen") { inclusive = true }
            }
            // Then, navigate to the actual deep link destination.
            navController.navigate(deepLinkUri)
            activity.intent.data = null // Important: Consume the intent's data
        } else {
            // Normal startup: go to the dashboard.
            navController.navigate(BottomNavItem.Dashboard.route) {
                popUpTo("splash_screen") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}