// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/TimePeriodReportScreen.kt
// REASON: MAJOR REFACTOR - This screen has been completely redesigned to match
// a modern, visually appealing layout. It now features a new summary header,
// a restyled bar chart with a highlighted selected day, and swipe-based
// navigation to move between periods, removing the old navigation arrows.
// FIX: Corrected the horizontal drag gesture detection to use the correct
// parameter type, resolving a compilation error.
// UPDATE: The ReportHeader is now more dynamic, displaying more specific and
// user-friendly date ranges for the Daily, Weekly, and Monthly views to make
// each report feel distinct.
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
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import io.pm.finlight.TimePeriod
import io.pm.finlight.TimePeriodReportViewModel
import io.pm.finlight.TimePeriodReportViewModelFactory
import io.pm.finlight.ui.components.TransactionItem
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
                title = { Text(
                    text = when(timePeriod) {
                        TimePeriod.DAILY -> "Daily Report"
                        TimePeriod.WEEKLY -> "Weekly Report"
                        TimePeriod.MONTHLY -> "Monthly Report"
                    }
                ) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        var dragAmount by remember { mutableStateOf(0f) }

        Box(
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
                        // --- FIX: The 'drag' parameter is a Float, not an Offset ---
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
                    chartDataPair?.let {
                        SpendingBarChart(
                            chartData = it,
                            timePeriod = timePeriod
                        )
                    }
                }

                if (transactions.isNotEmpty()) {
                    item {
                        Text(
                            "Top Spends",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(transactions) { transaction ->
                        TransactionItem(
                            transactionDetails = transaction,
                            onClick = { navController.navigate("transaction_detail/${transaction.transaction.id}") }
                        )
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No transactions for this period.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportHeader(totalSpent: Double, timePeriod: TimePeriod, selectedDate: Date) {
    val title = when (timePeriod) {
        TimePeriod.DAILY -> "Total Spend"
        TimePeriod.WEEKLY -> "Total Spend"
        TimePeriod.MONTHLY -> "Total Spend"
    }

    // --- UPDATE: More descriptive and dynamic subtitles for each time period ---
    val subtitle = when(timePeriod) {
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

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingBasket,
            contentDescription = "Spending",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "₹${"%,.0f".format(totalSpent)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SpendingBarChart(chartData: Pair<BarData, List<String>>, timePeriod: TimePeriod) {
    val (barData, labels) = chartData
    val selectedIndex = labels.size - 1 // The last item is the current period

    // --- FIX: Read theme colors in the composable scope, not in the factory lambda ---
    val highlightColor = MaterialTheme.colorScheme.primary.toArgb()
    val defaultColor = MaterialTheme.colorScheme.surfaceVariant.toArgb()
    val axisTextColor = MaterialTheme.colorScheme.onSurface.toArgb()

    val colors = labels.indices.map { if (it == selectedIndex) highlightColor else defaultColor }
    (barData.dataSets.first() as BarDataSet).colors = colors

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                            // --- FIX: Use the color variable read from the theme ---
                            textColor = axisTextColor
                            textSize = 12f
                            typeface = Typeface.DEFAULT_BOLD
                        }
                        axisLeft.apply {
                            setDrawGridLines(false)
                            setDrawLabels(false)
                            setDrawAxisLine(false)
                            axisMinimum = 0f
                        }
                        axisRight.isEnabled = false
                    }
                },
                update = { chart ->
                    chart.data = barData
                    chart.invalidate()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}
