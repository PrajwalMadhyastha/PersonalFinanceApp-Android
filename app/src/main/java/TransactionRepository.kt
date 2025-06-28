package io.pm.finlight

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

class TransactionRepository(private val transactionDao: TransactionDao) {
    val allTransactions: Flow<List<TransactionDetails>> =
        transactionDao.getAllTransactions()
            .onEach { transactions ->
                Log.d(
                    "TransactionFlowDebug",
                    "Repository Flow Emitted. Count: ${transactions.size}. Newest: ${transactions.firstOrNull()?.transaction?.description}",
                )
            }

    fun getAllSmsHashes(): Flow<List<String>> {
        return transactionDao.getAllSmsHashes()
    }

    fun getTransactionsForAccountDetails(accountId: Int): Flow<List<TransactionDetails>> {
        return transactionDao.getTransactionsForAccountDetails(accountId)
    }

    fun getTransactionDetailsForRange(
        startDate: Long,
        endDate: Long,
    ): Flow<List<TransactionDetails>> {
        return transactionDao.getTransactionDetailsForRange(startDate, endDate)
    }

    fun getAllTransactionsSimple(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactionsSimple()
    }

    fun getAllTransactionsForRange(
        startDate: Long,
        endDate: Long,
    ): Flow<List<Transaction>> {
        return transactionDao.getAllTransactionsForRange(startDate, endDate)
    }

    fun getTransactionById(id: Int): Flow<Transaction?> {
        return transactionDao.getTransactionById(id)
    }

    fun getTransactionsForAccount(accountId: Int): Flow<List<Transaction>> {
        return transactionDao.getTransactionsForAccount(accountId)
    }

    fun getSpendingForCategory(
        categoryName: String,
        startDate: Long,
        endDate: Long,
    ): Flow<Double?> {
        return transactionDao.getSpendingForCategory(categoryName, startDate, endDate)
    }

    fun getSpendingByCategoryForMonth(
        startDate: Long,
        endDate: Long,
    ): Flow<List<CategorySpending>> {
        return transactionDao.getSpendingByCategoryForMonth(startDate, endDate)
    }

    fun getMonthlyTrends(startDate: Long): Flow<List<MonthlyTrend>> {
        return transactionDao.getMonthlyTrends(startDate)
    }

    suspend fun countTransactionsForCategory(categoryId: Int): Int {
        return transactionDao.countTransactionsForCategory(categoryId)
    }

    fun getTagsForTransaction(transactionId: Int): Flow<List<Tag>> {
        return transactionDao.getTagsForTransaction(transactionId)
    }

    suspend fun insertTransactionWithTags(transaction: Transaction, tags: Set<Tag>) {
        val transactionId = transactionDao.insert(transaction).toInt()
        if (tags.isNotEmpty()) {
            val crossRefs = tags.map { tag ->
                TransactionTagCrossRef(transactionId = transactionId, tagId = tag.id)
            }
            transactionDao.addTagsToTransaction(crossRefs)
        }
    }

    suspend fun updateTransactionWithTags(transaction: Transaction, tags: Set<Tag>) {
        transactionDao.update(transaction)
        transactionDao.clearTagsForTransaction(transaction.id)
        if (tags.isNotEmpty()) {
            val crossRefs = tags.map { tag ->
                TransactionTagCrossRef(transactionId = transaction.id, tagId = tag.id)
            }
            transactionDao.addTagsToTransaction(crossRefs)
        }
    }

    suspend fun insert(transaction: Transaction) {
        transactionDao.insert(transaction)
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction)
    }
}
