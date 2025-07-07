// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/ReportsScreen.kt
// REASON: REFACTOR - The screen has been updated to fully align with the Project
// Aurora aesthetic. All cards now use the GlassPanel component, and chart
// text colors have been explicitly set from the MaterialTheme to ensure
// high-contrast legibility in dark mode, creating a cohesive and polished look.
// =================================================================================
package io.pm.finlight.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.PieChart
import io.pm.finlight.ReportsViewModel
import io.pm.finlight.TimePeriod
import io.pm.finlight.ui.components.ChartLegend
import io.pm.finlight.ui.components.GlassPanel
import io.pm.finlight.ui.components.GroupedBarChart

@Composable
fun ReportsScreen(
    navController: NavController,
    viewModel: ReportsViewModel = viewModel(),
) {
    val pieData by viewModel.spendingByCategoryPieData.collectAsState(initial = null)
    val trendDataPair by viewModel.monthlyTrendData.collectAsState(initial = null)
    val pieChartLabelColor = MaterialTheme.colorScheme.onSurface.toArgb()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Spending Reports",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            GlassReportNavigationCard(
                title = "Daily Report",
                subtitle = "View a breakdown of any day's spending.",
                icon = Icons.Default.CalendarViewDay,
                onClick = { navController.navigate("time_period_report_screen/${TimePeriod.DAILY}") }
            )
        }
        item {
            GlassReportNavigationCard(
                title = "Weekly Report",
                subtitle = "Analyze your spending week by week.",
                icon = Icons.Default.CalendarViewWeek,
                onClick = { navController.navigate("time_period_report_screen/${TimePeriod.WEEKLY}") }
            )
        }
        item {
            GlassReportNavigationCard(
                title = "Monthly Report",
                subtitle = "Get a high-level overview of your monthly habits.",
                icon = Icons.Default.CalendarViewMonth,
                onClick = { navController.navigate("time_period_report_screen/${TimePeriod.MONTHLY}") }
            )
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "Analysis",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Spending by Category for ${viewModel.monthYear}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (pieData == null || pieData?.entryCount == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No expense data for this month.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        AndroidView(
                            factory = { context ->
                                PieChart(context).apply {
                                    description.isEnabled = false
                                    isDrawHoleEnabled = true
                                    setHoleColor(android.graphics.Color.TRANSPARENT)
                                    setEntryLabelColor(pieChartLabelColor)
                                    setEntryLabelTextSize(12f)
                                    legend.isEnabled = false
                                }
                            },
                            update = { chart ->
                                chart.data = pieData
                                chart.invalidate()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        ChartLegend(pieData)
                    }
                }
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Income vs. Expense Trend",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (trendDataPair != null && trendDataPair!!.first.entryCount > 0) {
                        GroupedBarChart(trendDataPair!!)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Not enough data for trend analysis.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassReportNavigationCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
