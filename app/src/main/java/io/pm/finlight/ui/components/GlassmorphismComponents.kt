package io.pm.finlight.ui.components

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import io.pm.finlight.*
import io.pm.finlight.ui.theme.AuroraPrimary
import io.pm.finlight.ui.theme.AuroraSecondary
import io.pm.finlight.ui.theme.GlassPanelBorder
import io.pm.finlight.ui.theme.GlassPanelFill
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A reusable composable that creates a "glassmorphism" effect panel.
 *
 * @param modifier The modifier to be applied to the panel.
 * @param isCustomizationMode If true, a dashed border is shown.
 * @param content The content to be placed inside the panel.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    isCustomizationMode: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val borderModifier = if (isCustomizationMode) {
        Modifier.border(
            width = 1.dp,
            brush = Brush.horizontalGradient(listOf(GlassPanelBorder, GlassPanelBorder.copy(alpha = 0.5f))),
            shape = RoundedCornerShape(24.dp)
        )
    } else {
        Modifier.border(1.dp, GlassPanelBorder, RoundedCornerShape(24.dp))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(GlassPanelFill)
            .then(borderModifier),
        content = content
    )
}

/**
 * The new hero card for the dashboard, displaying the monthly budget status
 * with a visually rich "Aurora" theme and glassmorphism effect.
 *
 * @param totalBudget The total budget for the month.
 * @param amountSpent The amount spent so far in the month.
 * @param navController The NavController for navigation.
 */
@Composable
fun DashboardHeroCard(
    totalBudget: Float,
    amountSpent: Float,
    income: Float,
    safeToSpend: Float,
    navController: NavController
) {
    val remainingAmount = totalBudget - amountSpent

    val progress = if (totalBudget > 0) (amountSpent / totalBudget) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1500, easing = EaseOutCubic),
        label = "BudgetProgressAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Monthly Budget",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Remaining",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(remainingAmount.toInt())}",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AuroraProgressBar(progress = animatedProgress)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent: ₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(amountSpent.toInt())}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Total: ₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(totalBudget.toInt())}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItem(label = "Income", amount = income, onClick = { navController.navigate("income_screen") })
            StatItem(label = "Budget", amount = totalBudget, isCurrency = true, onClick = { navController.navigate("budget_screen") })
            StatItem(label = "Safe to Spend", amount = safeToSpend, isPerDay = true)
        }
    }
}

@Composable
private fun StatItem(label: String, amount: Float, isCurrency: Boolean = true, isPerDay: Boolean = false, onClick: (() -> Unit)? = null) {
    val animatedAmount by animateFloatAsState(
        targetValue = amount,
        animationSpec = tween(durationMillis = 1500, easing = EaseOutCubic),
        label = "StatItemAnimation"
    )
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = clickableModifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isCurrency) {
                Text(
                    text = "₹",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = NumberFormat.getNumberInstance(Locale("en", "IN")).format(animatedAmount.toInt()),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isPerDay) {
                Text(
                    text = "/day",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp, top = 4.dp)
                )
            }
        }
    }
}


/**
 * A custom styled Progress Bar that includes a percentage indicator.
 *
 * @param progress The progress to display, from 0.0 to 1.0.
 */
@Composable
private fun AuroraProgressBar(progress: Float) {
    val animatedPercentage = (progress * 100).roundToInt()
    val progressColor = when {
        progress > 0.9 -> MaterialTheme.colorScheme.error
        progress > 0.7 -> AuroraSecondary
        else -> AuroraPrimary
    }

    Layout(
        content = {
            Text(
                text = "$animatedPercentage%",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelSmall
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            ) {
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.2f),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
                    style = Stroke(width = 1.dp.toPx()),
                    topLeft = Offset(0f, 1.dp.toPx())
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.1f),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
                )

                if (progress > 0) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(progressColor.copy(alpha = 0.6f), progressColor)
                        ),
                        size = Size(width = size.width * progress, height = size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
                    )
                }
            }
        }
    ) { measurables, constraints ->
        val textPlaceable = measurables[0].measure(Constraints())
        val canvasPlaceable = measurables[1].measure(constraints)

        val progressWidth = (canvasPlaceable.width * progress).toInt()
        val textX = (progressWidth - textPlaceable.width / 2).coerceIn(
            0,
            canvasPlaceable.width - textPlaceable.width
        )
        val textY = (canvasPlaceable.height - textPlaceable.height) / 2

        layout(canvasPlaceable.width, canvasPlaceable.height + textPlaceable.height + 4.dp.roundToPx()) {
            canvasPlaceable.placeRelative(0, textPlaceable.height + 4.dp.roundToPx())
            textPlaceable.placeRelative(textX, 0)
        }
    }
}

