package io.pm.finlight

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year")
    fun getBudgetsForMonth(
        month: Int,
        year: Int,
    ): Flow<List<Budget>>

    // --- PERFORMANCE OPTIMIZATION ---
    // This new query calculates the actual spending for each category budget directly in the database.
    // It is much more efficient than fetching all transactions and all budgets into memory and combining them in the ViewModel.
    // It joins Budgets with an aggregate sum from Transactions for a given month and year.
    // --- FIX: The @Transaction annotation was removed as it's illegal on a @Query that returns a Flow. ---
    @Query("""
        SELECT
            B.*,
            IFNULL(TxSums.totalSpent, 0.0) as spent
        FROM
            budgets AS B
        LEFT JOIN
            (SELECT
                C.name as categoryName,
                SUM(T.amount) as totalSpent
             FROM transactions AS T
             JOIN categories AS C ON T.categoryId = C.id
             WHERE T.transactionType = 'expense' AND strftime('%Y-%m', T.date / 1000, 'unixepoch') = :yearMonth
             GROUP BY C.name) AS TxSums
        ON B.categoryName = TxSums.categoryName
        WHERE B.month = :month AND B.year = :year
    """)
    fun getBudgetsWithSpendingForMonth(yearMonth: String, month: Int, year: Int): Flow<List<BudgetWithSpending>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<Budget>)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget)

    // This query is more efficient as it performs the calculation in the database.
    @Query(
        "SELECT SUM(amount) FROM transactions WHERE categoryId = (SELECT id FROM categories WHERE name = :categoryName) AND strftime('%m', date / 1000, 'unixepoch') + 0 = :month AND strftime('%Y', date / 1000, 'unixepoch') + 0 = :year AND transactionType = 'expense'",
    )
    fun getActualSpendingForCategory(
        categoryName: String,
        month: Int,
        year: Int,
    ): Flow<Double?>

    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE id = :id")
    fun getById(id: Int): Flow<Budget?>

    @Update
    suspend fun update(budget: Budget)

    @Delete
    suspend fun delete(budget: Budget)
}
