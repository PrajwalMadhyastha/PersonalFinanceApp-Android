package com.example.personalfinanceapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionRepository: TransactionRepository
    private val accountRepository: AccountRepository
    private val budgetDao: BudgetDao
    // --- NEW: Add dependency on SettingsRepository ---
    private val settingsRepository: SettingsRepository

    // --- Flows for Dashboard UI ---
    val netWorth: StateFlow<Double>
    val monthlyIncome: StateFlow<Double>
    val monthlyExpenses: StateFlow<Double>
    val recentTransactions: StateFlow<List<TransactionDetails>>
    val budgetStatus: StateFlow<List<BudgetWithSpending>>

    // --- NEW: Flows to expose budget data ---
    val overallMonthlyBudget: StateFlow<Float>
    val amountRemaining: StateFlow<Float>


    init {
        val db = AppDatabase.getInstance(application)
        transactionRepository = TransactionRepository(db.transactionDao())
        accountRepository = AccountRepository(db.accountDao())
        budgetDao = db.budgetDao()
        // --- NEW: Initialize SettingsRepository ---
        settingsRepository = SettingsRepository(application)

        // --- Correct Date Range Calculation ---
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

        // --- Calculate Monthly Summary ---
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

        // --- Get Overall Budget from SharedPreferences ---
        overallMonthlyBudget = settingsRepository.getOverallBudgetForCurrentMonth()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

        // --- NEW: Calculate Amount Remaining ---
        amountRemaining = combine(overallMonthlyBudget, monthlyExpenses) { budget, expenses ->
            budget - expenses.toFloat()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)


        // --- Calculate Net Worth ---
        netWorth = accountRepository.accountsWithBalance.map { list ->
            list.sumOf { it.balance }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        // --- Get Recent Transactions ---
        recentTransactions = transactionRepository.allTransactions.map { it.take(5) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // --- Calculate Category-Specific Budget Status ---
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
