package com.example.personalfinanceapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val budgetRepository: BudgetRepository
    // --- NEW: Add a property for the transaction repository ---
    private val transactionRepository: TransactionRepository

    val budgetsForCurrentMonth: Flow<List<Budget>>
    private val currentMonth: Int
    private val currentYear: Int

    init {
        val budgetDao = AppDatabase.getInstance(application).budgetDao()
        // --- NEW: Get an instance of the TransactionDao ---
        val transactionDao = AppDatabase.getInstance(application).transactionDao()

        budgetRepository = BudgetRepository(budgetDao)
        // --- NEW: Initialize the transaction repository ---
        transactionRepository = TransactionRepository(transactionDao)

        val calendar = Calendar.getInstance()
        currentMonth = calendar.get(Calendar.MONTH) + 1
        currentYear = calendar.get(Calendar.YEAR)

        budgetsForCurrentMonth = budgetRepository.getBudgetsForMonth(currentMonth, currentYear)
    }

    // --- NEW: This is the core logic you requested ---
    /**
     * Gets the actual spending for a given budget category for the current month.
     *
     * @param categoryName The name of the budget category.
     * @return A Flow that emits the total spending as a Double. Emits 0.0 if no spending.
     */
    fun getActualSpending(categoryName: String): Flow<Double?> {
        // 1. Get a calendar instance to define the date range.
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, currentYear)
        calendar.set(Calendar.MONTH, currentMonth - 1) // Month is 0-indexed

        // 2. Calculate the start of the month.
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startDate = calendar.timeInMillis

        // 3. Calculate the end of the month.
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endDate = calendar.timeInMillis

        // 4. Call the repository with the calculated date range.
        return transactionRepository.getSpendingForCategory(categoryName, startDate, endDate)
    }


    fun addBudget(categoryName: String, amountStr: String) {
        val amount = amountStr.toDoubleOrNull() ?: return

        val newBudget = Budget(
            categoryName = categoryName,
            amount = amount,
            month = currentMonth,
            year = currentYear
        )
        viewModelScope.launch {
            budgetRepository.insert(newBudget)
        }
    }

    fun getCurrentMonthYearString(): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, currentMonth - 1)
        calendar.set(Calendar.YEAR, currentYear)
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    }
}