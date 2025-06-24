package com.example.personalfinanceapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

/**
 * ViewModel for the Dashboard screen.
 * This class is now testable because its dependencies are provided via the constructor.
 */
class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val budgetDao: BudgetDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val netWorth: StateFlow<Double>
    val monthlyIncome: StateFlow<Double>
    val monthlyExpenses: StateFlow<Double>
    val recentTransactions: StateFlow<List<TransactionDetails>>
    val budgetStatus: StateFlow<List<BudgetWithSpending>>
    val overallMonthlyBudget: StateFlow<Float>
    val amountRemaining: StateFlow<Float>
    val safeToSpendPerDay: StateFlow<Float>

    init {
        val calendar = Calendar.getInstance()
        val monthStart = calendar.apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val monthEnd = calendar.apply {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val transactionsThisMonth = transactionRepository.getTransactionDetailsForRange(monthStart, monthEnd)

        monthlyIncome = transactionsThisMonth.map { transactions ->
            transactions
                .filter { it.transaction.transactionType == "income" }
                .sumOf { it.transaction.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        monthlyExpenses = transactionsThisMonth.map { transactions ->
            transactions
                .filter { it.transaction.transactionType == "expense" }
                .sumOf { it.transaction.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        overallMonthlyBudget = settingsRepository.getOverallBudgetForCurrentMonth()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

        amountRemaining = combine(overallMonthlyBudget, monthlyExpenses) { budget, expenses ->
            budget - expenses.toFloat()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

        safeToSpendPerDay = amountRemaining.map { remaining ->
            val today = Calendar.getInstance()
            val lastDayOfMonth = today.getActualMaximum(Calendar.DAY_OF_MONTH)
            val remainingDays = (lastDayOfMonth - today.get(Calendar.DAY_OF_MONTH) + 1).coerceAtLeast(1)

            if (remaining > 0) remaining / remainingDays else 0f
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)


        netWorth = accountRepository.accountsWithBalance.map { list ->
            list.sumOf { it.balance }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        recentTransactions = transactionRepository.allTransactions.map { it.take(5) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val budgets = budgetDao.getBudgetsForMonth(currentMonth, currentYear)

        budgetStatus = budgets.combine(transactionRepository.allTransactions) { budgetList, allTransactions ->
            budgetList.map { budget ->
                val spending = allTransactions
                    .filter { it.categoryName == budget.categoryName }
                    .filter { it.transaction.transactionType == "expense" }
                    .filter {
                        val cal = Calendar.getInstance().apply { timeInMillis = it.transaction.date }
                        cal.get(Calendar.MONTH) + 1 == currentMonth && cal.get(Calendar.YEAR) == currentYear
                    }
                    .sumOf { it.transaction.amount }

                BudgetWithSpending(budget = budget, spent = spending)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }
}
