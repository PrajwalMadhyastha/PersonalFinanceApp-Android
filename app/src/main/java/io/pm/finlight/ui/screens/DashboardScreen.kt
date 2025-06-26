package io.pm.finlight.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.pm.finlight.BudgetViewModel
import io.pm.finlight.DashboardViewModel
import io.pm.finlight.DashboardViewModelFactory
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
    val safeToSpend by viewModel.safeToSpendPerDay.collectAsState()
    val budgetStatus by viewModel.budgetStatus.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val accountsSummary by viewModel.accountsSummary.collectAsState()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            OverallBudgetCard(
                totalBudget = overallBudget,
                amountSpent = monthlyExpenses.toFloat(),
                navController = navController,
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
                    onClick = { navController.navigate("reports_screen") },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Trends")
                }
                FilledTonalButton(
                    onClick = { navController.navigate("reports_screen") },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Categories")
                }
            }
        }
        item { NetWorthCard(netWorth) }
        item { RecentActivityCard(recentTransactions, navController) }
        item { AccountSummaryCard(accounts = accountsSummary, navController = navController) }
        item {
            BudgetWatchCard(
                budgetStatus = budgetStatus,
                viewModel = budgetViewModel,
                navController = navController,
            )
        }
    }
}
