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

    val budgetsForCurrentMonth: StateFlow<List<BudgetWithSpending>>
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

        val yearMonthString = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
        budgetsForCurrentMonth = budgetRepository.getBudgetsForMonthWithSpending(yearMonthString, currentMonth, currentYear)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

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
                val budgetedCategoryNames = budgets.map { it.budget.categoryName }.toSet()
                categories.filter { category -> category.name !in budgetedCategoryNames }
            }

        totalSpending = budgetsForCurrentMonth.flatMapLatest { budgets ->
            if (budgets.isEmpty()) {
                flowOf(0.0)
            } else {
                val spendingFlows = budgets.map {
                    getActualSpending(it.budget.categoryName)
                }
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
