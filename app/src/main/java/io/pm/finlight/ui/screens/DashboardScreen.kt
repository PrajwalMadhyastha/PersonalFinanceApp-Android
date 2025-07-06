package io.pm.finlight.ui.screens

import android.app.Application
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import io.pm.finlight.BottomNavItem
import io.pm.finlight.DashboardCardType
import io.pm.finlight.DashboardViewModel
import io.pm.finlight.DashboardViewModelFactory
import io.pm.finlight.ui.components.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModelFactory(LocalContext.current.applicationContext as Application)),
) {
    val visibleCards by viewModel.visibleCards.collectAsState()
    val isCustomizationMode by viewModel.isCustomizationMode.collectAsState()
    val showAddCardSheet by viewModel.showAddCardSheet.collectAsState()
    val hiddenCards by viewModel.hiddenCards.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var overscrollJob by remember { mutableStateOf<Job?>(null) }
    val dragDropState = rememberDragDropState(onMove = viewModel::updateCardOrder)

    if (showAddCardSheet) {
        ModalBottomSheet(onDismissRequest = { viewModel.onAddCardSheetDismiss() }) {
            AddCardSheetContent(
                hiddenCards = hiddenCards,
                onAddCard = { cardType ->
                    viewModel.showCard(cardType)
                    viewModel.onAddCardSheetDismiss()
                }
            )
        }
    }

    LazyColumn(
        state = dragDropState.lazyListState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDrag = { change, offset ->
                    change.consume()
                    dragDropState.onDrag(offset)

                    if (overscrollJob?.isActive == true) return@detectDragGesturesAfterLongPress

                    dragDropState
                        .checkForOverScroll()
                        .takeIf { it != 0f }
                        ?.let {
                            overscrollJob = coroutineScope.launch { dragDropState.lazyListState.scrollBy(it) }
                        } ?: run { overscrollJob?.cancel() }
                },
                onDragStart = { offset ->
                    viewModel.enterCustomizationMode()
                    dragDropState.onDragStart(offset)
                },
                onDragEnd = { dragDropState.onDragEnd() },
                onDragCancel = { dragDropState.onDragEnd() }
            )
        }
    ) {
        itemsIndexed(visibleCards, key = { _, item -> item.name }) { index, cardType ->
            val isBeingDragged = index == dragDropState.draggingItemIndex
            val animatedRotation by animateFloatAsState(
                targetValue = if (isBeingDragged) -2f else 0f,
                animationSpec = tween(300),
                label = "DragRotation"
            )
            val animatedElevation by animateFloatAsState(
                targetValue = if (isBeingDragged) 8f else 0f,
                animationSpec = tween(300),
                label = "DragElevation"
            )

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = if (isBeingDragged) dragDropState.draggingItemOffset else 0f
                        rotationZ = animatedRotation
                    }
                    .shadow(elevation = animatedElevation.dp, shape = MaterialTheme.shapes.extraLarge)
            ) {
                DashboardCard(
                    cardType = cardType,
                    navController = navController,
                    viewModel = viewModel,
                    isCustomizationMode = isCustomizationMode,
                    onHide = { viewModel.hideCard(cardType) }
                )
            }
        }
    }
}

@Composable
private fun DashboardCard(
    cardType: DashboardCardType,
    navController: NavController,
    viewModel: DashboardViewModel,
    isCustomizationMode: Boolean,
    onHide: () -> Unit
) {
    val netWorth by viewModel.netWorth.collectAsState()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlyExpenses by viewModel.monthlyExpenses.collectAsState()
    val overallBudget by viewModel.overallMonthlyBudget.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val accountsSummary by viewModel.accountsSummary.collectAsState()
    val safeToSpendPerDay by viewModel.safeToSpendPerDay.collectAsState()
    val budgetStatus by viewModel.budgetStatus.collectAsState()

    Box {
        when (cardType) {
            DashboardCardType.OVERALL_BUDGET -> AuroraMonthlyBudgetCard(
                totalBudget = overallBudget,
                amountSpent = monthlyExpenses.toFloat(),
                navController = navController,
            )
            DashboardCardType.QUICK_STATS -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AuroraStatCard(
                    label = "Income",
                    amount = monthlyIncome.toFloat(),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        navController.navigate("income_screen")
                    }
                )
                AuroraStatCard(
                    label = "Budget",
                    amount = overallBudget,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("budget_screen") }
                )
                AuroraStatCard(
                    label = "Safe To Spend",
                    amount = safeToSpendPerDay,
                    isPerDay = true,
                    modifier = Modifier.weight(1f)
                )
            }
            DashboardCardType.QUICK_ACTIONS -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FilledTonalButton(
                    onClick = {
                        navController.navigate(BottomNavItem.Reports.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Trends")
                }
                FilledTonalButton(
                    onClick = {
                        navController.navigate(BottomNavItem.Reports.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Categories")
                }
            }
            DashboardCardType.NET_WORTH -> AuroraNetWorthCard(netWorth)
            DashboardCardType.RECENT_ACTIVITY -> AuroraRecentActivityCard(recentTransactions, navController)
            DashboardCardType.ACCOUNTS_CAROUSEL -> AccountsCarouselCard(accounts = accountsSummary, navController = navController)
            DashboardCardType.BUDGET_WATCH -> BudgetWatchCard(
                budgetStatus = budgetStatus,
                navController = navController,
            )
        }
        if (isCustomizationMode) {
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onHide, modifier = Modifier.size(36.dp).padding(4.dp)) {
                    Icon(
                        Icons.Default.RemoveCircleOutline,
                        contentDescription = "Hide Card",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag to reorder",
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun AddCardSheetContent(
    hiddenCards: List<DashboardCardType>,
    onAddCard: (DashboardCardType) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
        Text("Add a Card to Your Dashboard", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        if (hiddenCards.isEmpty()) {
            Text("All available cards are already on your dashboard.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(hiddenCards) { cardType ->
                    ListItem(
                        headlineContent = { Text(cardType.name.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }) },
                        leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                        modifier = Modifier.clickable { onAddCard(cardType) }
                    )
                }
            }
        }
    }
}
