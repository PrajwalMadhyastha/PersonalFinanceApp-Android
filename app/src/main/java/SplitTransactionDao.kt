// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/SplitTransactionDao.kt
// REASON: NEW FILE - This DAO provides all necessary database operations for the
// SplitTransaction entity, including inserting, updating, and deleting split
// items, as well as clearing all splits associated with a parent transaction.
// =================================================================================
package io.pm.finlight

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SplitTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(splits: List<SplitTransaction>)

    @Update
    suspend fun update(split: SplitTransaction)

    @Query("DELETE FROM split_transactions WHERE id = :splitId")
    suspend fun deleteById(splitId: Int)

    @Query("DELETE FROM split_transactions WHERE parentTransactionId = :parentTransactionId")
    suspend fun deleteSplitsForParent(parentTransactionId: Int)
}
