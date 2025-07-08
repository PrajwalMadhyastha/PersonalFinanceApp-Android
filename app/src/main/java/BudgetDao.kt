// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/BudgetDao.kt
// REASON: BUG FIX - All `strftime` date functions have been updated to use the
// 'localtime' modifier. This ensures that all date-based grouping is performed
// using the device's local timezone instead of UTC. This corrects the bug where
// spending was being attributed to the wrong month, fixing budget calculations.
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
             WHERE T.transactionType = 'expense' AND strftime('%Y-%m', T.date / 1000, 'unixepoch', 'localtime') = :yearMonth AND T.isExcluded = 0
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
        "SELECT SUM(amount) FROM transactions WHERE categoryId = (SELECT id FROM categories WHERE name = :categoryName) AND strftime('%m', date / 1000, 'unixepoch', 'localtime') + 0 = :month AND strftime('%Y', date / 1000, 'unixepoch', 'localtime') + 0 = :year AND transactionType = 'expense' AND isExcluded = 0",
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
