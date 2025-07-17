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

// --- NEW: Data class to hold the calculated stats for the calendar ---
data class ConsistencyStats(val goodDays: Int, val badDays: Int, val noSpendDays: Int)

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

    val monthlyConsistencyData: StateFlow<List<CalendarDayStatus>>
    // --- NEW: StateFlow for the new consistency stats ---
    val consistencyStats: StateFlow<ConsistencyStats>

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

        val transactionsThisMonth = transactionRepository.getTransactionDetailsForRange(
            startDate = monthStart,
            endDate = monthEnd,
            keyword = null,
            accountId = null,
            categoryId = null
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        monthlyIncome =
            transactionsThisMonth.map { transactions ->
                transactions
                    .filter { it.transaction.transactionType == "income" && !it.transaction.isExcluded }
                    .sumOf { it.transaction.amount }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        monthlyExpenses =
            transactionsThisMonth.map { transactions ->
                transactions
                    .filter { it.transaction.transactionType == "expense" && !it.transaction.isExcluded }
                    .sumOf { it.transaction.amount }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

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

        monthlyConsistencyData = flow {
            emit(generateCurrentMonthConsistencyData())
        }.flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        // --- NEW: Derive stats from the consistency data ---
        consistencyStats = monthlyConsistencyData.map { data ->
            val goodDays = data.count { it.status == SpendingStatus.WITHIN_LIMIT }
            val badDays = data.count { it.status == SpendingStatus.OVER_LIMIT }
            val noSpendDays = data.count { it.status == SpendingStatus.NO_SPEND }
            ConsistencyStats(goodDays, badDays, noSpendDays)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConsistencyStats(0, 0, 0)
        )
    }

    private suspend fun generateCurrentMonthConsistencyData(): List<CalendarDayStatus> = withContext(Dispatchers.IO) {
        val today = Calendar.getInstance()
        val year = today.get(Calendar.YEAR)
        val month = today.get(Calendar.MONTH) + 1

        val monthStartCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
        }
        val monthEndCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
        }

        val firstTransactionDate = transactionRepository.getFirstTransactionDate().first()
        val firstDataCal = firstTransactionDate?.let { Calendar.getInstance().apply { timeInMillis = it } }

        val dailyTotals = transactionRepository.getDailySpendingForDateRange(monthStartCal.timeInMillis, monthEndCal.timeInMillis).first()
        val spendingMap = dailyTotals.associateBy({ it.date }, { it.totalAmount })

        val daysInMonth = today.getActualMaximum(Calendar.DAY_OF_MONTH)
        val budget = settingsRepository.getOverallBudgetForMonthBlocking(year, month)
        val safeToSpend = if (budget > 0) (budget.toDouble() / daysInMonth) else 0.0

        val resultList = mutableListOf<CalendarDayStatus>()
        val dayIterator = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }

        for (i in 1..daysInMonth) {
            dayIterator.set(Calendar.DAY_OF_MONTH, i)
            val dateKey = String.format(Locale.ROOT, "%d-%02d-%02d", year, month, i)

            if (firstDataCal != null && dayIterator.before(firstDataCal)) {
                resultList.add(CalendarDayStatus(dayIterator.time, SpendingStatus.NO_DATA, 0.0, 0.0))
                continue
            }

            val amountSpent = spendingMap[dateKey] ?: 0.0
            val status = when {
                dayIterator.after(today) -> SpendingStatus.NO_DATA
                amountSpent == 0.0 -> SpendingStatus.NO_SPEND
                safeToSpend > 0 && amountSpent > safeToSpend -> SpendingStatus.OVER_LIMIT
                else -> SpendingStatus.WITHIN_LIMIT
            }
            resultList.add(CalendarDayStatus(dayIterator.time, status, amountSpent, safeToSpend))
        }
        return@withContext resultList
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
