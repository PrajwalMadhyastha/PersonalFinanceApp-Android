// =================================================================================
// FILE: /app/src/main/java/com/pm/finlight/BudgetViewModel.kt
// REASON: Fixed a @Composable invocation error by moving the total spending calculation
// from the UI layer into the ViewModel.
// FIX: Added a new `totalSpending` StateFlow that combines the spending from all
// category budgets into a single, observable value.
// =================================================================================
package io.pm.finlight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BudgetViewModel(application: Application) : AndroidViewModel(application) {
    private val budgetRepository: BudgetRepository
    private val settingsRepository: SettingsRepository
    private val categoryRepository: CategoryRepository

    private val calendar: Calendar = Calendar.getInstance()
    private val currentMonth: Int
    private val currentYear: Int

    val budgetsForCurrentMonth: Flow<List<Budget>>
    val overallBudget: StateFlow<Float>
    val allCategories: Flow<List<Category>>
    val availableCategoriesForNewBudget: Flow<List<Category>>
    val totalSpending: StateFlow<Double>

    init {
        val db = AppDatabase.getInstance(application)
        budgetRepository = BudgetRepository(db.budgetDao())
        settingsRepository = SettingsRepository(application)
        categoryRepository = CategoryRepository(db.categoryDao())

        currentMonth = calendar.get(Calendar.MONTH) + 1
        currentYear = calendar.get(Calendar.YEAR)

        budgetsForCurrentMonth = budgetRepository.getBudgetsForMonth(currentMonth, currentYear)
        allCategories = categoryRepository.allCategories

        overallBudget =
            settingsRepository.getOverallBudgetForCurrentMonth()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = 0f,
                )

        availableCategoriesForNewBudget =
            combine(allCategories, budgetsForCurrentMonth) { categories, budgets ->
                val budgetedCategoryNames = budgets.map { it.categoryName }.toSet()
                categories.filter { category -> category.name !in budgetedCategoryNames }
            }

        // NEW: Calculate total spending from all category budgets
        totalSpending = budgetsForCurrentMonth.flatMapLatest { budgets ->
            if (budgets.isEmpty()) {
                flowOf(0.0) // Emit 0 if there are no budgets
            } else {
                // Create a list of spending flows for each budget
                val spendingFlows = budgets.map { budget ->
                    getActualSpending(budget.categoryName)
                }
                // Combine them to get the sum
                combine(spendingFlows) { amounts ->
                    amounts.sum()
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )
    }

    fun getActualSpending(categoryName: String): Flow<Double> {
        return budgetRepository.getActualSpendingForCategory(categoryName, currentMonth, currentYear)
            .map { spending -> spending ?: 0.0 }
    }

    fun addCategoryBudget(
        categoryName: String,
        amountStr: String,
    ) {
        val amount = amountStr.toDoubleOrNull() ?: return
        if (amount <= 0 || categoryName.isBlank()) {
            return
        }
        val newBudget =
            Budget(
                categoryName = categoryName,
                amount = amount,
                month = currentMonth,
                year = currentYear,
            )
        viewModelScope.launch {
            budgetRepository.insert(newBudget)
        }
    }

    fun saveOverallBudget(budgetStr: String) {
        val budgetFloat = budgetStr.toFloatOrNull() ?: 0f
        settingsRepository.saveOverallBudgetForCurrentMonth(budgetFloat)
    }

    fun getBudgetById(id: Int): Flow<Budget?> {
        return budgetRepository.getBudgetById(id)
    }

    fun updateBudget(budget: Budget) =
        viewModelScope.launch {
            budgetRepository.update(budget)
        }

    fun deleteBudget(budget: Budget) =
        viewModelScope.launch {
            budgetRepository.delete(budget)
        }

    fun getCurrentMonthYearString(): String {
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    }
}
