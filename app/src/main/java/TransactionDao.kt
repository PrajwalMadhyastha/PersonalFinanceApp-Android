package com.example.personalfinanceapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    /**
     * Inserts a new transaction into the database. The 'suspend' keyword means
     * this function must be called from a coroutine, as database operations
     * cannot block the main UI thread.
     */
    @Insert
    suspend fun insert(transaction: Transaction)

    /**
     * Retrieves all transactions from the 'transactions' table, ordering them
     * by date with the newest first.
     * It returns a Flow, which is a reactive data stream. The UI can observe
     * this Flow and will automatically update itself whenever the data in the
     * table changes.
     */
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    // We can add more queries here later, like update, delete, or get by ID.
}