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
    private val budgetRepository: BudgetRepository

    val netWorth: Flow<Double>
    val recentTransactions: Flow<List<TransactionDetails>>
    val monthlyIncome: Flow<Double>
    val monthlyExpenses: Flow<Double>
    val budgetStatus: Flow<List<BudgetWithSpending>>

    init {
        val db = AppDatabase.getInstance(application)
        transactionRepository = TransactionRepository(db.transactionDao())
        accountRepository = AccountRepository(db.accountDao())
        budgetRepository = BudgetRepository(db.budgetDao())

        netWorth = accountRepository.allAccounts.combine(
            transactionRepository.getAllTransactionsSimple()
        ) { accounts, transactions ->
            accounts.sumOf { account ->
                transactions
                    .filter { it.accountId == account.id }
                    .sumOf { it.amount }
            }
        }

        recentTransactions = transactionRepository.allTransactions.map { list ->
            list.take(5)
        }

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
        val startDate = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endDate = calendar.timeInMillis

        val transactionsThisMonth = transactionRepository.getTransactionDetailsForRange(startDate, endDate)

        monthlyIncome = transactionsThisMonth.map { transactions ->
            transactions
                .filter { it.transaction.amount > 0 }
                .sumOf { it.transaction.amount }
        }

        monthlyExpenses = transactionsThisMonth.map { transactions ->
            transactions
                .filter { it.transaction.amount < 0 }
                .sumOf { it.transaction.amount }
        }

        val budgetsThisMonth = budgetRepository.getBudgetsForMonth(currentMonth + 1, currentYear)

        budgetStatus = budgetsThisMonth.combine(transactionsThisMonth) { budgets, transactionDetails ->
            budgets.map { budget ->
                val spent = transactionDetails
                    .filter { it.categoryName == budget.categoryName && it.transaction.amount < 0 }
                    .sumOf { it.transaction.amount }

                BudgetWithSpending(
                    budget = budget,
                    spent = Math.abs(spent)
                )
            }.sortedByDescending { if (it.budget.amount > 0) (it.spent / it.budget.amount) else 0.0 }
                .take(3)
        }
    }
}
