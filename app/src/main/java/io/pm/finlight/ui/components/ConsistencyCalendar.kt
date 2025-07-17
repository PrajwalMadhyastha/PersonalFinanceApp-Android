package io.pm.finlight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.pm.finlight.CalendarDayStatus
import io.pm.finlight.SpendingStatus
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

private val DAY_SIZE = 16.dp
private val DAY_SPACING = 4.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsistencyCalendar(
    data: List<CalendarDayStatus>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val dataMap = remember(data) {
        data.associateBy {
            val cal = Calendar.getInstance()
            cal.time = it.date
            cal.get(Calendar.DAY_OF_YEAR) to cal.get(Calendar.YEAR)
        }
    }

    val today = remember { Calendar.getInstance() }
    val year = today.get(Calendar.YEAR)

    // Group days by month for proper layout
    val months = (0..11).map { monthIndex ->
        val monthCal = Calendar.getInstance().apply { set(year, monthIndex, 1) }
        val monthName = monthCal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) ?: ""
        val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
        val startOffset = (firstDayOfWeek - Calendar.SUNDAY + 7) % 7

        MonthData(monthName, daysInMonth, startOffset)
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DAY_SPACING * 2) // Space between months
    ) {
        items(months) { monthData ->
            MonthColumn(
                monthData = monthData,
                year = year,
                today = today,
                dataMap = dataMap
            )
        }
    }
}

private data class MonthData(
    val name: String,
    val dayCount: Int,
    val startOffset: Int
)

@Composable
private fun MonthColumn(
    monthData: MonthData,
    year: Int,
    today: Calendar,
    dataMap: Map<Pair<Int, Int>, CalendarDayStatus>
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = monthData.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        val totalCells = monthData.startOffset + monthData.dayCount
        val weekCount = (totalCells + 6) / 7

        Row(horizontalArrangement = Arrangement.spacedBy(DAY_SPACING)) {
            for (week in 0 until weekCount) {
                Column(verticalArrangement = Arrangement.spacedBy(DAY_SPACING)) {
                    for (day in 0..6) {
                        val cellIndex = week * 7 + day
                        if (cellIndex >= monthData.startOffset && cellIndex < totalCells) {
                            val dayOfMonth = cellIndex - monthData.startOffset + 1
                            val currentDayCal = Calendar.getInstance().apply {
                                set(year, monthData.name.toMonthIndex(), dayOfMonth)
                            }

                            if (!currentDayCal.after(today)) {
                                val dayData = dataMap[currentDayCal.get(Calendar.DAY_OF_YEAR) to year]
                                DayCell(dayData)
                            } else {
                                DayCell(null) // Future day
                            }
                        } else {
                            // Empty cell for padding
                            Spacer(modifier = Modifier.size(DAY_SIZE))
                        }
                    }
                }
            }
        }
    }
}

private fun String.toMonthIndex(): Int {
    val cal = Calendar.getInstance()
    val months = (0..11).map {
        cal.set(Calendar.MONTH, it)
        cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())
    }
    return months.indexOf(this)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayCell(dayData: CalendarDayStatus?) {
    val tooltipState = rememberTooltipState(isPersistent = true)

    // --- UPDATED: Dynamic color logic based on spending magnitude ---
    val color = when (dayData?.status) {
        SpendingStatus.NO_SPEND -> Color(0xFF39D353) // A distinct, bright green for zero spending

        SpendingStatus.WITHIN_LIMIT -> {
            val fraction = if (dayData.safeToSpend > 0) {
                (dayData.amountSpent / dayData.safeToSpend).toFloat()
            } else {
                0f
            }
            // Interpolate from a light green to a darker green
            lerp(Color(0xFFACD5F2), Color(0xFF006DAB), fraction.coerceIn(0f, 1f))
        }

        SpendingStatus.OVER_LIMIT -> {
            val fraction = if (dayData.safeToSpend > 0) {
                // How much over budget are we? Cap at 2x for visual consistency.
                min((dayData.amountSpent / dayData.safeToSpend).toFloat(), 2f) - 1f
            } else {
                1f // Max intensity if budget was zero
            }
            // Interpolate from a light red to a brighter red
            lerp(Color(0xFFF87171), Color(0xFFB91C1C), fraction.coerceIn(0f, 1f))
        }

        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) // Default for no data
    }


    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            if (dayData != null) {
                val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
                val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
                PlainTooltip {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = dateFormat.format(dayData.date),
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = "Spent: ${currencyFormat.format(dayData.amountSpent)}")
                        if (dayData.safeToSpend > 0) {
                            Text(text = "Budget: ${currencyFormat.format(dayData.safeToSpend)}")
                        }
                    }
                }
            }
        },
        state = tooltipState
    ) {
        Box(
            modifier = Modifier
                .size(DAY_SIZE)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
                .border(0.5.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                .clickable { /* Could be used for long-press tooltips if needed */ }
        )
    }
}
