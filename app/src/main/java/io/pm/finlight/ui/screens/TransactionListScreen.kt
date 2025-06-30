package io.pm.finlight.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.pm.finlight.TransactionViewModel
import io.pm.finlight.ui.components.TransactionList
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionListScreen(
    navController: NavController,
    viewModel: TransactionViewModel,
) {
    val tabs = listOf("Transactions", "Categories", "Merchants")
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()

    val transactions by viewModel.transactionsForSelectedMonth.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val recentMonths by viewModel.recentMonths.collectAsState()
    val totalSpent by viewModel.monthlyExpenses.collectAsState()
    val totalIncome by viewModel.monthlyIncome.collectAsState()
    val budget by viewModel.overallMonthlyBudget.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        MonthlySummaryHeader(
            selectedMonth = selectedMonth,
            recentMonths = recentMonths,
            totalSpent = totalSpent,
            totalIncome = totalIncome,
            budget = budget,
            onMonthSelected = { viewModel.setSelectedMonth(it) }
        )
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> {
                    TransactionList(transactions = transactions, navController = navController)
                }
                1 -> PlaceholderTabContent(title = "Categories")
                2 -> PlaceholderTabContent(title = "Merchants")
            }
        }
    }
}

@Composable
fun MonthlySummaryHeader(
    selectedMonth: Calendar,
    recentMonths: List<Calendar>,
    totalSpent: Double,
    totalIncome: Double,
    budget: Float,
    onMonthSelected: (Calendar) -> Unit
) {
    val monthYearFormat = SimpleDateFormat("MMM yy", Locale.getDefault())
    val monthDayFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

    val selectedTabIndex = recentMonths.indexOfFirst {
        it.get(Calendar.MONTH) == selectedMonth.get(Calendar.MONTH) &&
                it.get(Calendar.YEAR) == selectedMonth.get(Calendar.YEAR)
    }.coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
            indicator = {}, // No indicator needed
            divider = {}
        ) {
            recentMonths.forEach { month ->
                val isSelected = month.get(Calendar.MONTH) == selectedMonth.get(Calendar.MONTH) &&
                        month.get(Calendar.YEAR) == selectedMonth.get(Calendar.YEAR)
                Tab(
                    selected = isSelected,
                    onClick = { onMonthSelected(month) },
                    text = {
                        Text(
                            text = monthYearFormat.format(month.time),
                            style = if (isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Total Spent", style = MaterialTheme.typography.labelMedium)
                Text(
                    "₹${"%,.2f".format(totalSpent)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Total Income", style = MaterialTheme.typography.labelMedium)
                Text(
                    "₹${"%,.2f".format(totalIncome)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (budget > 0) {
            BudgetProgress(
                spent = totalSpent.toFloat(),
                budget = budget,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            Text(
                text = "No budget set for this month.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        }
    }
}


@Composable
fun BudgetProgress(spent: Float, budget: Float, modifier: Modifier = Modifier) {
    val progress = (spent / budget).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000), label = "")

    val progressColor = when {
        progress > 1f -> MaterialTheme.colorScheme.error
        progress > 0.85f -> Color(0xFFFBC02D) // Amber
        else -> MaterialTheme.colorScheme.primary
    }

    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "Spent: ₹${"%,.0f".format(spent)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Budget: ₹${"%,.0f".format(budget)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
fun PlaceholderTabContent(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Content for $title", style = MaterialTheme.typography.headlineMedium)
    }
}

// Simple non-composable lerp for Dp values
private fun lerp(start: Dp, stop: Dp, fraction: Float): Dp {
    return Dp(start.value + (stop.value - start.value) * fraction)
}


@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pagerTabIndicatorOffset(
    pagerState: PagerState,
    tabPositions: List<TabPosition>,
): Modifier = composed {
    if (tabPositions.isEmpty()) {
        this
    } else {
        val currentPage = pagerState.currentPage
        val fraction = pagerState.currentPageOffsetFraction.absoluteValue

        val currentTab = tabPositions[currentPage]
        val nextTab = tabPositions.getOrNull(currentPage + 1)

        val targetIndicatorOffset = if (nextTab != null) {
            lerp(currentTab.left, nextTab.left, fraction)
        } else {
            currentTab.left
        }

        val indicatorWidth = if (nextTab != null) {
            lerp(currentTab.width, nextTab.width, fraction)
        } else {
            currentTab.width
        }

        this.fillMaxWidth()
            .wrapContentSize(Alignment.BottomStart)
            .offset(x = targetIndicatorOffset)
            .width(indicatorWidth)
    }
}
