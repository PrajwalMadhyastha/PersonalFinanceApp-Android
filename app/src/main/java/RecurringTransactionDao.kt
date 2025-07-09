// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/RecurringTransactionDao.kt
// REASON: FEATURE - Added the `updateLastRunDate` function, which the worker will
// call after successfully creating a transaction from a rule. Also renamed `getAll`
// to `getAllRules` for clarity and consistency with the worker's implementation.
// REASON: FIX - Added a new suspend function `getAllRulesList()` for the worker to
// fetch a one-time list, resolving compilation errors. The original Flow-based
// function was renamed to `getAllRulesFlow()` for clarity.
// =================================================================================
package io.pm.finlight

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
    fun getAllRulesFlow(): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions")
    suspend fun getAllRulesList(): List<RecurringTransaction>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    fun getById(id: Int): Flow<RecurringTransaction?>

    @Query("UPDATE recurring_transactions SET lastRunDate = :lastRunDate WHERE id = :id")
    suspend fun updateLastRunDate(id: Int, lastRunDate: Long)
}
