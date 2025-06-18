package com.example.personalfinanceapp

class TransactionRepository {
}package com.example.personalfinanceapp

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
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    // The 'suspend' modifier tells us this is a long-running operation that
    // must be called from a coroutine.
    suspend fun insert(transaction: Transaction) {
        transactionDao.insert(transaction)
    }
}