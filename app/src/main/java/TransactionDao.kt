package com.example.personalfinanceapp

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT transactions.*, accounts.name as accountName FROM transactions LEFT JOIN accounts ON transactions.accountId = accounts.id ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionWithAccount>>

    // --- ADD THIS FUNCTION ---
    @Query("SELECT * FROM transactions")
    fun getAllTransactionsSimple(): Flow<List<Transaction>>
    // --- END OF ADDITION ---

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Int): Flow<Transaction?>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getTransactionsForAccount(accountId: Int): Flow<List<Transaction>>

    @Insert
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)
}