/**
 * A dashboard card that displays a horizontally scrolling carousel of user accounts.
 *
 * @param accounts The list of accounts with their balances.
 * @param navController The NavController for navigation.
 */
@Composable
fun AccountsCarouselCard(
    accounts: List<AccountWithBalance>,
    navController: NavController
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Accounts",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(accounts) { account ->
                AccountItem(account = account, navController = navController)
            }
        }
    }
}

/**
 * An individual item in the AccountsCarouselCard, styled to look like a mini glass credit card.
 *
 * @param account The account data to display.
 * @param navController The NavController for navigation.
 */
@Composable
private fun AccountItem(account: AccountWithBalance, navController: NavController) {
    GlassPanel(
        modifier = Modifier
            .width(180.dp)
            .height(110.dp)
            .clickable { navController.navigate("account_detail/${account.account.id}") }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = painterResource(id = BankLogoHelper.getLogoForAccount(account.account.name)),
                contentDescription = "${account.account.name} Logo",
                modifier = Modifier.height(24.dp)
            )
            Column {
                Text(
                    text = account.account.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(account.balance)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * A dashboard card that displays radial gauges for individual category budgets.
 *
 * @param budgetStatus A list of budgets with their current spending.
 * @param navController The NavController for navigation.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetWatchCard(
    budgetStatus: List<BudgetWithSpending>,
    navController: NavController
) {
    GlassPanel {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Budget Watch",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (budgetStatus.isEmpty()) {
                Text(
                    "No category-specific budgets set for this month.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    maxItemsInEachRow = 4
                ) {
                    budgetStatus.forEach { budget ->
                        CategoryBudgetGauge(budget = budget, navController = navController)
                    }
                }
            }
        }
    }
}

/**
 * A radial gauge for displaying the status of a single category budget.
 *
 * @param budget The budget data with spending information.
 * @param navController The NavController for navigation.
 */
@Composable
private fun CategoryBudgetGauge(budget: BudgetWithSpending, navController: NavController) {
    val progress = if (budget.budget.amount > 0) (budget.spent / budget.budget.amount).toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1500),
        label = "CategoryBudgetGaugeAnimation"
    )
    val remaining = budget.budget.amount - budget.spent

    val progressBrush = Brush.sweepGradient(
        colors = listOf(
            AuroraPrimary.copy(alpha = 0.5f),
            AuroraPrimary
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable { navController.navigate("budget_screen") }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 8.dp.toPx()
                val diameter = min(size.width, size.height) - strokeWidth
                drawArc(
                    color = Color.White.copy(alpha = 0.1f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
                drawArc(
                    brush = progressBrush,
                    startAngle = -90f,
                    sweepAngle = 360 * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            Icon(
                imageVector = CategoryIconHelper.getIcon(budget.iconKey ?: "category"),
                contentDescription = budget.budget.categoryName,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(32.dp)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = budget.budget.categoryName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(remaining.toInt())} left",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * A dashboard card for displaying the user's net worth.
 *
 * @param netWorth The calculated net worth.
 */
@Composable
fun AuroraNetWorthCard(netWorth: Double) {
    GlassPanel {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "Net Worth",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(netWorth)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * A dashboard card for displaying recent transactions.
 *
 * @param transactions The list of recent transactions.
 * @param navController The NavController for navigation.
 */
@Composable
fun AuroraRecentActivityCard(transactions: List<TransactionDetails>, navController: NavController) {
    GlassPanel {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = {
                        navController.navigate(BottomNavItem.Transactions.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                ) { Text("View All") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (transactions.isEmpty()) {
                Text(
                    "No transactions yet.",
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    transactions.forEach { details ->
                        TransactionItem(
                            transactionDetails = details,
                            onClick = {
                                navController.navigate("transaction_detail/${details.transaction.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * --- NEW ---
 * A dashboard card that provides quick navigation actions.
 *
 * @param navController The NavController for navigation.
 */
@Composable
fun AuroraQuickActionsCard(navController: NavController) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min), // Ensures divider stretches
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickActionItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Timeline,
                text = "View Trends",
                onClick = {
                    // Navigate to the main reports screen
                    navController.navigate(BottomNavItem.Reports.route)
                }
            )
            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
            QuickActionItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.PieChart,
                text = "View Categories",
                onClick = {
                    // Navigate to the main reports screen
                    navController.navigate(BottomNavItem.Reports.route)
                }
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
