package com.example.personalfinanceapp

import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {

    val allTransactions: Flow<List<TransactionDetails>> = transactionDao.getAllTransactions()

    fun getTransactionDetailsForRange(startDate: Long, endDate: Long): Flow<List<TransactionDetails>> {
        return transactionDao.getTransactionDetailsForRange(startDate, endDate)
    }

    fun getAllTransactionsSimple(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactionsSimple()
    }

    fun getAllTransactionsForRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getAllTransactionsForRange(startDate, endDate)
    }

    fun getTransactionById(id: Int): Flow<Transaction?> {
        return transactionDao.getTransactionById(id)
    }

    fun getTransactionsForAccount(accountId: Int): Flow<List<Transaction>> {
        return transactionDao.getTransactionsForAccount(accountId)
    }

    fun getSpendingForCategory(categoryName: String, startDate: Long, endDate: Long): Flow<Double?> {
        return transactionDao.getSpendingForCategory(categoryName, startDate, endDate)
    }

    // --- NEW: Expose the new spending by category function ---
    fun getSpendingByCategoryForMonth(startDate: Long, endDate: Long): Flow<List<CategorySpending>> {
        return transactionDao.getSpendingByCategoryForMonth(startDate, endDate)
    }
    fun getMonthlyTrends(startDate: Long): Flow<List<MonthlyTrend>> {
        return transactionDao.getMonthlyTrends(startDate)
    }

    suspend fun countTransactionsForCategory(categoryId: Int): Int {
        return transactionDao.countTransactionsForCategory(categoryId)
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
