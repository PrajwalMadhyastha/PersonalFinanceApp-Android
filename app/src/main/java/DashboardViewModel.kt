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

    // These are the public properties the UI will collect data from
    val netWorth: Flow<Double>
    val recentTransactions: Flow<List<TransactionWithAccount>>
    val monthlyIncome: Flow<Double>
    val monthlyExpenses: Flow<Double>
    val budgetStatus: Flow<List<BudgetWithSpending>>

    init {
        // Initialize all necessary repositories from our database singleton
        val db = AppDatabase.getInstance(application)
        transactionRepository = TransactionRepository(db.transactionDao())
        accountRepository = AccountRepository(db.accountDao())
        budgetRepository = BudgetRepository(db.budgetDao())


        // --- Instruction: Calculate the total balance across all Account objects ---
        // This 'netWorth' flow combines all accounts and all transactions.
        // It recalculates automatically if either list changes.
        netWorth = accountRepository.allAccounts.combine(
            transactionRepository.getAllTransactionsSimple()
        ) { accounts, transactions ->
            // We calculate the balance of each account and sum them up.
            accounts.sumOf { account ->
                transactions
                    .filter { it.accountId == account.id }
                    .sumOf { it.amount }
            }
        }

        // --- Instruction: Fetch the 3-5 most recent Transaction objects ---
        // The 'allTransactions' flow from the repository is already sorted by date descending.
        // We use .map to simply take the first 5 items from the list.
        recentTransactions = transactionRepository.allTransactions.map { list ->
            list.take(5)
        }

        // --- Logic to get transactions for the current month ---
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) // Jan is 0, Feb is 1, etc.

        // Set calendar to the first millisecond of the current month
        calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
        val startDate = calendar.timeInMillis

        // Set calendar to the last millisecond of the current month
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endDate = calendar.timeInMillis

        // This Flow contains only the transactions for the current month
        val transactionsThisMonth = transactionRepository.getAllTransactionsForRange(startDate, endDate)

        // --- Instruction: Calculate the total income and total expenses for the current month ---
        monthlyIncome = transactionsThisMonth.map { transactions ->
            transactions.filter { it.amount > 0 }.sumOf { it.amount }
        }

        monthlyExpenses = transactionsThisMonth.map { transactions ->
            transactions.filter { it.amount < 0 }.sumOf { it.amount }
        }

        // --- Instruction: Fetch budgets and sort them by percentage used ---
        val budgetsThisMonth = budgetRepository.getBudgetsForMonth(currentMonth + 1, currentYear) // Our month is 1-12

        budgetStatus = budgetsThisMonth.combine(transactionsThisMonth) { budgets, transactions ->
            budgets.map { budget ->
                val spent = transactions
                    .filter { it.description == budget.categoryName && it.amount < 0 }
                    .sumOf { it.amount }

                BudgetWithSpending(
                    budget = budget,
                    spent = Math.abs(spent)
                )
            }
                // Sort by the highest percentage spent first and take the top 3
                .sortedByDescending { if (it.budget.amount > 0) (it.spent / it.budget.amount) else 0.0 }
                .take(3)
        }
    }
}