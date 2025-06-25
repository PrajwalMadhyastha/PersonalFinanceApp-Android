package com.example.personalfinanceapp

import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val budgetDao: BudgetDao) {

    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>> {
        return budgetDao.getBudgetsForMonth(month, year)
    }

    // Expose the more efficient query to the ViewModel
    fun getActualSpendingForCategory(categoryName: String, month: Int, year: Int): Flow<Double?> {
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
