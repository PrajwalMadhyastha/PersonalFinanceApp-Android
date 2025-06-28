package io.pm.finlight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * ViewModel for the Dashboard screen.
 */
class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val budgetDao: BudgetDao,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val userName: StateFlow<String>
    val netWorth: StateFlow<Double>
    val monthlyIncome: StateFlow<Double>
    val monthlyExpenses: StateFlow<Double>
    val recentTransactions: StateFlow<List<TransactionDetails>>
    val budgetStatus: StateFlow<List<BudgetWithSpending>>
    val overallMonthlyBudget: StateFlow<Float>
    val amountRemaining: StateFlow<Float>
    val safeToSpendPerDay: StateFlow<Float>
    val accountsSummary: StateFlow<List<AccountWithBalance>>

    init {
        userName = settingsRepository.getUserName()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = "User"
            )

        val calendar = Calendar.getInstance()
        val monthStart =
            calendar.apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        val monthEnd =
            calendar.apply {
                add(Calendar.MONTH, 1)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.DAY_OF_MONTH, -1)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

        val transactionsThisMonth = transactionRepository.getTransactionDetailsForRange(monthStart, monthEnd)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        monthlyIncome =
            transactionsThisMonth.map { transactions ->
                transactions
                    .filter { it.transaction.transactionType == "income" }
                    .sumOf { it.transaction.amount }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        monthlyExpenses =
            transactionsThisMonth.map { transactions ->
                transactions
                    .filter { it.transaction.transactionType == "expense" }
                    .sumOf { it.transaction.amount }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        overallMonthlyBudget =
            settingsRepository.getOverallBudgetForCurrentMonth()
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

        // --- FINAL PERFORMANCE FIX: Use the new, efficient, limited query ---
        recentTransactions =
            transactionRepository.recentTransactions
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)
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
}
