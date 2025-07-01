// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/IncomeViewModel.kt
// REASON: NEW FILE - This ViewModel is responsible for fetching and preparing
// all data for the new dedicated Income screen.
// UPDATE: Added monthlySummaries StateFlow to power the new month selector UI.
// =================================================================================
package io.pm.finlight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class IncomeViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionRepository: TransactionRepository

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance())
    val selectedMonth: StateFlow<Calendar> = _selectedMonth.asStateFlow()

    // --- NEW: Flow that provides a list of past months with their total income ---
    val monthlySummaries: StateFlow<List<MonthlySummaryItem>>

    val incomeTransactionsForSelectedMonth: StateFlow<List<TransactionDetails>> = _selectedMonth.flatMapLatest { calendar ->
        val monthStart = (calendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        val monthEnd = (calendar.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        transactionRepository.getIncomeTransactionsForRange(monthStart, monthEnd)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIncomeForSelectedMonth: StateFlow<Double> = incomeTransactionsForSelectedMonth.map { transactions ->
        transactions.sumOf { it.transaction.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val incomeByCategoryForSelectedMonth: StateFlow<List<CategorySpending>> = _selectedMonth.flatMapLatest { calendar ->
        val monthStart = (calendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        val monthEnd = (calendar.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        transactionRepository.getIncomeByCategoryForMonth(monthStart, monthEnd)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    init {
        val db = AppDatabase.getInstance(application)
        transactionRepository = TransactionRepository(db.transactionDao())

        // --- NEW: Calculate monthly summaries for income ---
        val twelveMonthsAgo = Calendar.getInstance().apply { add(Calendar.YEAR, -1) }.timeInMillis
        monthlySummaries = transactionRepository.getMonthlyTrends(twelveMonthsAgo)
            .map { trends ->
                val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val monthMap = trends.associate {
                    val cal = Calendar.getInstance().apply {
                        time = dateFormat.parse(it.monthYear) ?: Date()
                    }
                    (cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)) to it.totalIncome
                }

                (0..11).map { i ->
                    val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -i) }
                    val key = cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)
                    val income = monthMap[key] ?: 0.0
                    // Re-using MonthlySummaryItem, where totalSpent will represent totalIncome
                    MonthlySummaryItem(calendar = cal, totalSpent = income)
                }.reversed()
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun setSelectedMonth(calendar: Calendar) {
        _selectedMonth.value = calendar
    }
}
