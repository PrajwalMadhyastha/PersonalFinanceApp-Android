// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/DailyReportViewModel.kt
// REASON: NEW FILE - This ViewModel manages the state and data fetching for the
// new Daily Report screen. It handles the currently selected date and exposes
// flows for daily transactions and weekly spending data for the chart.
// =================================================================================
package io.pm.finlight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class DailyReportViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionDao = AppDatabase.getInstance(application).transactionDao()

    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate: StateFlow<Calendar> = _selectedDate.asStateFlow()

    val dailyTransactions: StateFlow<List<TransactionDetails>> = _selectedDate.flatMapLatest { calendar ->
        val startOfDay = (calendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfDay = (calendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        transactionDao.getTransactionsForDay(startOfDay, endOfDay)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyBarChartData: StateFlow<Pair<BarData, List<String>>?> = _selectedDate.flatMapLatest {
        transactionDao.getDailySpendingForLastSevenDays(it.timeInMillis)
    }.map { dailyTotals ->
        if (dailyTotals.isEmpty()) return@map null

        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

        dailyTotals.forEachIndexed { index, dailyTotal ->
            entries.add(BarEntry(index.toFloat(), dailyTotal.totalAmount.toFloat()))
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dailyTotal.date)
            labels.add(dayFormat.format(date ?: Date()))
        }

        val dataSet = BarDataSet(entries, "Daily Spending").apply {
            color = 0xFF81D4FA.toInt() // Light Blue
            setDrawValues(false)
        }

        Pair(BarData(dataSet), labels)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    fun selectPreviousDay() {
        _selectedDate.update {
            (it.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        }
    }

    fun selectNextDay() {
        _selectedDate.update {
            (it.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        }
    }
}
