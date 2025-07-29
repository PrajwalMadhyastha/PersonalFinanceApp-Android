// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/CategorySpendingScreen.kt
// REASON: MAJOR REFACTOR - The MPAndroidChart PieChart has been completely
// replaced with a custom DonutChart composable built with the Jetpack Compose
// Canvas API. This provides full control over layout, sizing, and animation,
// resolving the issue where the chart would not expand to fill its allocated
// space. The new chart is now larger, animates smoothly, and perfectly matches
// the desired modern UI.
// =================================================================================
package io.pm.finlight.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.pm.finlight.CategoryIconHelper
import io.pm.finlight.CategorySpending
import io.pm.finlight.ui.components.GlassPanel
import kotlin.math.min

@Composable
fun CategorySpendingScreen(
    spendingList: List<CategorySpending>,
    onCategoryClick: (CategorySpending) -> Unit
) {
    if (spendingList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No spending data for this month.")
        }
        return
    }

    val totalSpending = spendingList.sumOf { it.totalAmount }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Category Breakdown",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // --- NEW: Use the custom Compose DonutChart ---
                        DonutChart(
                            modifier = Modifier.weight(2.5f),
                            data = spendingList
                        )
                        ChartLegend(
                            modifier = Modifier.weight(1f),
                            spendingList = spendingList
                        )
                    }
                }
            }
        }

        items(spendingList) { categorySpending ->
            CategorySpendingCard(
                categorySpending = categorySpending,
                totalSpending = totalSpending,
                onClick = { onCategoryClick(categorySpending) }
            )
        }
    }
}

/**
 * A custom composable that draws an animated donut chart using the Canvas API.
 */
@Composable
private fun DonutChart(
    modifier: Modifier = Modifier,
    data: List<CategorySpending>
) {
    val totalAmount = remember(data) { data.sumOf { it.totalAmount }.toFloat() }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 1000))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = 20.dp.toPx()
        val diameter = min(size.width, size.height) * 0.8f
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val size = Size(diameter, diameter)
        var startAngle = -90f

        data.forEach { item ->
            val sweepAngle = (item.totalAmount.toFloat() / totalAmount) * 360f
            val color = CategoryIconHelper.getIconBackgroundColor(item.colorKey ?: "gray_light")

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle * animationProgress.value,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                topLeft = topLeft,
                size = size
            )
            startAngle += sweepAngle
        }
    }
}


@Composable
fun CategorySpendingCard(
    categorySpending: CategorySpending,
    totalSpending: Double,
    onClick: () -> Unit
) {
    val percentage = if (totalSpending > 0) (categorySpending.totalAmount / totalSpending * 100) else 0.0

    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        CategoryIconHelper.getIconBackgroundColor(
                            categorySpending.colorKey ?: "gray_light"
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryIconHelper.getIcon(categorySpending.iconKey ?: "category"),
                    contentDescription = categorySpending.categoryName,
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    categorySpending.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${"%.1f".format(percentage)}% of total spending",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "₹${"%,.2f".format(categorySpending.totalAmount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ChartLegend(modifier: Modifier = Modifier, spendingList: List<CategorySpending>) {
    val totalValue = remember(spendingList) { spendingList.sumOf { it.totalAmount } }

    LazyColumn(
        modifier = modifier.padding(start = 16.dp),
    ) {
        items(spendingList) { item ->
            val color = CategoryIconHelper.getIconBackgroundColor(item.colorKey ?: "gray_light")
            val percentage = if (totalValue > 0) (item.totalAmount / totalValue * 100) else 0.0

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.categoryName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${"%.1f".format(percentage)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
