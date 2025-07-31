// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/SplitTransactionDao.kt
// REASON: FEATURE (Splitting) - Added a new `getSplitsForParent` query. This
// query joins the split_transactions table with the categories table to fetch
// each split item along with its full category details, which is necessary for
// displaying them in the UI.
// =================================================================================
package io.pm.finlight

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SplitTransactionDao {

    @Transaction
    @Query("""
        SELECT s.*, c.name as categoryName, c.iconKey as categoryIconKey, c.colorKey as categoryColorKey
        FROM split_transactions s
        LEFT JOIN categories c ON s.categoryId = c.id
        WHERE s.parentTransactionId = :parentTransactionId
        ORDER BY s.amount DESC
    """)
    fun getSplitsForParent(parentTransactionId: Int): Flow<List<SplitTransactionDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(splits: List<SplitTransaction>)

    @Update
    suspend fun update(split: SplitTransaction)

    @Query("DELETE FROM split_transactions WHERE id = :splitId")
    suspend fun deleteById(splitId: Int)

    @Query("DELETE FROM split_transactions WHERE parentTransactionId = :parentTransactionId")
    suspend fun deleteSplitsForParent(parentTransactionId: Int)
}
