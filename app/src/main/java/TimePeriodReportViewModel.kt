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
    private val timePeriod: TimePeriod
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimePeriodReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimePeriodReportViewModel(application, timePeriod) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TimePeriodReportViewModel(
    application: Application,
    private val timePeriod: TimePeriod
) : androidx.lifecycle.AndroidViewModel(application) {

    private val transactionDao = AppDatabase.getInstance(application).transactionDao()

    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
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

                    // Create a map of date strings to total amounts
                    val totalsMap = dailyTotals.associateBy { it.date }

                    // Iterate through the last 7 days to ensure all days are present
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
            TimePeriod.WEEKLY, TimePeriod.MONTHLY -> {
                // Placeholder for future implementation
                flowOf(null)
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

    private fun getPeriodDateRange(calendar: Calendar): Pair<Long, Long> {
        val startCal = calendar.clone() as Calendar
        val endCal = calendar.clone() as Calendar

        when (timePeriod) {
            TimePeriod.DAILY -> {
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
            }
            TimePeriod.WEEKLY -> {
                startCal.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                endCal.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                endCal.add(Calendar.WEEK_OF_YEAR, 1)
                endCal.add(Calendar.DAY_OF_YEAR, -1)
            }
            TimePeriod.MONTHLY -> {
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                endCal.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            }
        }
        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }
}