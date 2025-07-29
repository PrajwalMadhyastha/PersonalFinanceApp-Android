// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/DashboardViewModel.kt
// REASON: FEATURE - Re-added the dynamic "Budget Health Summary". This logic
// calculates if the user is on track with their spending for the month and
// provides a context-aware summary string to the UI, making the dashboard
// hero card more informative.
// =================================================================================
package io.pm.finlight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ConsistencyStats(val goodDays: Int, val badDays: Int, val noSpendDays: Int, val noDataDays: Int)

class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val budgetDao: BudgetDao,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val userName: StateFlow<String>
    val profilePictureUri: StateFlow<String?>

    val netWorth: StateFlow<Double>
    val monthlyIncome: StateFlow<Double>
    val monthlyExpenses: StateFlow<Double>
    val recentTransactions: StateFlow<List<TransactionDetails>>
    val budgetStatus: StateFlow<List<BudgetWithSpending>>
    val overallMonthlyBudget: StateFlow<Float>
    val amountRemaining: StateFlow<Float>
    val safeToSpendPerDay: StateFlow<Float>
    val accountsSummary: StateFlow<List<AccountWithBalance>>
    val monthYear: String

    val visibleCards: StateFlow<List<DashboardCardType>>

    private val _isCustomizationMode = MutableStateFlow(false)
    val isCustomizationMode: StateFlow<Boolean> = _isCustomizationMode.asStateFlow()

    private val _cardOrder = MutableStateFlow<List<DashboardCardType>>(emptyList())
    private val _visibleCardsSet = MutableStateFlow<Set<DashboardCardType>>(emptySet())

    val hiddenCards: StateFlow<List<DashboardCardType>>

    private val _showAddCardSheet = MutableStateFlow(false)
    val showAddCardSheet: StateFlow<Boolean> = _showAddCardSheet.asStateFlow()

    val yearlyConsistencyData: StateFlow<List<CalendarDayStatus>>

    // --- NEW: StateFlow for the dynamic budget summary ---
    val budgetHealthSummary: StateFlow<String>

    init {
        userName = settingsRepository.getUserName()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = "User"
            )

        profilePictureUri = settingsRepository.getProfilePictureUri()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

        viewModelScope.launch {
            settingsRepository.getDashboardCardOrder().collect {
                if (it.contains(DashboardCardType.SPENDING_CONSISTENCY)) {
                    _cardOrder.value = it
                } else {
                    _cardOrder.value = it + DashboardCardType.SPENDING_CONSISTENCY
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.getDashboardVisibleCards().collect {
                if (it.contains(DashboardCardType.SPENDING_CONSISTENCY)) {
                    _visibleCardsSet.value = it
                } else {
                    _visibleCardsSet.value = it + DashboardCardType.SPENDING_CONSISTENCY
                }
            }
        }

        visibleCards = combine(
            _cardOrder,
            _visibleCardsSet
        ) { order, visible ->
            order.filter { it in visible }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        hiddenCards = combine(
            _cardOrder,
            _visibleCardsSet
        ) { order, visible ->
            DashboardCardType.entries.filterNot { it in visible }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


        val calendar = Calendar.getInstance()
        monthYear = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)

        val monthStart =
            (calendar.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        val monthEnd =
            (calendar.clone() as Calendar).apply {
                add(Calendar.MONTH, 1)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.DAY_OF_MONTH, -1)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

        val financialSummaryFlow = transactionRepository.getFinancialSummaryForRangeFlow(monthStart, monthEnd)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        monthlyIncome = financialSummaryFlow.map { it?.totalIncome ?: 0.0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        monthlyExpenses = financialSummaryFlow.map { it?.totalExpenses ?: 0.0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1

        overallMonthlyBudget =
            settingsRepository.getOverallBudgetForMonth(currentYear, currentMonth)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

        amountRemaining =
            combine(overallMonthlyBudget, monthlyExpenses) { budget, expenses ->
                budget - expenses.toFloat()
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

        safeToSpendPerDay =
            amountRemaining.map { remaining ->
                val today = Calendar.getInstance()
                val lastDayOfMonth = today.getActualMaximum(Calendar.DAY_OF_MONTH)
                val remainingDays = (lastDayOfMonth - today.get(Calendar.DAY_OF_MONTH) + 1).coerceAtLeast(1)

                if (remaining > 0) remaining / remainingDays else 0f
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

        // --- NEW: Logic for the dynamic budget health summary ---
        budgetHealthSummary = combine(
            monthlyExpenses,
            overallMonthlyBudget
        ) { expenses, budget ->
            if (budget <= 0f) {
                "Set a budget to see insights"
            } else {
                val cal = Calendar.getInstance()
                val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

                val percentOfMonthPassed = dayOfMonth.toFloat() / daysInMonth.toFloat()
                val percentOfBudgetSpent = (expenses / budget).toFloat()

                when {
                    percentOfBudgetSpent > 1 -> "Budget Exceeded"
                    percentOfBudgetSpent > percentOfMonthPassed + 0.2 -> "Spending High"
                    percentOfBudgetSpent < percentOfMonthPassed - 0.2 -> "Spending Low"
                    else -> "You're On Track"
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Monthly Budget")


        netWorth =
            accountRepository.accountsWithBalance.map { list ->
                list.sumOf { it.balance }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        recentTransactions =
            transactionRepository.recentTransactions
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val yearMonthString = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
        budgetStatus = budgetDao.getBudgetsWithSpendingForMonth(yearMonthString, currentMonth, currentYear)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        accountsSummary =
            accountRepository.accountsWithBalance
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )

        yearlyConsistencyData = flow {
            emit(generateYearlyConsistencyData())
        }.flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    private suspend fun generateYearlyConsistencyData(): List<CalendarDayStatus> = withContext(Dispatchers.IO) {
        val today = Calendar.getInstance()
        val year = today.get(Calendar.YEAR)
        val currentMonthIndex = today.get(Calendar.MONTH)
        val daysSoFar = today.get(Calendar.DAY_OF_YEAR)

        var totalBudgetSoFar = 0f
        for (month in 0..currentMonthIndex) {
            totalBudgetSoFar += settingsRepository.getOverallBudgetForMonthBlocking(year, month + 1)
        }
        val yearlySafeToSpend = if (totalBudgetSoFar > 0 && daysSoFar > 0) (totalBudgetSoFar / daysSoFar).toDouble() else 0.0

        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        calendar.set(Calendar.DAY_OF_YEAR, 1)
        val startDate = calendar.timeInMillis

        val firstTransactionDate = transactionRepository.getFirstTransactionDate().first()
        val firstDataCal = firstTransactionDate?.let { Calendar.getInstance().apply { timeInMillis = it } }

        val dailyTotals = transactionRepository.getDailySpendingForDateRange(startDate, endDate).first()
        val spendingMap = dailyTotals.associateBy({ it.date }, { it.totalAmount })

        val resultList = mutableListOf<CalendarDayStatus>()
        val dayIterator = Calendar.getInstance().apply { timeInMillis = startDate }

        while (!dayIterator.after(today)) {
            if (firstDataCal != null && dayIterator.before(firstDataCal)) {
                resultList.add(CalendarDayStatus(dayIterator.time, SpendingStatus.NO_DATA, 0.0, 0.0))
                dayIterator.add(Calendar.DAY_OF_YEAR, 1)
                continue
            }

            val dateKey = String.format(Locale.ROOT, "%d-%02d-%02d", dayIterator.get(Calendar.YEAR), dayIterator.get(Calendar.MONTH) + 1, dayIterator.get(Calendar.DAY_OF_MONTH))
            val amountSpent = spendingMap[dateKey] ?: 0.0

            val status = when {
                amountSpent == 0.0 -> SpendingStatus.NO_SPEND
                yearlySafeToSpend > 0 && amountSpent > yearlySafeToSpend -> SpendingStatus.OVER_LIMIT
                else -> SpendingStatus.WITHIN_LIMIT
            }

            resultList.add(CalendarDayStatus(dayIterator.time, status, amountSpent, yearlySafeToSpend))
            dayIterator.add(Calendar.DAY_OF_YEAR, 1)
        }
        resultList
    }


    fun enterCustomizationMode() {
        _isCustomizationMode.value = true
    }

    fun exitCustomizationModeAndSave() {
        viewModelScope.launch {
            settingsRepository.saveDashboardLayout(_cardOrder.value, _visibleCardsSet.value)
            _isCustomizationMode.value = false
        }
    }

    fun updateCardOrder(from: Int, to: Int) {
        _cardOrder.update { currentList ->
            currentList.toMutableList().apply {
                add(to, removeAt(from))
            }
        }
    }

    fun hideCard(cardType: DashboardCardType) {
        _visibleCardsSet.update { it - cardType }
    }

    fun showCard(cardType: DashboardCardType) {
        _visibleCardsSet.update { it + cardType }
    }

    fun onAddCardClick() {
        _showAddCardSheet.value = true
    }

    fun onAddCardSheetDismiss() {
        _showAddCardSheet.value = false
    }
}
