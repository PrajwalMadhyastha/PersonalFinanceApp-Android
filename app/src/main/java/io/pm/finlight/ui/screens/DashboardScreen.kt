// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/DashboardScreen.kt
// REASON: FEATURE - The `onCategoryClick` callback is now passed down from the
// DashboardScreen through the `DashboardCard` to the `AuroraRecentActivityCard`.
// This connects the UI event to the ViewModel, enabling the in-place category
// change feature from the dashboard.
// =================================================================================
package io.pm.finlight.ui.screens

import android.app.Application
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.pm.finlight.*
import io.pm.finlight.ui.components.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Date

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
    val yearlyConsistencyData by viewModel.yearlyConsistencyData.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var overscrollJob by remember { mutableStateOf<Job?>(null) }
    val dragDropState = rememberDragDropState(onMove = viewModel::updateCardOrder)

    if (showAddCardSheet) {
        val sheetContainerColor = if (isSystemInDarkTheme()) {
            Color(0xFF2C2C34)
        } else {
            BottomSheetDefaults.ContainerColor
        }

        ModalBottomSheet(
            onDismissRequest = { viewModel.onAddCardSheetDismiss() },
            containerColor = sheetContainerColor
        ) {
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .testTag("dashboard_lazy_column")
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDrag = { change, offset ->
                        change.consume()
                        dragDropState.onDrag(offset)

                        if (overscrollJob?.isActive == true) return@detectDragGesturesAfterLongPress

                        dragDropState
                            .checkForOverScroll()
                            .takeIf { it != 0f }
                            ?.let {
                                overscrollJob =
                                    coroutineScope.launch { dragDropState.lazyListState.scrollBy(it) }
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

            val infiniteTransition = rememberInfiniteTransition(label = "giggle_animation")
            val giggleRotation by infiniteTransition.animateFloat(
                initialValue = -0.8f,
                targetValue = 0.8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(150, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "giggle"
            )

            val dragTiltRotation by animateFloatAsState(
                targetValue = if (isBeingDragged) -2f else 0f,
                animationSpec = tween(300),
                label = "DragRotation"
            )

            val finalRotation = when {
                isBeingDragged -> dragTiltRotation
                isCustomizationMode && cardType != DashboardCardType.HERO_BUDGET -> giggleRotation
                else -> 0f
            }

            val animatedElevation by animateFloatAsState(
                targetValue = if (isBeingDragged) 8f else 0f,
                animationSpec = tween(300),
                label = "DragElevation"
            )

            Box(
                modifier = Modifier
                    .animateItemPlacement()
                    .graphicsLayer {
                        translationY = if (isBeingDragged) dragDropState.draggingItemTranslationY else 0f
                        rotationZ = finalRotation
                    }
                    .shadow(elevation = animatedElevation.dp, shape = MaterialTheme.shapes.extraLarge)
            ) {
                DashboardCard(
                    cardType = cardType,
                    navController = navController,
                    viewModel = viewModel,
                    isCustomizationMode = isCustomizationMode,
                    onHide = { viewModel.hideCard(cardType) },
                    yearlyConsistencyData = yearlyConsistencyData,
                    onCategoryClick = { viewModel.requestCategoryChange(it) }
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
    onHide: () -> Unit,
    yearlyConsistencyData: List<CalendarDayStatus>,
    onCategoryClick: (TransactionDetails) -> Unit
) {
    val netWorth by viewModel.netWorth.collectAsState()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlyExpenses by viewModel.monthlyExpenses.collectAsState()
    val overallBudget by viewModel.overallMonthlyBudget.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val accountsSummary by viewModel.accountsSummary.collectAsState()
    val safeToSpendPerDay by viewModel.safeToSpendPerDay.collectAsState()
    val budgetStatus by viewModel.budgetStatus.collectAsState()
    val amountRemaining by viewModel.amountRemaining.collectAsState()
    val monthYear = viewModel.monthYear

    Box {
        when (cardType) {
            DashboardCardType.HERO_BUDGET -> DashboardHeroCard(
                totalBudget = overallBudget,
                amountSpent = monthlyExpenses.toFloat(),
                amountRemaining = amountRemaining,
                income = monthlyIncome.toFloat(),
                safeToSpend = safeToSpendPerDay,
                navController = navController,
                monthYear = monthYear
            )
            DashboardCardType.QUICK_ACTIONS -> AuroraQuickActionsCard(navController = navController)
            DashboardCardType.NET_WORTH -> AuroraNetWorthCard(netWorth)
            DashboardCardType.RECENT_ACTIVITY -> AuroraRecentActivityCard(recentTransactions, navController, onCategoryClick)
            DashboardCardType.ACCOUNTS_CAROUSEL -> AccountsCarouselCard(accounts = accountsSummary, navController = navController)
            DashboardCardType.BUDGET_WATCH -> BudgetWatchCard(
                budgetStatus = budgetStatus,
                navController = navController,
            )
            DashboardCardType.SPENDING_CONSISTENCY -> {
                GlassPanel {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Yearly Spending Consistency",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (yearlyConsistencyData.isEmpty()) {
                            CircularProgressIndicator()
                        } else {
                            ConsistencyCalendar(
                                data = yearlyConsistencyData,
                                onDayClick = { date ->
                                    navController.navigate("search_screen?date=${date.time}&focusSearch=false")
                                }
                            )
                        }
                    }
                }
            }
        }
        if (isCustomizationMode && cardType != DashboardCardType.HERO_BUDGET) {
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
        Text(
            "Add a Card to Your Dashboard",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(16.dp))
        if (hiddenCards.isEmpty()) {
            Text("All available cards are already on your dashboard.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(hiddenCards, key = { _, cardType -> cardType.name }) { _, cardType ->
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
