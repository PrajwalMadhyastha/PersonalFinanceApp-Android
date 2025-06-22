package com.example.personalfinanceapp.com.example.personalfinanceapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.personalfinanceapp.PotentialTransaction
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

@Composable
fun TimePickerDialog(
    title: String = "Select Time",
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                content()
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ChartLegend(pieData: PieData?) {
    // Safely get the dataset from the PieData object.
    val dataSet = pieData?.dataSet as? PieDataSet ?: return

    // Use a classic for loop for maximum compatibility with the Java library.
    // This explicitly gets each entry and its corresponding color by index.
    Column {
        for (i in 0 until dataSet.entryCount) {
            val entry = dataSet.getEntryForIndex(i)
            val color = dataSet.getColor(i)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(color)) // Convert the Android integer color to a Compose Color
                )
                Spacer(modifier = Modifier.width(8.dp))
                // The 'label' property of PieEntry holds the category name.
                Text(text = "${entry.label} - ₹${"%.2f".format(entry.value)}")
            }
        }
    }
}

@Composable
fun GroupedBarChart(trendDataPair: Pair<BarData, List<String>>) {
    val (barData, labels) = trendDataPair

    AndroidView(
        factory = { context ->
            // FACTORY: For one-time, data-independent setup
            BarChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = true
                setDrawGridBackground(false)

                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.granularity = 1f // Essential for labels to align with bars

                axisLeft.axisMinimum = 0f
                axisLeft.setDrawGridLines(true)

                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            // UPDATE: For applying data and data-dependent properties

            // 1. Define the widths and spacing for the grouped bars
            val barWidth = 0.25f
            val barSpace = 0.05f
            val groupSpace = 0.4f
            barData.barWidth = barWidth

            // 2. Apply the data to the chart
            chart.data = barData

            // 3. Set the labels for the X-Axis
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)

            // 4. Set the visible range of the x-axis
            // This is crucial for groupBars to work correctly
            chart.xAxis.axisMinimum = 0f
            chart.xAxis.axisMaximum = labels.size.toFloat()

            // 5. Center the labels under the groups
            chart.xAxis.setCenterAxisLabels(true)

            // 6. Group the bars. The 'fromX' (first param) should be the starting point.
            chart.groupBars(0f, groupSpace, barSpace)

            // 7. Refresh the chart to apply all changes
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
    )
}