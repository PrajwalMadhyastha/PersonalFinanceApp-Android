// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/BudgetRepository.kt
// REASON: FIX - The unused `getBudgetsForMonth` function has been removed to
// resolve the "UnusedSymbol" warning.
// =================================================================================
package io.pm.finlight

import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val budgetDao: BudgetDao) {

    fun getBudgetsForMonthWithSpending(yearMonth: String, month: Int, year: Int): Flow<List<BudgetWithSpending>> {
        return budgetDao.getBudgetsWithSpendingForMonth(yearMonth, month, year)
    }

    fun getActualSpendingForCategory(
        categoryName: String,
        month: Int,
        year: Int,
    ): Flow<Double?> {
        return budgetDao.getActualSpendingForCategory(categoryName, month, year)
    }

    suspend fun update(budget: Budget) {
        budgetDao.update(budget)
    }

    suspend fun insert(budget: Budget) {
        budgetDao.insert(budget)
    }

    suspend fun delete(budget: Budget) {
        budgetDao.delete(budget)
    }

    fun getBudgetById(id: Int): Flow<Budget?> {
        return budgetDao.getById(id)
    }
}
