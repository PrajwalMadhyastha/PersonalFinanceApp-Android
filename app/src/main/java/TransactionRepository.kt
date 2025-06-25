// =================================================================================
// FILE: /app/src/main/java/com/example/personalfinanceapp/TransactionRepository.kt
// PURPOSE: Centralizes data operations between ViewModels and the Database DAOs.
// NOTE: Added logging to trace database interactions.
// =================================================================================
package com.example.personalfinanceapp

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

class TransactionRepository(private val transactionDao: TransactionDao) {

    // DEBUG LOG: Added .onEach to see every new list emitted from the database flow.
    val allTransactions: Flow<List<TransactionDetails>> = transactionDao.getAllTransactions()
        .onEach { transactions ->
            Log.d("TransactionFlowDebug", "Repository Flow Emitted. Count: ${transactions.size}. Newest: ${transactions.firstOrNull()?.transaction?.description}")
        }

    fun getTransactionsForAccountDetails(accountId: Int): Flow<List<TransactionDetails>> {
        return transactionDao.getTransactionsForAccountDetails(accountId)
    }

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

    fun getSpendingByCategoryForMonth(startDate: Long, endDate: Long): Flow<List<CategorySpending>> {
        return transactionDao.getSpendingByCategoryForMonth(startDate, endDate)
    }

    fun getMonthlyTrends(startDate: Long): Flow<List<MonthlyTrend>> {
        return transactionDao.getMonthlyTrends(startDate)
    }

    suspend fun countTransactionsForCategory(categoryId: Int): Int {
        return transactionDao.countTransactionsForCategory(categoryId)
    }

    // DEBUG LOG: Log every time a transaction is inserted.
    suspend fun insert(transaction: Transaction) {
        Log.d("TransactionFlowDebug", "Repository: Inserting transaction '${transaction.description}'")
        transactionDao.insert(transaction)
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction)
    }
}
