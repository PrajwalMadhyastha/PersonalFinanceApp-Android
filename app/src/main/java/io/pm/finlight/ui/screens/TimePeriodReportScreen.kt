// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/TimePeriodReportScreen.kt
// REASON: NEW FILE - This screen replaces the old DailyReportScreen. It is now
// a generic component that can display reports for any `TimePeriod` (daily,
// weekly, monthly), making the UI more reusable and scalable.
// =================================================================================
package io.pm.finlight.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import io.pm.finlight.TimePeriod
import io.pm.finlight.TimePeriodReportViewModel
import io.pm.finlight.TimePeriodReportViewModelFactory
import io.pm.finlight.ui.components.TransactionItem
import java.text.SimpleDateFormat
import java.util.*

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
    val weeklyChartData by viewModel.chartData.collectAsState()

    val totalSpent = transactions.filter { it.transaction.transactionType == "expense" }.sumOf { it.transaction.amount }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (timePeriod == TimePeriod.DAILY) {
            NotificationManagerCompat.from(context).cancel(2) // Daily Report Notification ID
        }
    }

    Scaffold(
        topBar = {
            ReportTopAppBar(
                selectedDate = selectedDate.time,
                timePeriod = timePeriod,
                onBack = { navController.popBackStack() },
                onPrevious = { viewModel.selectPreviousPeriod() },
                onNext = { viewModel.selectNextPeriod() }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PeriodSummaryCard(totalSpent = totalSpent, timePeriod = timePeriod)
            }

            item {
                WeeklySpendingChart(chartData = weeklyChartData)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportTopAppBar(
    selectedDate: Date,
    timePeriod: TimePeriod,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val dateFormatter = remember(timePeriod) {
        when (timePeriod) {
            TimePeriod.DAILY -> SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
            TimePeriod.WEEKLY -> SimpleDateFormat("'Week of' dd MMMM", Locale.getDefault())
            TimePeriod.MONTHLY -> SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        }
    }

    TopAppBar(
        title = {
            Text(
                text = dateFormatter.format(selectedDate),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            Row {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Period")
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Period")
                }
            }
        }
    )
}

@Composable
private fun PeriodSummaryCard(totalSpent: Double, timePeriod: TimePeriod) {
    val title = when (timePeriod) {
        TimePeriod.DAILY -> "Today"
        TimePeriod.WEEKLY -> "This Week"
        TimePeriod.MONTHLY -> "This Month"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingBasket,
                contentDescription = "Spending",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "₹${"%,.2f".format(totalSpent)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun WeeklySpendingChart(chartData: Pair<BarData, List<String>>?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (chartData != null) {
                AndroidView(
                    factory = { context ->
                        BarChart(context).apply {
                            description.isEnabled = false
                            legend.isEnabled = false
                            setDrawGridBackground(false)
                            setDrawValueAboveBar(true)

                            xAxis.apply {
                                position = XAxis.XAxisPosition.BOTTOM
                                setDrawGridLines(false)
                                granularity = 1f
                                valueFormatter = IndexAxisValueFormatter(chartData.second)
                            }
                            axisLeft.apply {
                                setDrawGridLines(false)
                                axisMinimum = 0f
                            }
                            axisRight.isEnabled = false
                        }
                    },
                    update = { chart ->
                        chart.data = chartData.first
                        chart.invalidate()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Not enough data for trend.")
                }
            }
        }
    }
}