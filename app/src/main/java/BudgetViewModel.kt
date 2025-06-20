package com.example.personalfinanceapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val budgetRepository: BudgetRepository
    private val calendar: Calendar = Calendar.getInstance()
    private val currentMonth: Int
    private val currentYear: Int

    val budgetsForCurrentMonth: Flow<List<Budget>>

    init {
        val db = AppDatabase.getInstance(application)
        val budgetDao = db.budgetDao()
        budgetRepository = BudgetRepository(budgetDao)

        currentMonth = calendar.get(Calendar.MONTH) + 1 // Month is 0-based, so add 1
        currentYear = calendar.get(Calendar.YEAR)

        budgetsForCurrentMonth = budgetRepository.getBudgetsForMonth(currentMonth, currentYear)
    }

    /**
     * Gets the actual spending for a given budget category for the current month.
     * This now uses the more efficient repository method.
     */
    fun getActualSpending(categoryName: String): Flow<Double> {
        return budgetRepository.getActualSpendingForCategory(categoryName, currentMonth, currentYear)
            .map { spending -> spending ?: 0.0 } // Ensure we don't return null
    }

    fun addBudget(categoryName: String, amountStr: String) {
        val amount = amountStr.toDoubleOrNull() ?: return
        if (amount <= 0 || categoryName.isBlank()) {
            return // Add error handling here in a real app
        }

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
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    }
}
