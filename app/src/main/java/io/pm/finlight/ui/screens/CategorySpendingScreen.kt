package io.pm.finlight.ui.screens

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import io.pm.finlight.CategoryIconHelper
import io.pm.finlight.CategorySpending
import io.pm.finlight.ui.components.GlassPanel

@Composable
fun CategorySpendingScreen(spendingList: List<CategorySpending>) {
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
    val pieData = createPieData(spendingList)
    val pieChartLabelColor = MaterialTheme.colorScheme.onSurface.toArgb()

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
                    AndroidView(
                        factory = { context ->
                            PieChart(context).apply {
                                description.isEnabled = false
                                isDrawHoleEnabled = true
                                setHoleColor(Color.TRANSPARENT)
                                setEntryLabelColor(pieChartLabelColor)
                                setEntryLabelTextSize(12f)
                                legend.isEnabled = false
                                setUsePercentValues(true)
                            }
                        },
                        update = { chart ->
                            chart.data = pieData
                            chart.invalidate()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )
                }
            }
        }

        items(spendingList) { categorySpending ->
            CategorySpendingCard(
                categorySpending = categorySpending,
                totalSpending = totalSpending
            )
        }
    }
}

@Composable
fun CategorySpendingCard(categorySpending: CategorySpending, totalSpending: Double) {
    val percentage = if (totalSpending > 0) (categorySpending.totalAmount / totalSpending * 100) else 0.0

    GlassPanel(
        modifier = Modifier.fillMaxWidth()
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
                    tint = androidx.compose.ui.graphics.Color.Black,
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

fun createPieData(spendingList: List<CategorySpending>): PieData {
    val entries = spendingList.map {
        PieEntry(it.totalAmount.toFloat(), it.categoryName)
    }
    val colors = spendingList.map {
        (CategoryIconHelper.getIconBackgroundColor(it.colorKey ?: "gray_light")).toArgb()
    }
    val dataSet = PieDataSet(entries, "Spending").apply {
        this.colors = colors
        valueFormatter = PercentFormatter()
        valueTextSize = 12f
        valueTextColor = Color.BLACK
        setDrawValues(false) // Hiding values on the chart itself for a cleaner look
    }
    return PieData(dataSet)
}
