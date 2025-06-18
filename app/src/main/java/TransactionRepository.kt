package com.example.personalfinanceapp

import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {

    val allTransactions: Flow<List<TransactionWithAccount>> = transactionDao.getAllTransactions()

    // Ensure this function exists
    fun getAllTransactionsSimple(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactionsSimple()
    }

    fun getTransactionById(id: Int): Flow<Transaction?> {
        return transactionDao.getTransactionById(id)
    }

    fun getTransactionsForAccount(accountId: Int): Flow<List<Transaction>> {
        return transactionDao.getTransactionsForAccount(accountId)
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
    fun getSpendingForCategory(categoryName: String, startDate: Long, endDate: Long): Flow<Double?> {
        return transactionDao.getSpendingForCategory(categoryName, startDate, endDate)
    }
}