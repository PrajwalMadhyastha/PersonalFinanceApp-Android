package com.example.personalfinanceapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionRepository: TransactionRepository
    private val accountRepository: AccountRepository
    private val budgetDao: BudgetDao // Direct DAO access for budget logic

    // --- Flows for Dashboard UI ---
    val netWorth: Flow<Double>
    val monthlyIncome: Flow<Double>
    val monthlyExpenses: Flow<Double>
    val recentTransactions: Flow<List<TransactionDetails>>
    val budgetStatus: Flow<List<BudgetWithSpending>>

    init {
        val db = AppDatabase.getInstance(application)
        transactionRepository = TransactionRepository(db.transactionDao())
        accountRepository = AccountRepository(db.accountDao())
        budgetDao = db.budgetDao() // Initialize budget DAO

        // --- Correct Date Range Calculation ---
        val calendar = Calendar.getInstance()

        // Start of the current month
        val monthStart = calendar.apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // End of the current month
        val monthEnd = calendar.apply {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        // --- Calculate Monthly Summary ---
        val transactionsThisMonth = transactionRepository.getTransactionDetailsForRange(monthStart, monthEnd)

        monthlyIncome = transactionsThisMonth.map { transactions ->
            transactions
                .filter { it.transaction.transactionType == "income" }
                .sumOf { it.transaction.amount }
        }

        monthlyExpenses = transactionsThisMonth.map { transactions ->
            transactions
                .filter { it.transaction.transactionType == "expense" }
                .sumOf { it.transaction.amount }
        }

        // --- Calculate Net Worth ---
        netWorth = accountRepository.accountsWithBalance.map { list ->
            list.sumOf { it.balance }
        }

        // --- Get Recent Transactions ---
        recentTransactions = transactionRepository.allTransactions.map { it.take(5) }

        // --- Calculate Budget Status ---
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
        }
    }
}
