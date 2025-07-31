// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/BudgetDao.kt
// REASON: FEATURE (Splitting) - The queries for calculating spending against
// budgets (`getBudgetsWithSpendingForMonth` and `getActualSpendingForCategory`)
// have been rewritten. They now use a UNION ALL to combine non-split transactions
// with child split items, ensuring budget calculations are accurate.
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
        WITH AtomicExpenses AS (
            -- 1. Regular, non-split transactions
            SELECT T.categoryId, T.amount FROM transactions AS T
            WHERE T.isSplit = 0 AND T.transactionType = 'expense' AND strftime('%Y-%m', T.date / 1000, 'unixepoch', 'localtime') = :yearMonth AND T.isExcluded = 0
            UNION ALL
            -- 2. Child items from split transactions
            SELECT S.categoryId, S.amount FROM split_transactions AS S
            JOIN transactions AS P ON S.parentTransactionId = P.id
            WHERE P.transactionType = 'expense' AND strftime('%Y-%m', P.date / 1000, 'unixepoch', 'localtime') = :yearMonth AND P.isExcluded = 0
        )
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
                SUM(AE.amount) as totalSpent
             FROM AtomicExpenses AS AE
             JOIN categories AS C ON AE.categoryId = C.id
             WHERE AE.categoryId IS NOT NULL
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
        """
        WITH AtomicExpenses AS (
            SELECT T.categoryId, T.amount FROM transactions AS T
            WHERE T.isSplit = 0 AND T.transactionType = 'expense' AND strftime('%m', T.date / 1000, 'unixepoch', 'localtime') + 0 = :month AND strftime('%Y', T.date / 1000, 'unixepoch', 'localtime') + 0 = :year AND T.isExcluded = 0
            UNION ALL
            SELECT S.categoryId, S.amount FROM split_transactions AS S
            JOIN transactions AS P ON S.parentTransactionId = P.id
            WHERE P.transactionType = 'expense' AND strftime('%m', P.date / 1000, 'unixepoch', 'localtime') + 0 = :month AND strftime('%Y', P.date / 1000, 'unixepoch', 'localtime') + 0 = :year AND P.isExcluded = 0
        )
        SELECT SUM(AE.amount) FROM AtomicExpenses AS AE
        JOIN categories AS C ON AE.categoryId = C.id
        WHERE C.name = :categoryName AND AE.categoryId IS NOT NULL
    """
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
