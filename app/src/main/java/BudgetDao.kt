package com.example.personalfinanceapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    /**
     * Inserts a new budget. If a budget for the same category, month, and year
     * already exists, it will be replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget)

    /**
     * Retrieves all budgets for a specific month and year, ordered by category name.
     */
    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year ORDER BY categoryName ASC")
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>>

}