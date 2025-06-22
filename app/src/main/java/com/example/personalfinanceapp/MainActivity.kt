package com.example.personalfinanceapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens.AccountListScreen
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens.AddTransactionScreen
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens.DashboardScreen
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens.EditAccountScreen
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens.EditTransactionScreen
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens.ReportsScreen
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens.ReviewSmsScreen
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens.SettingsScreen
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens.SmsDebugScreen
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens.TransactionListScreen
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens.AddAccountScreen
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens.AccountDetailScreen
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens.BudgetScreen
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens.AddBudgetScreen
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens.ApproveTransactionScreen
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens.CategoryListScreen
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens.EditCategoryScreen


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
