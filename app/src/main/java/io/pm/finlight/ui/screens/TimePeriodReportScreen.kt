// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/TimePeriodReportScreen.kt
// REASON: BUG FIX - The duplicated ViewModel and Factory code has been removed
// from this file to resolve the "Redeclaration" compilation errors. This screen
// now correctly uses the ViewModel from its dedicated file.
// REASON: REFACTOR - The `ReportHeader` subtitle is now dynamic and reflects
// the rolling time period (e.g., "Since Jul 8, 10:00 PM"). This provides a
// clearer and more accurate description of the data being displayed, matching
// the user's requirement.
// REASON: FEATURE - The UI has been enhanced by splitting the header into a
// "Hero" card and a new "Insights" card. The Hero card now has a more prominent
// design with a background icon, and the Insights card displays the percentage
// change and top spending category for the period.
// REASON: FEATURE - The "Total Spent" amount in the hero card now uses a
// subtle gradient text effect for added visual flair, completing the
// implementation of "Idea 3".
// FEATURE: The hero card has been redesigned to show both "Total Income" and
// "Total Spent" side-by-side, providing a more comprehensive financial overview
// for the selected period.
// =================================================================================
package io.pm.finlight.ui.screens

import android.app.Application
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import io.pm.finlight.*
import io.pm.finlight.ui.components.GlassPanel
import io.pm.finlight.ui.components.TransactionItem
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePeriodReportScreen(
    navController: NavController,
    timePeriod: TimePeriod,
    initialDateMillis: Long? = null // Allow null for default
) {
    val application = LocalContext.current.applicationContext as Application
    val factory = TimePeriodReportViewModelFactory(application, timePeriod, initialDateMillis)
    val viewModel: TimePeriodReportViewModel = viewModel(factory = factory)

    val selectedDate by viewModel.selectedDate.collectAsState()
    val transactions by viewModel.transactionsForPeriod.collectAsState()
    val chartDataPair by viewModel.chartData.collectAsState()
    val insights by viewModel.insights.collectAsState()

    val totalSpent = transactions.filter { it.transaction.transactionType == "expense" && !it.transaction.isExcluded }.sumOf { it.transaction.amount }
    val totalIncome by viewModel.totalIncome.collectAsState()

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (timePeriod == TimePeriod.DAILY) {
            NotificationManagerCompat.from(context).cancel(2) // Daily Report Notification ID
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (timePeriod) {
                            TimePeriod.DAILY -> "Daily Report"
                            TimePeriod.WEEKLY -> "Weekly Report"
                            TimePeriod.MONTHLY -> "Monthly Report"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent // Make scaffold background transparent
    ) { innerPadding ->
        var dragAmount by remember { mutableStateOf(0f) }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { },
                        onDragEnd = {
                            if (dragAmount > 150) { // Swipe Right
                                viewModel.selectPreviousPeriod()
                            } else if (dragAmount < -150) { // Swipe Left
                                viewModel.selectNextPeriod()
                            }
                            dragAmount = 0f
                        },
                        onDragCancel = { dragAmount = 0f }
                    ) { change, horizontalDragAmount ->
                        dragAmount += horizontalDragAmount
                        change.consume()
                    }
                }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ReportHeader(
                        totalSpent = totalSpent,
                        totalIncome = totalIncome,
                        timePeriod = timePeriod,
                        selectedDate = selectedDate.time
                    )
                }

                item {
                    ReportInsightsCard(insights = insights)
                }

                item {
                    GlassPanel {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Spending Chart",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(16.dp))
                            if (chartDataPair != null) {
                                SpendingBarChart(
                                    chartData = chartDataPair!!
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No chart data for this period.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (transactions.isNotEmpty()) {
                    item {
                        Text(
                            "Transactions in this Period",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(transactions, key = { it.transaction.id }) { transaction ->
                        TransactionItem(
                            transactionDetails = transaction,
                            onClick = { navController.navigate("transaction_detail/${transaction.transaction.id}") }
                        )
                    }
                } else {
                    item {
                        GlassPanel {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "No transactions recorded for this period.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportHeader(totalSpent: Double, totalIncome: Double, timePeriod: TimePeriod, selectedDate: Date) {
    val subtitle = when (timePeriod) {
        TimePeriod.DAILY -> {
            val format = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            val startCal = Calendar.getInstance().apply {
                time = selectedDate
                add(Calendar.HOUR_OF_DAY, -24)
            }
            "Since ${format.format(startCal.time)}"
        }
        TimePeriod.WEEKLY -> {
            val format = SimpleDateFormat("MMM d", Locale.getDefault())
            val startCal = Calendar.getInstance().apply {
                time = selectedDate
                add(Calendar.DAY_OF_YEAR, -7)
            }
            "Since ${format.format(startCal.time)}"
        }
        TimePeriod.MONTHLY -> {
            val format = SimpleDateFormat("MMM d", Locale.getDefault())
            val startCal = Calendar.getInstance().apply {
                time = selectedDate
                add(Calendar.DAY_OF_YEAR, -30)
            }
            "Since ${format.format(startCal.time)}"
        }
    }

    val backgroundIcon = when (timePeriod) {
        TimePeriod.DAILY -> Icons.Default.CalendarViewDay
        TimePeriod.WEEKLY -> Icons.Default.CalendarViewWeek
        TimePeriod.MONTHLY -> Icons.Default.CalendarViewMonth
    }

    GlassPanel {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp), // Increased height
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = backgroundIcon,
                contentDescription = null,
                modifier = Modifier.size(180.dp), // Slightly larger icon
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total Income",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹${NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(totalIncome).drop(1)}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total Spent",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹${NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(totalSpent).drop(1)}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReportInsightsCard(insights: ReportInsights?) {
    if (insights == null) return

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
private fun SpendingBarChart(chartData: Pair<BarData, List<String>>) {
    val (barData, labels) = chartData
    val selectedIndex = labels.size - 1

    val highlightColor = MaterialTheme.colorScheme.primary.toArgb()
    val defaultColor = MaterialTheme.colorScheme.surfaceVariant.toArgb()
    val axisTextColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val valueTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    val colors = labels.indices.map { if (it == selectedIndex) highlightColor else defaultColor }
    (barData.dataSets.first() as BarDataSet).colors = colors

    AndroidView(
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setDrawGridBackground(false)
                setDrawValueAboveBar(true)
                setTouchEnabled(false)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    setDrawAxisLine(false)
                    granularity = 1f
                    valueFormatter = IndexAxisValueFormatter(labels)
                    textColor = axisTextColor
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                }
                axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = axisTextColor and 0x22FFFFFF // Transparent grid lines
                    setDrawLabels(false)
                    setDrawAxisLine(false)
                    axisMinimum = 0f
                }
                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            chart.data = barData
            (chart.data.dataSets.first() as BarDataSet).valueTextColor = valueTextColor
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}
