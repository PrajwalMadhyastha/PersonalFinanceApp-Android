// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/TimePeriodReportViewModel.kt
// REASON: BUG FIX - The ViewModel and its Factory have been moved to their own
// dedicated file to resolve the "Redeclaration" compilation errors.
// REASON: REFACTOR - The `getPeriodDateRange` function has been updated to
// calculate a rolling time window (last 24 hours, 7 days, or 30 days)
// ending at the currently selected date. This ensures the report accurately
// reflects the user's request for a floating time period rather than a fixed
// calendar day/week.
// =================================================================================
package io.pm.finlight

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class TimePeriodReportViewModelFactory(
    private val application: Application,
    private val timePeriod: TimePeriod,
    private val initialDateMillis: Long?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimePeriodReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimePeriodReportViewModel(application, timePeriod, initialDateMillis) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TimePeriodReportViewModel(
    application: Application,
    private val timePeriod: TimePeriod,
    initialDateMillis: Long?
) : androidx.lifecycle.AndroidViewModel(application) {

    private val transactionDao = AppDatabase.getInstance(application).transactionDao()

    private val _selectedDate = MutableStateFlow(
        Calendar.getInstance().apply {
            if (initialDateMillis != null && initialDateMillis != -1L) {
                timeInMillis = initialDateMillis
            }
        }
    )
    val selectedDate: StateFlow<Calendar> = _selectedDate.asStateFlow()

    val transactionsForPeriod: StateFlow<List<TransactionDetails>> = _selectedDate.flatMapLatest { calendar ->
        val (start, end) = getPeriodDateRange(calendar)
        transactionDao.getTransactionsForDateRange(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chartData: StateFlow<Pair<BarData, List<String>>?> = _selectedDate.flatMapLatest { calendar ->
        when (timePeriod) {
            TimePeriod.DAILY -> {
                val end = (calendar.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                }.timeInMillis
                val start = (calendar.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, -6)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                }.timeInMillis

                transactionDao.getDailySpendingForDateRange(start, end).map { dailyTotals ->
                    if (dailyTotals.isEmpty()) return@map null

                    val entries = mutableListOf<BarEntry>()
                    val labels = mutableListOf<String>()
                    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                    val fullDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                    val totalsMap = dailyTotals.associateBy { it.date }

                    for (i in 0..6) {
                        val dayCal = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -6 + i) }
                        val dateString = fullDateFormat.format(dayCal.time)

                        val total = totalsMap[dateString]?.totalAmount?.toFloat() ?: 0f
                        entries.add(BarEntry(i.toFloat(), total))
                        labels.add(dayFormat.format(dayCal.time))
                    }

                    val dataSet = BarDataSet(entries, "Daily Spending").apply {
                        color = 0xFF81D4FA.toInt() // Light Blue
                        setDrawValues(true)
                        valueTextColor = 0xFFFFFFFF.toInt()
                    }
                    Pair(BarData(dataSet), labels)
                }
            }
            TimePeriod.WEEKLY -> {
                val endCal = (calendar.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    add(Calendar.WEEK_OF_YEAR, 1)
                    add(Calendar.DAY_OF_YEAR, -1)
                }
                val startCal = (calendar.clone() as Calendar).apply {
                    add(Calendar.WEEK_OF_YEAR, -7) // 8 weeks total
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                }

                transactionDao.getWeeklySpendingForDateRange(startCal.timeInMillis, endCal.timeInMillis).map { weeklyTotals ->
                    if (weeklyTotals.isEmpty()) return@map null

                    val entries = mutableListOf<BarEntry>()
                    val labels = mutableListOf<String>()
                    val totalsMap = weeklyTotals.associateBy { it.period }

                    for (i in 0..7) {
                        val weekCal = (startCal.clone() as Calendar).apply { add(Calendar.WEEK_OF_YEAR, i) }
                        val yearWeek = "${weekCal.get(Calendar.YEAR)}-${weekCal.get(Calendar.WEEK_OF_YEAR).toString().padStart(2, '0')}"

                        val total = totalsMap[yearWeek]?.totalAmount?.toFloat() ?: 0f
                        entries.add(BarEntry(i.toFloat(), total))
                        labels.add("W${weekCal.get(Calendar.WEEK_OF_YEAR)}")
                    }

                    val dataSet = BarDataSet(entries, "Weekly Spending").apply {
                        color = 0xFF9575CD.toInt() // Deep Purple
                        setDrawValues(true)
                        valueTextColor = 0xFFFFFFFF.toInt()
                    }
                    Pair(BarData(dataSet), labels)
                }
            }
            TimePeriod.MONTHLY -> {
                val endCal = (calendar.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                }
                val startCal = (calendar.clone() as Calendar).apply {
                    add(Calendar.MONTH, -5) // 6 months total
                    set(Calendar.DAY_OF_MONTH, 1)
                }

                transactionDao.getMonthlySpendingForDateRange(startCal.timeInMillis, endCal.timeInMillis).map { monthlyTotals ->
                    if (monthlyTotals.isEmpty()) return@map null

                    val entries = mutableListOf<BarEntry>()
                    val labels = mutableListOf<String>()
                    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                    val yearMonthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                    val totalsMap = monthlyTotals.associateBy { it.period }

                    for (i in 0..5) {
                        val monthCal = (startCal.clone() as Calendar).apply { add(Calendar.MONTH, i) }
                        val yearMonth = yearMonthFormat.format(monthCal.time)

                        val total = totalsMap[yearMonth]?.totalAmount?.toFloat() ?: 0f
                        entries.add(BarEntry(i.toFloat(), total))
                        labels.add(monthFormat.format(monthCal.time))
                    }

                    val dataSet = BarDataSet(entries, "Monthly Spending").apply {
                        color = 0xFF4DB6AC.toInt() // Teal
                        setDrawValues(true)
                        valueTextColor = 0xFFFFFFFF.toInt()
                    }
                    Pair(BarData(dataSet), labels)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    fun selectPreviousPeriod() {
        _selectedDate.update {
            (it.clone() as Calendar).apply {
                add(
                    when (timePeriod) {
                        TimePeriod.DAILY -> Calendar.DAY_OF_YEAR
                        TimePeriod.WEEKLY -> Calendar.WEEK_OF_YEAR
                        TimePeriod.MONTHLY -> Calendar.MONTH
                    }, -1
                )
            }
        }
    }

    fun selectNextPeriod() {
        _selectedDate.update {
            (it.clone() as Calendar).apply {
                add(
                    when (timePeriod) {
                        TimePeriod.DAILY -> Calendar.DAY_OF_YEAR
                        TimePeriod.WEEKLY -> Calendar.WEEK_OF_YEAR
                        TimePeriod.MONTHLY -> Calendar.MONTH
                    }, 1
                )
            }
        }
    }

    // --- REFACTORED: This function now calculates a rolling time window ---
    private fun getPeriodDateRange(calendar: Calendar): Pair<Long, Long> {
        val endCal = (calendar.clone() as Calendar) // The end of the period is the selected date/time

        val startCal = (endCal.clone() as Calendar).apply {
            when (timePeriod) {
                TimePeriod.DAILY -> add(Calendar.HOUR_OF_DAY, -24)
                TimePeriod.WEEKLY -> add(Calendar.DAY_OF_YEAR, -7)
                TimePeriod.MONTHLY -> add(Calendar.DAY_OF_YEAR, -30)
            }
        }

        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }
}
