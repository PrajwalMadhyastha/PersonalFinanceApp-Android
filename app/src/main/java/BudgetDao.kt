// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/BudgetDao.kt
// REASON: FIX - No changes were needed here. The `isExcluded = 0` filter was
// already correctly applied to the subqueries that calculate spending for budgets.
// This ensures that excluded transactions do not count against a user's budget,
// which is the correct behavior for these financial aggregations.
// =================================================================================
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

    @Query(
        """
        SELECT
            B.*,
            IFNULL(TxSums.totalSpent, 0.0) as spent,
            Cat.iconKey as iconKey,
            Cat.colorKey as colorKey
        FROM
            budgets AS B
        LEFT JOIN
            (SELECT
                C.name as categoryName,
                SUM(T.amount) as totalSpent
             FROM transactions AS T
             JOIN categories AS C ON T.categoryId = C.id
             WHERE T.transactionType = 'expense' AND strftime('%Y-%m', T.date / 1000, 'unixepoch') = :yearMonth AND T.isExcluded = 0 -- This is correct for budget spending
             GROUP BY C.name) AS TxSums
        ON B.categoryName = TxSums.categoryName
        LEFT JOIN categories AS Cat ON B.categoryName = Cat.name
        WHERE B.month = :month AND B.year = :year
    """
    )
    fun getBudgetsWithSpendingForMonth(yearMonth: String, month: Int, year: Int): Flow<List<BudgetWithSpending>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<Budget>)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget)

    @Query(
        "SELECT SUM(amount) FROM transactions WHERE categoryId = (SELECT id FROM categories WHERE name = :categoryName) AND strftime('%m', date / 1000, 'unixepoch') + 0 = :month AND strftime('%Y', date / 1000, 'unixepoch') + 0 = :year AND transactionType = 'expense' AND isExcluded = 0", // This is correct for budget spending
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
