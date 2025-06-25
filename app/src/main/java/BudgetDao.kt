package com.example.personalfinanceapp

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year")
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<Budget>)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget)

    // This query is more efficient as it performs the calculation in the database.
    @Query("SELECT SUM(amount) FROM transactions WHERE categoryId = (SELECT id FROM categories WHERE name = :categoryName) AND strftime('%m', date / 1000, 'unixepoch') + 0 = :month AND strftime('%Y', date / 1000, 'unixepoch') + 0 = :year AND transactionType = 'expense'")
    fun getActualSpendingForCategory(categoryName: String, month: Int, year: Int): Flow<Double?>

    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE id = :id")
    fun getById(id: Int): Flow<Budget?>

    @Update
    suspend fun update(budget: Budget)

    @Delete
    suspend fun delete(budget: Budget)
}
