// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/TimePeriodReportScreen.kt
// REASON: MAJOR REFACTOR - This screen has been completely redesigned to align
// with the "Project Aurora" vision. It now features a dynamic header, a restyled
// bar chart, and a fully glassmorphic layout, creating a modern and cohesive
// user experience for all time-period-based reports. All text and chart colors
// have been carefully selected for high contrast and legibility.
// FIX: Corrected an unresolved reference error by changing the icon from
// InfoOutline to the standard Info icon.
// FIX: Resolved a NullPointerException by ensuring the chart's data is set
// before its properties are accessed in the AndroidView's update block.
// =================================================================================
package io.pm.finlight.ui.screens

import android.app.Application
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import io.pm.finlight.TimePeriod
import io.pm.finlight.TimePeriodReportViewModel
import io.pm.finlight.TimePeriodReportViewModelFactory
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
    timePeriod: TimePeriod
) {
    val application = LocalContext.current.applicationContext as Application
    val factory = TimePeriodReportViewModelFactory(application, timePeriod)
    val viewModel: TimePeriodReportViewModel = viewModel(factory = factory)

    val selectedDate by viewModel.selectedDate.collectAsState()
    val transactions by viewModel.transactionsForPeriod.collectAsState()
    val chartDataPair by viewModel.chartData.collectAsState()

    val totalSpent = transactions.filter { it.transaction.transactionType == "expense" && !it.transaction.isExcluded }.sumOf { it.transaction.amount }

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
                    ) { change, drag ->
                        dragAmount += drag
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
                        timePeriod = timePeriod,
                        selectedDate = selectedDate.time
                    )
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
private fun ReportHeader(totalSpent: Double, timePeriod: TimePeriod, selectedDate: Date) {
    val subtitle = when (timePeriod) {
        TimePeriod.DAILY -> {
            SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(selectedDate)
        }
        TimePeriod.WEEKLY -> {
            val calendar = Calendar.getInstance().apply { time = selectedDate }
            val startOfWeek = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_WEEK, firstDayOfWeek) }.time
            val endOfWeek = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }.time
            val formatter = SimpleDateFormat("dd MMM", Locale.getDefault())
            "For the week of ${formatter.format(startOfWeek)} - ${formatter.format(endOfWeek)}"
        }
        TimePeriod.MONTHLY -> {
            "For ${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(selectedDate)}"
        }
    }

    GlassPanel {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Total Spent",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "₹${NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(totalSpent).drop(1)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
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
            // --- FIX: Assign data to the chart *before* modifying its properties ---
            chart.data = barData
            (chart.data.dataSets.first() as BarDataSet).valueTextColor = valueTextColor
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}
