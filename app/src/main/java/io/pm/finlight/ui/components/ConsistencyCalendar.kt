// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/components/ConsistencyCalendar.kt
// REASON: FIX - The logic for generating the list of months has been corrected.
// Instead of creating a rolling 12-month view, it now correctly generates a
// list of all 12 months for the current calendar year (Jan-Dec), restoring the
// intended behavior. The initial scroll position is also fixed.
// =================================================================================
package io.pm.finlight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.pm.finlight.CalendarDayStatus
import io.pm.finlight.ConsistencyStats
import io.pm.finlight.SpendingStatus
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

private val DAY_SIZE = 14.dp
private val DAY_SPACING = 3.dp

@Composable
fun MonthlyConsistencyCalendarCard(
    data: List<CalendarDayStatus>,
    stats: ConsistencyStats,
    navController: NavController,
    onDayClick: (Date) -> Unit
) {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("reports_screen") }
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Spending Consistency",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "View Full Report",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "View Full Report",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (data.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val currentMonthData = data.filter {
                    val cal = Calendar.getInstance()
                    val currentMonth = cal.get(Calendar.MONTH)
                    cal.time = it.date
                    cal.get(Calendar.MONTH) == currentMonth
                }
                MonthColumn(
                    monthData = MonthData.fromCalendar(Calendar.getInstance()),
                    year = Calendar.getInstance().get(Calendar.YEAR),
                    today = Calendar.getInstance(),
                    dataMap = currentMonthData.associateByDate(),
                    onDayClick = onDayClick
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(stats.noSpendDays, "No Spend")
                StatItem(stats.goodDays, "Good Days")
                StatItem(stats.badDays, "Over Budget")
            }
        }
    }
}

@Composable
private fun StatItem(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsistencyCalendar(
    data: List<CalendarDayStatus>,
    modifier: Modifier = Modifier,
    onDayClick: (Date) -> Unit
) {
    if (data.isEmpty()) return

    val dataMap = remember(data) { data.associateByDate() }

    val today = remember { Calendar.getInstance() }
    val year = today.get(Calendar.YEAR)

    // --- FIX: Generate months for the current calendar year ---
    val months = (0..11).map { monthIndex ->
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndex)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        MonthData.fromCalendar(cal)
    }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // --- FIX: Correctly calculate the initial scroll index ---
    LaunchedEffect(key1 = data) {
        if (data.isNotEmpty()) {
            val currentMonthIndex = today.get(Calendar.MONTH)
            val scrollIndex = (currentMonthIndex - 2).coerceAtLeast(0)
            coroutineScope.launch {
                lazyListState.animateScrollToItem(scrollIndex)
            }
        }
    }

    LazyRow(
        state = lazyListState,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DAY_SPACING * 2)
    ) {
        items(months) { monthData ->
            MonthColumn(
                monthData = monthData,
                year = year,
                today = today,
                dataMap = dataMap,
                onDayClick = onDayClick
            )
        }
    }
}

private data class MonthData(
    val name: String,
    val dayCount: Int,
    val startOffset: Int,
    val monthIndex: Int
) {
    companion object {
        fun fromCalendar(cal: Calendar): MonthData {
            val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) ?: ""
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val firstDayOfWeek = cal.apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK)
            val startOffset = (firstDayOfWeek - Calendar.SUNDAY + 7) % 7
            return MonthData(monthName, daysInMonth, startOffset, cal.get(Calendar.MONTH))
        }
    }
}

@Composable
private fun MonthColumn(
    monthData: MonthData,
    year: Int,
    today: Calendar,
    dataMap: Map<Pair<Int, Int>, CalendarDayStatus>,
    onDayClick: (Date) -> Unit
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
                                set(year, monthData.monthIndex, dayOfMonth)
                            }

                            if (!currentDayCal.after(today)) {
                                val dayData = dataMap[currentDayCal.get(Calendar.DAY_OF_YEAR) to year]
                                DayCell(dayData, onClick = { onDayClick(currentDayCal.time) })
                            } else {
                                DayCell(null, onClick = {}) // Future day
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayCell(dayData: CalendarDayStatus?, onClick: () -> Unit) {
    val tooltipState = rememberTooltipState(isPersistent = true)

    val color = when (dayData?.status) {
        SpendingStatus.NO_SPEND -> Color(0xFF39D353)
        SpendingStatus.WITHIN_LIMIT -> {
            val fraction = if (dayData.safeToSpend > 0) {
                (dayData.amountSpent / dayData.safeToSpend).toFloat()
            } else {
                0f
            }
            lerp(Color(0xFFACD5F2), Color(0xFF006DAB), fraction.coerceIn(0f, 1f))
        }
        SpendingStatus.OVER_LIMIT -> {
            val fraction = if (dayData.safeToSpend > 0) {
                min((dayData.amountSpent / dayData.safeToSpend).toFloat(), 2f) - 1f
            } else {
                1f
            }
            lerp(Color(0xFFF87171), Color(0xFFB91C1C), fraction.coerceIn(0f, 1f))
        }
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    }

    val isClickable = dayData?.status != SpendingStatus.NO_DATA

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
                .clickable(enabled = isClickable, onClick = onClick)
        )
    }
}

private fun List<CalendarDayStatus>.associateByDate(): Map<Pair<Int, Int>, CalendarDayStatus> {
    return this.associateBy {
        val cal = Calendar.getInstance()
        cal.time = it.date
        cal.get(Calendar.DAY_OF_YEAR) to cal.get(Calendar.YEAR)
    }
}
