package com.example.personalfinanceapp

import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val budgetDao: BudgetDao) {

    /**
     * Retrieves all budgets for a specific month and year as a Flow.
     */
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>> {
        return budgetDao.getBudgetsForMonth(month, year)
    }

    /**
     * Inserts a new budget into the database.
     */
    suspend fun insert(budget: Budget) {
        budgetDao.insert(budget)
    }
}