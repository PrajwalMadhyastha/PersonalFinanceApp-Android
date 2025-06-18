package com.example.personalfinanceapp

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: Transaction)

    // --- ADD THESE NEW FUNCTIONS ---

    /**
     * Updates an existing transaction in the database.
     */
    @Update
    suspend fun update(transaction: Transaction)

    /**
     * Deletes a transaction from the database.
     */
    @Delete
    suspend fun delete(transaction: Transaction)

    /**
     * Retrieves a single transaction by its ID. Returns a Flow so the UI
     * can observe changes to this specific item.
     */
    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Int): Flow<Transaction?> // The '?' makes it nullable

    // --- END OF NEW FUNCTIONS ---

    @Query("SELECT transactions.*, accounts.name as accountName FROM transactions LEFT JOIN accounts ON transactions.accountId = accounts.id ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionWithAccount>>
}