// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/DashboardViewModel.kt
// REASON: FIX - Removed the unused import for `java.util.Collections` to resolve
// the "KotlinUnusedImport" warning.
// =================================================================================
package io.pm.finlight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
    // --- NEW: Expose the current month's name ---
    val monthYear: String

    val visibleCards: StateFlow<List<DashboardCardType>>

    private val _isCustomizationMode = MutableStateFlow(false)
    val isCustomizationMode: StateFlow<Boolean> = _isCustomizationMode.asStateFlow()

    private val _cardOrder = MutableStateFlow<List<DashboardCardType>>(emptyList())
    private val _visibleCardsSet = MutableStateFlow<Set<DashboardCardType>>(emptySet())

    // --- NEW: Expose hidden cards for the "Add Card" sheet ---
    val hiddenCards: StateFlow<List<DashboardCardType>>

    // --- NEW: State to control the "Add Card" bottom sheet ---
    private val _showAddCardSheet = MutableStateFlow(false)
    val showAddCardSheet: StateFlow<Boolean> = _showAddCardSheet.asStateFlow()


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
                _cardOrder.value = it
            }
        }
        viewModelScope.launch {
            settingsRepository.getDashboardVisibleCards().collect {
                _visibleCardsSet.value = it
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
            order.filterNot { it in visible }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


        val calendar = Calendar.getInstance()
        // --- NEW: Get the full month name ---
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
    }

    fun enterCustomizationMode() {
        _isCustomizationMode.value = true
    }

    fun exitCustomizationModeAndSave() {
        viewModelScope.launch {
            // --- UPDATED: Save both order and visibility ---
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

    // --- NEW: Functions to manage card visibility ---
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
