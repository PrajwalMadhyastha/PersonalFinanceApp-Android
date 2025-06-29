package io.pm.finlight.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import io.pm.finlight.AccountWithBalance
import io.pm.finlight.BottomNavItem
import io.pm.finlight.Budget
import io.pm.finlight.BudgetViewModel
import io.pm.finlight.BudgetWithSpending
import io.pm.finlight.CategoryIconHelper
import io.pm.finlight.TransactionDetails
import kotlinx.coroutines.flow.map
import kotlin.math.sin

private fun formatAmountCompact(amount: Float): String {
    return when {
        amount >= 1_000_000 -> "₹${"%.1f".format(amount / 1_000_000)}M"
        amount >= 1_000 -> "₹${"%.1f".format(amount / 1_000)}k"
        else -> "₹${"%.0f".format(amount)}"
    }
}

@Composable
fun StatCard(
    label: String,
    amount: Float,
    modifier: Modifier = Modifier,
    isPerDay: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${formatAmountCompact(amount)}${if (isPerDay) "/day" else ""}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun OverallBudgetCard(
    totalBudget: Float,
    amountSpent: Float,
    navController: NavController
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("budget_screen") },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Monthly Budget", style = MaterialTheme.typography.titleLarge)
                if (totalBudget > 0) {
                    TextButton(onClick = { navController.navigate("budget_screen") }) {
                        Text("Edit")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (totalBudget <= 0) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "You haven't set a budget for this month yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { navController.navigate("budget_screen") }) {
                        Text("Set Budget")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    LiquidTumbler(
                        progress = (amountSpent / totalBudget),
                        modifier = Modifier.size(120.dp)
                    )
                    Column {
                        Text("Spent", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "₹${"%.2f".format(amountSpent)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Remaining", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "₹${"%.2f".format(totalBudget - amountSpent)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun LiquidTumbler(progress: Float, modifier: Modifier = Modifier) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "LiquidFillAnimation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "WaveAnimation")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing)
        ), label = "WaveOffset"
    )

    val waterColor = when {
        clampedProgress >= 1f -> MaterialTheme.colorScheme.error
        clampedProgress > 0.8f -> Color(0xFFFBC02D) // Amber
        else -> MaterialTheme.colorScheme.primary
    }
    val glassColor = MaterialTheme.colorScheme.onSurfaceVariant
    val strokeWidthPx = with(LocalDensity.current) { 4.dp.toPx() }


    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val glassPath = Path().apply {
                moveTo(width * 0.1f, height * 0.05f)
                lineTo(width * 0.2f, height * 0.95f)
                quadraticBezierTo(width * 0.5f, height * 1.05f, width * 0.8f, height * 0.95f)
                lineTo(width * 0.9f, height * 0.05f)
                close()
            }

            drawPath(
                path = glassPath,
                color = glassColor,
                style = Stroke(width = strokeWidthPx)
            )

            clipPath(glassPath) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(waterColor.copy(alpha = 0.5f), waterColor),
                        startY = height * (1 - animatedProgress),
                        endY = height
                    ),
                    topLeft = Offset(0f, height * (1 - animatedProgress)),
                    size = size
                )

                val wavePath = Path().apply {
                    moveTo(-width, height * (1 - animatedProgress))
                    for (i in 0..width.toInt() * 2) {
                        lineTo(
                            i.toFloat(),
                            height * (1 - animatedProgress) + sin((i * 0.03f) + (waveOffset * Math.PI.toFloat())) * 5f
                        )
                    }
                    lineTo(width * 2, height)
                    lineTo(-width, height)
                    close()
                }
                drawPath(path = wavePath, color = waterColor)
            }
        }
        Text(
            text = "${(clampedProgress * 100).toInt()}%",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
fun NetWorthCard(netWorth: Double) {
    Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Net Worth", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "₹${"%.2f".format(netWorth)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AccountSummaryCard(accounts: List<AccountWithBalance>, navController: NavController) {
    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Your Accounts",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        navController.navigate("account_list")
                    }
                ) { Text("View All") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (accounts.isEmpty()) {
                Text("No accounts found. Add one from the Settings.", modifier = Modifier.padding(vertical = 16.dp))
            } else {
                Column {
                    accounts.forEachIndexed { index, accountWithBalance ->
                        if (index > 0) HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("account_detail/${accountWithBalance.account.id}") }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = accountWithBalance.account.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = accountWithBalance.account.type,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "₹${"%.2f".format(accountWithBalance.balance)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (accountWithBalance.balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentActivityCard(transactions: List<TransactionDetails>, navController: NavController) {
    Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Transactions", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
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
            if(transactions.isEmpty()){
                Text("No transactions yet.", modifier = Modifier.padding(vertical = 16.dp))
            } else {
                transactions.forEach { details ->
                    TransactionItem(
                        transactionDetails = details,
                        // --- UPDATED: Navigate to the new detail screen ---
                        onClick = {
                            navController.navigate("transaction_detail/${details.transaction.id}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetWatchCard(
    budgetStatus: List<BudgetWithSpending>,
    viewModel: BudgetViewModel,
    navController: NavController
) {
    Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Budget Watch", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { navController.navigate("add_budget") }) { Text("+ Add Category Budget") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (budgetStatus.isEmpty()) {
                Text("No category-specific budgets set for this month.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 16.dp))
            } else {
                budgetStatus.forEach { budgetWithSpendingItem ->
                    BudgetItem(budgetWithSpending = budgetWithSpendingItem)
                }
            }
        }
    }
}


@Composable
fun BudgetItem(budgetWithSpending: BudgetWithSpending) {
    val budget = budgetWithSpending.budget
    val actualSpending = budgetWithSpending.spent
    val progress = if (budget.amount > 0) (actualSpending / budget.amount).toFloat() else 0f
    val amountRemaining = budget.amount - actualSpending
    val progressColor = when {
        progress > 1f -> MaterialTheme.colorScheme.error
        progress > 0.8f -> Color(0xFFFBC02D) // Amber
        else -> MaterialTheme.colorScheme.primary
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        CategoryIconHelper.getIconBackgroundColor(
                            budgetWithSpending.colorKey ?: "gray_light"
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (budgetWithSpending.iconKey == "letter_default") {
                    Text(
                        text = budgetWithSpending.budget.categoryName.firstOrNull()?.uppercase() ?: "?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                } else {
                    Icon(
                        imageVector = CategoryIconHelper.getIcon(budgetWithSpending.iconKey ?: "category"),
                        contentDescription = budgetWithSpending.budget.categoryName,
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = budget.categoryName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(text = "₹${"%.0f".format(budget.amount)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp), color = progressColor)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Spent: ₹${"%.2f".format(actualSpending)}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Remaining: ₹${"%.2f".format(amountRemaining)}", style = MaterialTheme.typography.bodySmall, color = if (amountRemaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        }
    }
}
