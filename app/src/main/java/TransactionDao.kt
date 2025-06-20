package com.example.personalfinanceapp

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // ... (all your existing DAO methods are here)

    @Query("""
        SELECT
            T.*,
            A.name as accountName,
            C.name as categoryName
        FROM
            transactions AS T
        LEFT JOIN
            accounts AS A ON T.accountId = A.id
        LEFT JOIN
            categories AS C ON T.categoryId = C.id
        ORDER BY
            T.date DESC
    """)
    fun getAllTransactions(): Flow<List<TransactionDetails>>

    @Query("""
        SELECT
            T.*,
            A.name as accountName,
            C.name as categoryName
        FROM
            transactions AS T
        LEFT JOIN
            accounts AS A ON T.accountId = A.id
        LEFT JOIN
            categories AS C ON T.categoryId = C.id
        WHERE T.date BETWEEN :startDate AND :endDate
        ORDER BY
            T.date DESC
    """)
    fun getTransactionDetailsForRange(startDate: Long, endDate: Long): Flow<List<TransactionDetails>>

    // --- NEW: This is the method required by AccountViewModel ---
    @Query("""
        SELECT T.*, A.name as accountName, C.name as categoryName
        FROM transactions AS T
        LEFT JOIN accounts AS A ON T.accountId = A.id
        LEFT JOIN categories AS C ON T.categoryId = C.id
        WHERE T.accountId = :accountId
        ORDER BY T.date DESC
    """)
    fun getTransactionsForAccountDetails(accountId: Int): Flow<List<TransactionDetails>>


    @Query("SELECT * FROM transactions")
    fun getAllTransactionsSimple(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getAllTransactionsForRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Int): Flow<Transaction?>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getTransactionsForAccount(accountId: Int): Flow<List<Transaction>>

    @Query("""
        SELECT SUM(T.amount) FROM transactions AS T
        INNER JOIN categories AS C ON T.categoryId = C.id
        WHERE C.name = :categoryName AND T.date BETWEEN :startDate AND :endDate AND T.transactionType = 'expense'
    """)
    fun getSpendingForCategory(categoryName: String, startDate: Long, endDate: Long): Flow<Double?>

    @Query("""
        SELECT C.name as categoryName, SUM(T.amount) as totalAmount
        FROM transactions AS T
        INNER JOIN categories AS C ON T.categoryId = C.id
        WHERE T.transactionType = 'expense' AND T.date BETWEEN :startDate AND :endDate
        GROUP BY C.name
        ORDER BY totalAmount ASC
    """)
    fun getSpendingByCategoryForMonth(startDate: Long, endDate: Long): Flow<List<CategorySpending>>

    @Query("""
        SELECT
            strftime('%Y-%m', date / 1000, 'unixepoch') as monthYear,
            SUM(CASE WHEN transactionType = 'income' THEN amount ELSE 0 END) as totalIncome,
            SUM(CASE WHEN transactionType = 'expense' THEN amount ELSE 0 END) as totalExpenses
        FROM transactions
        WHERE date >= :startDate
        GROUP BY monthYear
        ORDER BY monthYear ASC
    """)
    fun getMonthlyTrends(startDate: Long): Flow<List<MonthlyTrend>>


    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countTransactionsForCategory(categoryId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)
}
