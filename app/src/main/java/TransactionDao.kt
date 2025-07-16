package io.pm.finlight

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query(
        """
        SELECT
            C.name as categoryName,
            SUM(T.amount) as totalAmount,
            C.iconKey as iconKey,
            C.colorKey as categoryColorKey
        FROM transactions AS T
        INNER JOIN categories AS C ON T.categoryId = C.id
        WHERE T.transactionType = 'expense' AND T.date BETWEEN :startDate AND :endDate
          AND T.isExcluded = 0
        GROUP BY C.name
        ORDER BY totalAmount DESC
        LIMIT 3
    """
    )
    suspend fun getTopSpendingCategoriesForRange(startDate: Long, endDate: Long): List<CategorySpending>

    @Query(
        """
        SELECT
            C.name as categoryName,
            SUM(T.amount) as totalAmount,
            C.iconKey as iconKey,
            C.colorKey as categoryColorKey
        FROM transactions AS T
        INNER JOIN categories AS C ON T.categoryId = C.id
        WHERE T.transactionType = 'expense' AND T.date BETWEEN :startDate AND :endDate
          AND T.isExcluded = 0
        GROUP BY C.name
        ORDER BY totalAmount DESC
        LIMIT 1
    """
    )
    fun getTopSpendingCategoriesForRangeFlow(startDate: Long, endDate: Long): Flow<CategorySpending?>


    @Query("UPDATE transactions SET isExcluded = :isExcluded WHERE id = :id")
    suspend fun updateExclusionStatus(id: Int, isExcluded: Boolean)

    @Query(
        """
        SELECT
            T.*,
            A.name as accountName,
            C.name as categoryName,
            C.iconKey as categoryIconKey,
            C.colorKey as categoryColorKey
        FROM
            transactions AS T
        LEFT JOIN
            accounts AS A ON T.accountId = A.id
        LEFT JOIN
            categories AS C ON T.categoryId = C.id
        ORDER BY
            T.date DESC
    """
    )
    fun getAllTransactions(): Flow<List<TransactionDetails>>

    @Query("""
        SELECT
            T.*,
            A.name as accountName,
            C.name as categoryName,
            C.iconKey as categoryIconKey,
            C.colorKey as categoryColorKey
        FROM
            transactions AS T
        LEFT JOIN
            accounts AS A ON T.accountId = A.id
        LEFT JOIN
            categories AS C ON T.categoryId = C.id
        WHERE T.transactionType = 'income' AND T.date BETWEEN :startDate AND :endDate
          AND T.isExcluded = 0
          AND (:keyword IS NULL OR LOWER(T.description) LIKE '%' || LOWER(:keyword) || '%' OR LOWER(T.notes) LIKE '%' || LOWER(:keyword) || '%')
          AND (:accountId IS NULL OR T.accountId = :accountId)
          AND (:categoryId IS NULL OR T.categoryId = :categoryId)
        ORDER BY
            T.date DESC
    """)
    fun getIncomeTransactionsForRange(startDate: Long, endDate: Long, keyword: String?, accountId: Int?, categoryId: Int?): Flow<List<TransactionDetails>>

    @Query("""
        SELECT 
            C.name as categoryName, 
            SUM(T.amount) as totalAmount,
            C.iconKey as iconKey,
            C.colorKey as colorKey
        FROM transactions AS T
        INNER JOIN categories AS C ON T.categoryId = C.id
        WHERE T.transactionType = 'income' AND T.date BETWEEN :startDate AND :endDate
          AND T.isExcluded = 0
          AND (:keyword IS NULL OR LOWER(T.description) LIKE '%' || LOWER(:keyword) || '%' OR LOWER(T.notes) LIKE '%' || LOWER(:keyword) || '%')
          AND (:accountId IS NULL OR T.accountId = :accountId)
          AND (:categoryId IS NULL OR T.categoryId = :categoryId)
        GROUP BY C.name
        ORDER BY totalAmount DESC
    """)
    fun getIncomeByCategoryForMonth(startDate: Long, endDate: Long, keyword: String?, accountId: Int?, categoryId: Int?): Flow<List<CategorySpending>>

    @Query("""
        SELECT
            description as merchantName,
            SUM(amount) as totalAmount,
            COUNT(id) as transactionCount
        FROM transactions
        WHERE transactionType = 'expense' AND date BETWEEN :startDate AND :endDate
          AND isExcluded = 0
          AND (:keyword IS NULL OR LOWER(description) LIKE '%' || LOWER(:keyword) || '%' OR LOWER(notes) LIKE '%' || LOWER(:keyword) || '%')
          AND (:accountId IS NULL OR accountId = :accountId)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
        GROUP BY LOWER(description)
        ORDER BY totalAmount DESC
    """)
    fun getSpendingByMerchantForMonth(startDate: Long, endDate: Long, keyword: String?, accountId: Int?, categoryId: Int?): Flow<List<MerchantSpendingSummary>>

    @Insert
    suspend fun insertImage(transactionImage: TransactionImage)

    @Delete
    suspend fun deleteImage(transactionImage: TransactionImage)

    @Query("SELECT * FROM transaction_images WHERE transactionId = :transactionId")
    fun getImagesForTransaction(transactionId: Int): Flow<List<TransactionImage>>


    @Query("UPDATE transactions SET description = :description WHERE id = :id")
    suspend fun updateDescription(id: Int, description: String)

    @Query("UPDATE transactions SET amount = :amount WHERE id = :id")
    suspend fun updateAmount(id: Int, amount: Double)

    @Query("UPDATE transactions SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Int, notes: String?)

    @Query("UPDATE transactions SET categoryId = :categoryId WHERE id = :id")
    suspend fun updateCategoryId(id: Int, categoryId: Int?)

    @Query("UPDATE transactions SET accountId = :accountId WHERE id = :id")
    suspend fun updateAccountId(id: Int, accountId: Int)

    @Query("UPDATE transactions SET date = :date WHERE id = :id")
    suspend fun updateDate(id: Int, date: Long)


    @Query(
        """
        SELECT
            T.*,
            A.name as accountName,
            C.name as categoryName,
            C.iconKey as categoryIconKey,
            C.colorKey as categoryColorKey
        FROM
            transactions AS T
        LEFT JOIN
            accounts AS A ON T.accountId = A.id
        LEFT JOIN
            categories AS C ON T.categoryId = C.id
        WHERE T.id = :id
    """
    )
    fun getTransactionDetailsById(id: Int): Flow<TransactionDetails?>


    @Query(
        """
        SELECT
            T.*,
            A.name as accountName,
            C.name as categoryName,
            C.iconKey as categoryIconKey,
            C.colorKey as categoryColorKey
        FROM
            transactions AS T
        LEFT JOIN
            accounts AS A ON T.accountId = A.id
        LEFT JOIN
            categories AS C ON T.categoryId = C.id
        ORDER BY
            T.date DESC
        LIMIT 5
    """
    )
    fun getRecentTransactionDetails(): Flow<List<TransactionDetails>>

    @Query("SELECT sourceSmsHash FROM transactions WHERE sourceSmsHash IS NOT NULL")
    fun getAllSmsHashes(): Flow<List<String>>

    @Query(
        """
        SELECT
            T.*,
            A.name as accountName,
            C.name as categoryName,
            C.iconKey as categoryIconKey,
            C.colorKey as categoryColorKey
        FROM
            transactions AS T
        LEFT JOIN
            accounts AS A ON T.accountId = A.id
        LEFT JOIN
            categories AS C ON T.categoryId = C.id
        WHERE T.date BETWEEN :startDate AND :endDate
          AND (:keyword IS NULL OR LOWER(T.description) LIKE '%' || LOWER(:keyword) || '%' OR LOWER(T.notes) LIKE '%' || LOWER(:keyword) || '%')
          AND (:accountId IS NULL OR T.accountId = :accountId)
          AND (:categoryId IS NULL OR T.categoryId = :categoryId)
        ORDER BY
            T.date DESC
    """
    )
    fun getTransactionDetailsForRange(
        startDate: Long,
        endDate: Long,
        keyword: String?,
        accountId: Int?,
        categoryId: Int?
    ): Flow<List<TransactionDetails>>

    @Query(
        """
        SELECT t.*, a.name as accountName, c.name as categoryName, c.iconKey as categoryIconKey, c.colorKey as categoryColorKey
        FROM transactions t
        LEFT JOIN accounts a ON t.accountId = a.id
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.accountId = :accountId
        ORDER BY t.date DESC
    """
    )
    fun getTransactionsForAccountDetails(accountId: Int): Flow<List<TransactionDetails>>

    @Query("SELECT * FROM transactions")
    fun getAllTransactionsSimple(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getAllTransactionsForRange(
        startDate: Long,
        endDate: Long,
    ): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Int): Flow<Transaction?>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getTransactionsForAccount(accountId: Int): Flow<List<Transaction>>

    @Query(
        """
        SELECT SUM(T.amount) FROM transactions AS T
        INNER JOIN categories AS C ON T.categoryId = C.id
        WHERE C.name = :categoryName AND T.date BETWEEN :startDate AND :endDate AND T.transactionType = 'expense' AND T.isExcluded = 0
    """
    )
    fun getSpendingForCategory(
        categoryName: String,
        startDate: Long,
        endDate: Long,
    ): Flow<Double?>

    @Query(
        """
        SELECT 
            C.name as categoryName, 
            SUM(T.amount) as totalAmount,
            C.iconKey as iconKey,
            C.colorKey as colorKey
        FROM transactions AS T
        INNER JOIN categories AS C ON T.categoryId = C.id
        WHERE T.transactionType = 'expense' AND T.date BETWEEN :startDate AND :endDate
          AND T.isExcluded = 0
          AND (:keyword IS NULL OR LOWER(T.description) LIKE '%' || LOWER(:keyword) || '%' OR LOWER(T.notes) LIKE '%' || LOWER(:keyword) || '%')
          AND (:accountId IS NULL OR T.accountId = :accountId)
          AND (:categoryId IS NULL OR T.categoryId = :categoryId)
        GROUP BY C.name
        ORDER BY totalAmount ASC
    """
    )
    fun getSpendingByCategoryForMonth(
        startDate: Long,
        endDate: Long,
        keyword: String?,
        accountId: Int?,
        categoryId: Int?
    ): Flow<List<CategorySpending>>

    @Query(
        """
        SELECT
            strftime('%Y-%m', date / 1000, 'unixepoch', 'localtime') as monthYear,
            SUM(CASE WHEN transactionType = 'income' THEN amount ELSE 0 END) as totalIncome,
            SUM(CASE WHEN transactionType = 'expense' THEN amount ELSE 0 END) as totalExpenses
        FROM transactions
        WHERE date >= :startDate AND isExcluded = 0
        GROUP BY monthYear
        ORDER BY monthYear ASC
    """
    )
    fun getMonthlyTrends(startDate: Long): Flow<List<MonthlyTrend>>

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countTransactionsForCategory(categoryId: Int): Int

    @Query("SELECT COUNT(*) FROM transaction_tag_cross_ref WHERE tagId = :tagId")
    suspend fun countTransactionsForTag(tagId: Int): Int

    @Query("""
        SELECT
            SUM(CASE WHEN transactionType = 'income' THEN amount ELSE 0 END) as totalIncome,
            SUM(CASE WHEN transactionType = 'expense' THEN amount ELSE 0 END) as totalExpenses
        FROM transactions
        WHERE date BETWEEN :startDate AND :endDate AND isExcluded = 0
    """)
    suspend fun getFinancialSummaryForRange(startDate: Long, endDate: Long): FinancialSummary?

    @Query("""
        SELECT
            SUM(CASE WHEN transactionType = 'income' THEN amount ELSE 0 END) as totalIncome,
            SUM(CASE WHEN transactionType = 'expense' THEN amount ELSE 0 END) as totalExpenses
        FROM transactions
        WHERE date BETWEEN :startDate AND :endDate AND isExcluded = 0
    """)
    fun getFinancialSummaryForRangeFlow(startDate: Long, endDate: Long): Flow<FinancialSummary?>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTagsToTransaction(crossRefs: List<TransactionTagCrossRef>)

    @Query("DELETE FROM transaction_tag_cross_ref WHERE transactionId = :transactionId")
    suspend fun clearTagsForTransaction(transactionId: Int)

    @Query("SELECT T.* FROM tags T INNER JOIN transaction_tag_cross_ref TTCR ON T.id = TTCR.tagId WHERE TTCR.transactionId = :transactionId")
    fun getTagsForTransaction(transactionId: Int): Flow<List<Tag>>

    @Query("SELECT T.* FROM tags T INNER JOIN transaction_tag_cross_ref TTCR ON T.id = TTCR.tagId WHERE TTCR.transactionId = :transactionId")
    suspend fun getTagsForTransactionSimple(transactionId: Int): List<Tag>

    @Query("""
        SELECT T.*, A.name as accountName, C.name as categoryName, C.iconKey as categoryIconKey, C.colorKey as categoryColorKey
        FROM transactions AS T
        LEFT JOIN accounts AS A ON T.accountId = A.id
        LEFT JOIN categories AS C ON T.categoryId = C.id
        WHERE T.date BETWEEN :startDate AND :endDate
        ORDER BY T.date DESC
    """)
    fun getTransactionsForDateRange(startDate: Long, endDate: Long): Flow<List<TransactionDetails>>

    @Query("""
        SELECT
            strftime('%Y-%m-%d', date / 1000, 'unixepoch', 'localtime') as date,
            SUM(CASE WHEN transactionType = 'expense' THEN amount ELSE 0 END) as totalAmount
        FROM transactions
        WHERE date BETWEEN :startDate AND :endDate AND isExcluded = 0
        GROUP BY date
        ORDER BY date ASC
    """)
    fun getDailySpendingForDateRange(startDate: Long, endDate: Long): Flow<List<DailyTotal>>

    @Query("""
        SELECT
            strftime('%Y-%W', date / 1000, 'unixepoch', 'localtime') as period,
            SUM(CASE WHEN transactionType = 'expense' THEN amount ELSE 0 END) as totalAmount
        FROM transactions
        WHERE date BETWEEN :startDate AND :endDate AND isExcluded = 0
        GROUP BY period
        ORDER BY period ASC
    """)
    fun getWeeklySpendingForDateRange(startDate: Long, endDate: Long): Flow<List<PeriodTotal>>

    @Query("""
        SELECT
            strftime('%Y-%m', date / 1000, 'unixepoch', 'localtime') as period,
            SUM(CASE WHEN transactionType = 'expense' THEN amount ELSE 0 END) as totalAmount
        FROM transactions
        WHERE date BETWEEN :startDate AND :endDate AND isExcluded = 0
        GROUP BY period
        ORDER BY period ASC
    """)
    fun getMonthlySpendingForDateRange(startDate: Long, endDate: Long): Flow<List<PeriodTotal>>

    @Query("UPDATE transactions SET sourceSmsHash = :smsHash WHERE id = :transactionId")
    suspend fun setSmsHash(transactionId: Int, smsHash: String)

    @Query("""
        SELECT * FROM transactions
        WHERE sourceSmsHash IS NULL
          AND date BETWEEN :startDate AND :endDate
          AND amount BETWEEN :minAmount AND :maxAmount
          AND transactionType = :transactionType
        ORDER BY ABS(date - :smsDate) ASC
    """)
    suspend fun findLinkableTransactions(
        startDate: Long,
        endDate: Long,
        minAmount: Double,
        maxAmount: Double,
        smsDate: Long,
        transactionType: String
    ): List<Transaction>

    @Query("""
        SELECT COUNT(*) FROM transactions
        WHERE LOWER(description) = LOWER(:description) OR LOWER(originalDescription) = LOWER(:description)
        AND isExcluded = 0
    """)
    fun getTransactionCountForMerchant(description: String): Flow<Int>

    @Query("""
        SELECT * FROM transactions
        WHERE (LOWER(description) = LOWER(:description) OR LOWER(originalDescription) = LOWER(:description))
        AND id != :excludeId
        AND isExcluded = 0
    """)
    suspend fun findSimilarTransactions(description: String, excludeId: Int): List<Transaction>

    @Query("UPDATE transactions SET categoryId = :categoryId WHERE id IN (:ids)")
    suspend fun updateCategoryForIds(ids: List<Int>, categoryId: Int)

    @Query("UPDATE transactions SET description = :newDescription WHERE id IN (:ids)")
    suspend fun updateDescriptionForIds(ids: List<Int>, newDescription: String)

    @Query("""
        SELECT t.*, a.name as accountName, c.name as categoryName, c.iconKey as categoryIconKey, c.colorKey as categoryColorKey
        FROM transactions t
        LEFT JOIN accounts a ON t.accountId = a.id
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE
            (:keyword = '' OR LOWER(t.description) LIKE '%' || LOWER(:keyword) || '%' OR LOWER(t.notes) LIKE '%' || LOWER(:keyword) || '%') AND
            (:accountId IS NULL OR t.accountId = :accountId) AND
            (:categoryId IS NULL OR t.categoryId = :categoryId) AND
            (:transactionType IS NULL OR t.transactionType = :transactionType) AND
            (:startDate IS NULL OR t.date >= :startDate) AND
            (:endDate IS NULL OR t.date <= :endDate)
        ORDER BY t.date DESC
    """)
    suspend fun searchTransactions(
        keyword: String,
        accountId: Int?,
        categoryId: Int?,
        transactionType: String?,
        startDate: Long?,
        endDate: Long?,
    ): List<TransactionDetails>
}
