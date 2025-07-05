// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/DashboardScreen.kt
// REASON: REFACTOR - The dashboard's `LazyColumn` has been completely refactored
// to be dynamic. Instead of hardcoded `item` blocks, it now uses `items` to
// iterate over the `visibleCards` list from the ViewModel. A `when` statement
// renders the appropriate card composable for each type, enabling a fully
// customizable and orderable dashboard layout.
// =================================================================================
package io.pm.finlight.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import coil.compose.AsyncImage
import io.pm.finlight.BottomNavItem
import io.pm.finlight.BudgetViewModel
import io.pm.finlight.DashboardCardType
import io.pm.finlight.DashboardViewModel
import io.pm.finlight.DashboardViewModelFactory
import io.pm.finlight.R
import io.pm.finlight.ui.components.AccountSummaryCard
import io.pm.finlight.ui.components.BudgetWatchCard
import io.pm.finlight.ui.components.NetWorthCard
import io.pm.finlight.ui.components.OverallBudgetCard
import io.pm.finlight.ui.components.RecentActivityCard
import io.pm.finlight.ui.components.StatCard

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModelFactory(LocalContext.current.applicationContext as Application)),
    budgetViewModel: BudgetViewModel,
) {
    val netWorth by viewModel.netWorth.collectAsState()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlyExpenses by viewModel.monthlyExpenses.collectAsState()
    val overallBudget by viewModel.overallMonthlyBudget.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val accountsSummary by viewModel.accountsSummary.collectAsState()
    val safeToSpendPerDay by viewModel.safeToSpendPerDay.collectAsState()
    val budgetStatus by viewModel.budgetStatus.collectAsState()
    val visibleCards by viewModel.visibleCards.collectAsState()

    // --- REFACTORED: Use items to dynamically render the dashboard ---
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(visibleCards, key = { it.name }) { cardType ->
            when (cardType) {
                DashboardCardType.OVERALL_BUDGET -> OverallBudgetCard(
                    totalBudget = overallBudget,
                    amountSpent = monthlyExpenses.toFloat(),
                    navController = navController,
                )
                DashboardCardType.MONTHLY_STATS -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label = "Monthly Income",
                        amount = monthlyIncome.toFloat(),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            navController.navigate("income_screen")
                        }
                    )
                    StatCard(
                        label = "Total Budget",
                        amount = overallBudget,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("budget_screen") }
                    )
                    StatCard(
                        label = "Safe To Spend",
                        amount = safeToSpendPerDay,
                        isPerDay = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                DashboardCardType.QUICK_ACTIONS -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilledTonalButton(
                        onClick = {
                            navController.navigate(BottomNavItem.Reports.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Trends")
                    }
                    FilledTonalButton(
                        onClick = {
                            navController.navigate(BottomNavItem.Reports.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Categories")
                    }
                }
                DashboardCardType.NET_WORTH -> NetWorthCard(netWorth)
                DashboardCardType.RECENT_ACTIVITY -> RecentActivityCard(recentTransactions, navController)
                DashboardCardType.ACCOUNTS_SUMMARY -> AccountSummaryCard(accounts = accountsSummary, navController = navController)
                DashboardCardType.BUDGET_WATCH -> BudgetWatchCard(
                    budgetStatus = budgetStatus,
                    viewModel = budgetViewModel,
                    navController = navController,
                )
            }
        }
    }
}
