// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/IncomeScreen.kt
// REASON: MAJOR REFACTOR - The `IncomeHeader` has been updated to align with the
// "Project Aurora" vision. The standard `Card` has been replaced with a `GlassPanel`
// component, and all text colors are now theme-aware, ensuring a cohesive,
// modern, and high-contrast user experience for the income screen.
// =================================================================================
package io.pm.finlight.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.pm.finlight.IncomeViewModel
import io.pm.finlight.MonthlySummaryItem
import io.pm.finlight.ui.components.FilterBottomSheet
import io.pm.finlight.ui.components.GlassPanel
import io.pm.finlight.ui.components.TransactionList
import io.pm.finlight.ui.components.pagerTabIndicatorOffset
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun IncomeScreen(
    navController: NavController,
    viewModel: IncomeViewModel = viewModel(),
) {
    val tabs = listOf("Credits", "Categories")
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()

    val incomeTransactions by viewModel.incomeTransactionsForSelectedMonth.collectAsState()
    val incomeByCategory by viewModel.incomeByCategoryForSelectedMonth.collectAsState()
    val totalIncome by viewModel.totalIncomeForSelectedMonth.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val monthlySummaries by viewModel.monthlySummaries.collectAsState()

    val filterState by viewModel.filterState.collectAsState()
    val allAccounts by viewModel.allAccounts.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState(initial = emptyList())
    var showFilterSheet by remember { mutableStateOf(false) }

    val areFiltersActive by remember(filterState) {
        derivedStateOf {
            filterState.keyword.isNotBlank() || filterState.account != null || filterState.category != null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Income") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (areFiltersActive) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    ) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter Income")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            IncomeHeader(
                totalIncome = totalIncome,
                selectedMonth = selectedMonth,
                monthlySummaries = monthlySummaries,
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
                    0 -> TransactionList(transactions = incomeTransactions, navController = navController)
                    1 -> CategorySpendingScreen(spendingList = incomeByCategory)
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            FilterBottomSheet(
                filterState = filterState,
                accounts = allAccounts,
                categories = allCategories,
                onKeywordChange = viewModel::updateFilterKeyword,
                onAccountChange = viewModel::updateFilterAccount,
                onCategoryChange = viewModel::updateFilterCategory,
                onClearFilters = viewModel::clearFilters
            )
        }
    }
}

@Composable
fun IncomeHeader(
    selectedMonth: Calendar,
    monthlySummaries: List<MonthlySummaryItem>,
    totalIncome: Double,
    onMonthSelected: (Calendar) -> Unit
) {
    val monthFormat = SimpleDateFormat("LLL", Locale.getDefault())
    val monthYearFormat = SimpleDateFormat("LLLL yyyy", Locale.getDefault())
    var showMonthScroller by remember { mutableStateOf(false) }

    val selectedTabIndex = monthlySummaries.indexOfFirst {
        it.calendar.get(Calendar.MONTH) == selectedMonth.get(Calendar.MONTH) &&
                it.calendar.get(Calendar.YEAR) == selectedMonth.get(Calendar.YEAR)
    }.coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showMonthScroller = !showMonthScroller }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = monthYearFormat.format(selectedMonth.time),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = if (showMonthScroller) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = if (showMonthScroller) "Hide month selector" else "Show month selector",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        AnimatedVisibility(
            visible = showMonthScroller,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                indicator = {},
                divider = {}
            ) {
                monthlySummaries.forEach { summaryItem ->
                    val isSelected = summaryItem.calendar.get(Calendar.MONTH) == selectedMonth.get(Calendar.MONTH) &&
                            summaryItem.calendar.get(Calendar.YEAR) == selectedMonth.get(Calendar.YEAR)
                    Tab(
                        selected = isSelected,
                        onClick = {
                            onMonthSelected(summaryItem.calendar)
                            showMonthScroller = false
                        },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = monthFormat.format(summaryItem.calendar.time),
                                    style = if (isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "₹${"%,.0f".format(summaryItem.totalSpent)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text("Total Income", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "₹${"%,.2f".format(totalIncome)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
