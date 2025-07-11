// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/TimePeriodReportViewModel.kt
// REASON: REFACTOR - The date range calculation for the Daily report has been
// restored to a rolling 24-hour window. This ensures that when a user clicks
// the daily report notification, they see data from the 24 hours preceding the
// notification's generation time, as intended.
// BUG FIX: The logic for generating the 7-day bar chart on the daily report
// screen has been corrected. It now correctly calculates the date for each bar
// based on the start of the 7-day period, not the selected date. This ensures
// the chart accurately reflects the spending for the week leading up to the
// report date, regardless of which day the user is viewing.
// BUG FIX: The ViewModel now correctly initializes its selectedDate with the
// initialDateMillis provided from the notification deep link, ensuring the
// report displays data for the correct 24-hour period.
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
import kotlin.math.roundToInt

class TimePeriodReportViewModelFactory(
    private val application: Application,
    private val timePeriod: TimePeriod,
    private val initialDateMillis: Long?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimePeriodReportViewModel::class.java)) {
            val db = AppDatabase.getInstance(application)
            @Suppress("UNCHECKED_CAST")
            return TimePeriodReportViewModel(db.transactionDao(), timePeriod, initialDateMillis) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class ReportInsights(
    val percentageChange: Int?,
    val topCategory: CategorySpending?
)

@OptIn(ExperimentalCoroutinesApi::class)
class TimePeriodReportViewModel(
    private val transactionDao: TransactionDao,
    private val timePeriod: TimePeriod,
    initialDateMillis: Long?
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(
        Calendar.getInstance().apply {
            // --- FIX: Use the provided initialDateMillis from the notification ---
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

    val totalIncome: StateFlow<Double> = transactionsForPeriod.map { transactions ->
        transactions
            .filter { it.transaction.transactionType == "income" && !it.transaction.isExcluded }
            .sumOf { it.transaction.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)


    val insights: StateFlow<ReportInsights?> = _selectedDate.flatMapLatest { calendar ->
        flow {
            val (currentStart, currentEnd) = getPeriodDateRange(calendar)

            val previousPeriodEndCal = (calendar.clone() as Calendar).apply {
                when (timePeriod) {
                    TimePeriod.DAILY -> add(Calendar.HOUR_OF_DAY, -24) // Compare with previous 24-hour window
                    TimePeriod.WEEKLY -> add(Calendar.DAY_OF_YEAR, -7)
                    TimePeriod.MONTHLY -> add(Calendar.DAY_OF_YEAR, -30)
                }
            }
            val (previousStart, previousEnd) = getPeriodDateRange(previousPeriodEndCal)

            val currentSummary = transactionDao.getFinancialSummaryForRange(currentStart, currentEnd)
            val previousSummary = transactionDao.getFinancialSummaryForRange(previousStart, previousEnd)
            val topCategories = transactionDao.getTopSpendingCategoriesForRange(currentStart, currentEnd)

            val percentageChange = if (previousSummary?.totalExpenses != null && previousSummary.totalExpenses > 0) {
                val currentExpenses = currentSummary?.totalExpenses ?: 0.0
                ((currentExpenses - previousSummary.totalExpenses) / previousSummary.totalExpenses * 100).roundToInt()
            } else {
                null
            }

            emit(ReportInsights(percentageChange, topCategories.firstOrNull()))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    val chartData: StateFlow<Pair<BarData, List<String>>?> = _selectedDate.flatMapLatest { calendar ->
        when (timePeriod) {
            TimePeriod.DAILY -> {
                val endCal = (calendar.clone() as Calendar)
                val startCal = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -6) }

                transactionDao.getDailySpendingForDateRange(startCal.timeInMillis, endCal.timeInMillis).map { dailyTotals ->
                    if (dailyTotals.isEmpty()) return@map null

                    val entries = mutableListOf<BarEntry>()
                    val labels = mutableListOf<String>()
                    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                    val fullDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                    val totalsMap = dailyTotals.associateBy { it.date }

                    for (i in 0..6) {
                        val dayCal = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
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

    private fun getPeriodDateRange(calendar: Calendar): Pair<Long, Long> {
        val endCal = (calendar.clone() as Calendar)
        val startCal = (endCal.clone() as Calendar)

        when (timePeriod) {
            TimePeriod.DAILY -> startCal.add(Calendar.HOUR_OF_DAY, -24)
            TimePeriod.WEEKLY -> startCal.add(Calendar.DAY_OF_YEAR, -7)
            TimePeriod.MONTHLY -> startCal.add(Calendar.DAY_OF_YEAR, -30)
        }

        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }
}
