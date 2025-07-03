// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/ReportsScreen.kt
// REASON: FEATURE - Added new cards for navigating to the generic
// TimePeriodReportScreen with the appropriate TimePeriod enum (DAILY, WEEKLY,
// MONTHLY), enabling the new reporting features.
// =================================================================================
package io.pm.finlight.ui.screens

import android.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.pm.finlight.ReportsViewModel
import io.pm.finlight.TimePeriod
import io.pm.finlight.ui.components.ChartLegend
import io.pm.finlight.ui.components.GroupedBarChart
import com.github.mikephil.charting.charts.PieChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavController,
    viewModel: ReportsViewModel = viewModel(),
) {
    val pieData by viewModel.spendingByCategoryPieData.collectAsState(initial = null)
    val trendDataPair by viewModel.monthlyTrendData.collectAsState(initial = null)


    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Spending Reports", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            ReportNavigationCard(
                title = "Daily Report",
                subtitle = "View a breakdown of any day's spending.",
                icon = Icons.Default.CalendarViewDay,
                onClick = { navController.navigate("time_period_report_screen/${TimePeriod.DAILY}") }
            )
        }
        item {
            ReportNavigationCard(
                title = "Weekly Report",
                subtitle = "Analyze your spending week by week.",
                icon = Icons.Default.CalendarViewWeek,
                onClick = { navController.navigate("time_period_report_screen/${TimePeriod.WEEKLY}") }
            )
        }
        item {
            ReportNavigationCard(
                title = "Monthly Report",
                subtitle = "Get a high-level overview of your monthly habits.",
                icon = Icons.Default.CalendarViewMonth,
                onClick = { navController.navigate("time_period_report_screen/${TimePeriod.MONTHLY}") }
            )
        }

        item {
            Spacer(Modifier.height(16.dp))
            Text("Analysis", style = MaterialTheme.typography.headlineSmall)
        }

        // --- Pie Chart Card ---
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
                                    description.isEnabled = false
                                    isDrawHoleEnabled = true
                                    setHoleColor(
                                        Color.TRANSPARENT,
                                    )
                                    setEntryLabelColor(Color.BLACK)
                                    setEntryLabelTextSize(12f)
                                    legend.isEnabled = false
                                }
                            },
                            update = { chart ->
                                chart.data = pieData
                                chart.notifyDataSetChanged()
                                chart.invalidate()
                            },
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        pieData?.let { ChartLegend(it) }
                    }
                }
            }
        }

        // --- Bar Chart Card ---
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Income vs. Expense Trend", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (trendDataPair != null && trendDataPair!!.first.entryCount > 0) {
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

@Composable
fun ReportNavigationCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(32.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
