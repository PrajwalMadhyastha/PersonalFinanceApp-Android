package com.example.personalfinanceapp

import kotlinx.coroutines.flow.Flow

/**
 * A repository class that abstracts access to the transaction data source.
 * In a more complex app, this might handle fetching data from a network
 * and saving it to the local database. For now, it's a simple pass-through
 * to our Room DAO.
 *
 * @param transactionDao The Data Access Object for transactions.
 */
class TransactionRepository(private val transactionDao: TransactionDao) {

    // Exposes the Flow of all transactions directly from the DAO.
    // The ViewModel will observe this Flow.
    val allTransactions: Flow<List<TransactionWithAccount>> = transactionDao.getAllTransactions()

    // --- ADD THESE NEW FUNCTIONS ---

    fun getTransactionById(id: Int): Flow<Transaction?> {
        return transactionDao.getTransactionById(id)
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction)
    }

    // --- END OF NEW FUNCTIONS ---

    suspend fun insert(transaction: Transaction) {
        transactionDao.insert(transaction)
    }
}