package com.example.personalfinanceapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurringTransaction: RecurringTransaction)

    @Update
    suspend fun update(recurringTransaction: RecurringTransaction)

    @Delete
    suspend fun delete(recurringTransaction: RecurringTransaction)

    @Query("SELECT * FROM recurring_transactions ORDER BY startDate DESC")
    fun getAll(): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    fun getById(id: Int): Flow<RecurringTransaction?>
}
