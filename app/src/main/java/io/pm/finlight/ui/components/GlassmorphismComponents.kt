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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import io.pm.finlight.AccountWithBalance
import io.pm.finlight.BankLogoHelper
import io.pm.finlight.ui.theme.AuroraPrimary
import io.pm.finlight.ui.theme.GlassPanelBorder
import io.pm.finlight.ui.theme.GlassPanelFill
import io.pm.finlight.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

/**
 * A reusable composable that creates a "glassmorphism" effect panel.
 *
 * @param modifier The modifier to be applied to the panel.
 * @param content The content to be placed inside the panel.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(GlassPanelFill)
            .border(1.dp, GlassPanelBorder, RoundedCornerShape(24.dp)),
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
fun AuroraMonthlyBudgetCard(
    totalBudget: Float,
    amountSpent: Float,
    navController: NavController
) {
    var targetAmount by remember { mutableStateOf(0f) }
    val animatedRemainingAmount by animateFloatAsState(
        targetValue = targetAmount,
        animationSpec = tween(durationMillis = 1500, easing = EaseOutCubic),
        label = "RemainingAmountAnimation"
    )

    val progress = if (totalBudget > 0) (amountSpent / totalBudget) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1500),
        label = "BudgetProgressAnimation"
    )

    LaunchedEffect(totalBudget, amountSpent) {
        targetAmount = totalBudget - amountSpent
    }

    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("budget_screen") }
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Monthly Budget",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column {
                Text(
                    text = "Remaining",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(animatedRemainingAmount.toInt())}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column {
                AuroraProgressBar(progress = animatedProgress)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Spent: ₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(amountSpent.toInt())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "Total: ₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(totalBudget.toInt())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * A custom styled LinearProgressIndicator with a gradient progress and decorative track.
 *
 * @param progress The progress to display, from 0.0 to 1.0.
 */
@Composable
private fun AuroraProgressBar(progress: Float) {
    val barHeight = 16.dp
    val trackColor = Color.White.copy(alpha = 0.2f)
    val progressBrush = Brush.horizontalGradient(
        colors = listOf(
            AuroraPrimary.copy(alpha = 0.6f),
            AuroraPrimary
        )
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight)
    ) {
        // Draw the track
        drawLine(
            color = trackColor,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = barHeight.toPx(),
            cap = StrokeCap.Round
        )

        // Draw the progress
        if (progress > 0) {
            drawLine(
                brush = progressBrush,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width * progress, size.height / 2),
                strokeWidth = barHeight.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Draw decorative milestone markers on the track
        val milestoneCount = 10
        for (i in 1 until milestoneCount) {
            val x = size.width * (i.toFloat() / milestoneCount)
            drawLine(
                color = Color.Black.copy(alpha = 0.2f),
                start = Offset(x, size.height * 0.25f),
                end = Offset(x, size.height * 0.75f),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

/**
 * A small card for displaying a single, quick-glance statistic on the dashboard.
 * It uses the GlassPanel for its background and features an animated number count-up.
 *
 * @param label The text label for the statistic.
 * @param amount The numerical value of the statistic.
 * @param modifier Modifier for this composable.
 * @param isPerDay If true, appends "/day" to the amount.
 * @param onClick A lambda to be invoked when the card is clicked.
 */
@Composable
fun AuroraStatCard(
    label: String,
    amount: Float,
    modifier: Modifier = Modifier,
    isPerDay: Boolean = false,
    onClick: () -> Unit = {}
) {
    var targetAmount by remember { mutableStateOf(0f) }
    val animatedAmount by animateFloatAsState(
        targetValue = targetAmount,
        animationSpec = tween(durationMillis = 1500, easing = EaseOutCubic),
        label = "StatCardAmountAnimation"
    )

    LaunchedEffect(amount) {
        targetAmount = amount
    }

    GlassPanel(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Start
            )
            Text(
                text = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(animatedAmount.toInt())}${if (isPerDay) "/day" else ""}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurface
            )
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
                    color = TextSecondary
                )
            }
        }
    }
}
