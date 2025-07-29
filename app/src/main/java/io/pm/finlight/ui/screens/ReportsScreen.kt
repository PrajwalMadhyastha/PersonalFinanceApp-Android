// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/ReportsScreen.kt
// REASON: FIX - The layout weight for the PieChart has been increased from 1.5f
// to 2.5f, allocating more horizontal space to it. The holeRadius has been
// increased to 75f to create a larger, thinner donut ring. These changes allow
// the chart to be significantly larger and more visually prominent, matching the
// desired design.
// =================================================================================
package io.pm.finlight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import io.pm.finlight.CategoryIconHelper
import io.pm.finlight.ReportInsights
import io.pm.finlight.ReportPeriod
import io.pm.finlight.ReportViewType
import io.pm.finlight.ReportsViewModel
import io.pm.finlight.TimePeriod
import io.pm.finlight.ui.components.ConsistencyCalendar
import io.pm.finlight.ui.components.DetailedMonthlyCalendar
import io.pm.finlight.ui.components.GlassPanel
import io.pm.finlight.ui.components.GroupedBarChart
import java.util.Calendar
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavController,
    viewModel: ReportsViewModel = viewModel(),
) {
    val reportData by viewModel.reportData.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val pieChartLabelColor = MaterialTheme.colorScheme.onSurface.toArgb()

    val reportViewType by viewModel.reportViewType.collectAsState()
    val yearlyCalendarData by viewModel.consistencyCalendarData.collectAsState()
    val detailedMonthData by viewModel.detailedMonthData.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val consistencyStats by viewModel.displayedConsistencyStats.collectAsState()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val periods = ReportPeriod.entries
                periods.chunked(2).forEach { rowPeriods ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowPeriods.forEach { period ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = period == selectedPeriod,
                                onClick = { viewModel.selectPeriod(period) },
                                label = { Text(period.displayName) },
                                leadingIcon = if (period == selectedPeriod) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    }
                                } else {
                                    null
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            reportData.insights?.let {
                ReportInsightsCard(insights = it)
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Spending Consistency",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(16.dp))

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = reportViewType == ReportViewType.MONTHLY,
                            onClick = { viewModel.setReportView(ReportViewType.MONTHLY) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Monthly")
                        }
                        SegmentedButton(
                            selected = reportViewType == ReportViewType.YEARLY,
                            onClick = { viewModel.setReportView(ReportViewType.YEARLY) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Yearly")
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (reportViewType) {
                            ReportViewType.YEARLY -> {
                                if (yearlyCalendarData.isEmpty()) {
                                    CircularProgressIndicator()
                                } else {
                                    ConsistencyCalendar(
                                        data = yearlyCalendarData,
                                        onDayClick = { date ->
                                            navController.navigate("search_screen?date=${date.time}&focusSearch=false")
                                        }
                                    )
                                }
                            }
                            ReportViewType.MONTHLY -> {
                                if (detailedMonthData.isEmpty()) {
                                    CircularProgressIndicator()
                                } else {
                                    DetailedMonthlyCalendar(
                                        data = detailedMonthData,
                                        selectedMonth = selectedMonth,
                                        onPreviousMonth = viewModel::selectPreviousMonth,
                                        onNextMonth = viewModel::selectNextMonth,
                                        onDayClick = { date ->
                                            navController.navigate("search_screen?date=${date.time}&focusSearch=false")
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(consistencyStats.noSpendDays, "No Spend")
                        StatItem(consistencyStats.goodDays, "Good Days")
                        StatItem(consistencyStats.badDays, "Over Budget")
                        StatItem(consistencyStats.noDataDays, "No Data")
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
                        "Spending by Category for ${reportData.periodTitle}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(16.dp))
                    val pieData = reportData.pieData
                    if (pieData == null || pieData.entryCount == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No expense data for this period.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AndroidView(
                                // --- UPDATED: Increased weight to give chart more space ---
                                modifier = Modifier.weight(2.5f),
                                factory = { context ->
                                    PieChart(context).apply {
                                        description.isEnabled = false
                                        isDrawHoleEnabled = true
                                        setHoleColor(android.graphics.Color.TRANSPARENT)
                                        // --- UPDATED: Increase hole radius for a larger donut hole ---
                                        holeRadius = 75f
                                        legend.isEnabled = false
                                        setUsePercentValues(true)
                                        setDrawEntryLabels(false)
                                        setTransparentCircleAlpha(0)
                                        setExtraOffsets(0f, 0f, 0f, 0f)
                                        setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                                            override fun onValueSelected(e: Entry?, h: Highlight?) {
                                                val categoryName = e?.data as? String ?: return
                                                val category = allCategories.find { it.name.equals(categoryName, ignoreCase = true) }
                                                category?.let {
                                                    navController.navigate("search_screen?categoryId=${it.id}")
                                                }
                                            }

                                            override fun onNothingSelected() {}
                                        })
                                    }
                                },
                                update = { chart ->
                                    chart.data = pieData
                                    chart.invalidate()
                                }
                            )
                            ChartLegend(
                                modifier = Modifier.weight(1f),
                                pieData = pieData
                            )
                        }
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
                    Spacer(Modifier.height(16.dp))
                    val trendDataPair = reportData.trendData
                    if (trendDataPair != null && trendDataPair.first.entryCount > 0) {
                        GroupedBarChart(
                            chartData = trendDataPair,
                            onBarClick = { entry ->
                                val monthIndex = entry.x.toInt()
                                val trends = viewModel.reportData.value.trendData?.first?.dataSets?.firstOrNull()?.getEntryForIndex(monthIndex)
                                if (trends != null) {
                                    val calendar = Calendar.getInstance()
                                    calendar.add(Calendar.MONTH, monthIndex - (reportData.trendData?.second?.size?.minus(1) ?: 0))
                                    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                                    val dateMillis = calendar.timeInMillis
                                    navController.navigate("time_period_report_screen/${TimePeriod.MONTHLY}?date=$dateMillis")
                                }
                            }
                        )
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

        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "Detailed Reports",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    }
}

@Composable
fun ReportInsightsCard(insights: ReportInsights) {
    GlassPanel {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Change", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val (text, color) = when {
                    insights.percentageChange == null -> "--" to MaterialTheme.colorScheme.onSurface
                    insights.percentageChange > 0 -> "↑ ${insights.percentageChange}%" to MaterialTheme.colorScheme.error
                    insights.percentageChange < 0 -> "↓ ${abs(insights.percentageChange)}%" to MaterialTheme.colorScheme.primary
                    else -> "No Change" to MaterialTheme.colorScheme.onSurface
                }
                Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
                Text("vs. previous period", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Top Spend", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (insights.topCategory != null) {
                    Icon(
                        imageVector = CategoryIconHelper.getIcon(insights.topCategory.iconKey ?: "category"),
                        contentDescription = insights.topCategory.categoryName,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CategoryIconHelper.getIconBackgroundColor(insights.topCategory.colorKey ?: "gray_light"))
                            .padding(8.dp),
                        tint = Color.Black
                    )
                    Text(insights.topCategory.categoryName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("--", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun GlassReportNavigationCard(
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

@Composable
private fun StatItem(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChartLegend(modifier: Modifier = Modifier, pieData: PieData?) {
    val dataSet = pieData?.dataSet as? PieDataSet ?: return
    var sumOfValues = 0f
    for (i in 0 until dataSet.entryCount) {
        sumOfValues += dataSet.getEntryForIndex(i).value
    }
    val totalValue = sumOfValues

    LazyColumn(
        modifier = modifier.padding(start = 16.dp),
    ) {
        items(dataSet.entryCount) { i ->
            val entry = dataSet.getEntryForIndex(i)
            val color = dataSet.getColor(i)
            val percentage = if (totalValue > 0) (entry.value / totalValue * 100) else 0f

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(androidx.compose.ui.graphics.Color(color)),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = entry.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${"%.1f".format(percentage)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
