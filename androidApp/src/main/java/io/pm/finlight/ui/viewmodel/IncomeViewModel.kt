// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/IncomeViewModel.kt
// REASON: FIX - The logic for the `monthlySummaries` flow has been updated.
// Instead of showing a rolling 12-month view, it now correctly displays all
// 12 months of the current calendar year, restoring the original, preferred
// behavior for the month scroller component.
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
    val accountRepository: AccountRepository
    val categoryRepository: CategoryRepository

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance())
    val selectedMonth: StateFlow<Calendar> = _selectedMonth.asStateFlow()

    private val _filterState = MutableStateFlow(TransactionFilterState())
    val filterState: StateFlow<TransactionFilterState> = _filterState.asStateFlow()

    private val combinedState: Flow<Pair<Calendar, TransactionFilterState>> =
        _selectedMonth.combine(_filterState) { month, filters ->
            Pair(month, filters)
        }

    val allAccounts: StateFlow<List<Account>>
    val allCategories: Flow<List<Category>>

    val incomeTransactionsForSelectedMonth: StateFlow<List<TransactionDetails>> = combinedState.flatMapLatest { (calendar, filters) ->
        val monthStart = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
        val monthEnd = (calendar.clone() as Calendar).apply { add(Calendar.MONTH, 1); set(Calendar.DAY_OF_MONTH, 1); add(Calendar.DAY_OF_MONTH, -1); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
        transactionRepository.getIncomeTransactionsForRange(monthStart, monthEnd, filters.keyword.takeIf { it.isNotBlank() }, filters.account?.id, filters.category?.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIncomeForSelectedMonth: StateFlow<Double> = incomeTransactionsForSelectedMonth.map { transactions ->
        transactions.sumOf { it.transaction.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val incomeByCategoryForSelectedMonth: StateFlow<List<CategorySpending>> = combinedState.flatMapLatest { (calendar, filters) ->
        val monthStart = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
        val monthEnd = (calendar.clone() as Calendar).apply { add(Calendar.MONTH, 1); set(Calendar.DAY_OF_MONTH, 1); add(Calendar.DAY_OF_MONTH, -1); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
        transactionRepository.getIncomeByCategoryForMonth(monthStart, monthEnd, filters.keyword.takeIf { it.isNotBlank() }, filters.account?.id, filters.category?.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlySummaries: StateFlow<List<MonthlySummaryItem>>

    init {
        val db = AppDatabase.getInstance(application)
        transactionRepository = TransactionRepository(db.transactionDao())
        accountRepository = AccountRepository(db.accountDao())
        categoryRepository = CategoryRepository(db.categoryDao())

        allAccounts = accountRepository.allAccounts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        allCategories = categoryRepository.allCategories

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val startOfYear = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis

        monthlySummaries = transactionRepository.getMonthlyTrends(startOfYear)
            .map { trends ->
                val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val monthMap = trends.filter {
                    (dateFormat.parse(it.monthYear) ?: Date()).let { date ->
                        val cal = Calendar.getInstance().apply { time = date }
                        cal.get(Calendar.YEAR) == currentYear
                    }
                }.associate {
                    val cal = Calendar.getInstance().apply { time = dateFormat.parse(it.monthYear) ?: Date() }
                    (cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)) to it.totalIncome
                }

                (0..11).map { monthIndex ->
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, currentYear)
                        set(Calendar.MONTH, monthIndex)
                    }
                    val key = cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)
                    val income = monthMap[key] ?: 0.0
                    MonthlySummaryItem(calendar = cal, totalSpent = income)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun setSelectedMonth(calendar: Calendar) {
        _selectedMonth.value = calendar
    }

    fun updateFilterKeyword(keyword: String) {
        _filterState.update { it.copy(keyword = keyword) }
    }

    fun updateFilterAccount(account: Account?) {
        _filterState.update { it.copy(account = account) }
    }

    fun updateFilterCategory(category: Category?) {
        _filterState.update { it.copy(category = category) }
    }

    fun clearFilters() {
        _filterState.value = TransactionFilterState()
    }
}
