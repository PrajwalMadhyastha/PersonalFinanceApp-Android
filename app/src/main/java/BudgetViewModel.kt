package com.example.personalfinanceapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat // <-- FIX: Import added
import java.util.Calendar
import java.util.Locale         // <-- FIX: Import added

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BudgetRepository

    val budgetsForCurrentMonth: Flow<List<Budget>>
    private val currentMonth: Int
    private val currentYear: Int

    init {
        val budgetDao = AppDatabase.getInstance(application).budgetDao()
        repository = BudgetRepository(budgetDao)

        val calendar = Calendar.getInstance()
        currentMonth = calendar.get(Calendar.MONTH) + 1
        currentYear = calendar.get(Calendar.YEAR)

        budgetsForCurrentMonth = repository.getBudgetsForMonth(currentMonth, currentYear)
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
            repository.insert(newBudget)
        }
    }

    fun getCurrentMonthYearString(): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, currentMonth - 1)
        calendar.set(Calendar.YEAR, currentYear)
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    }
}