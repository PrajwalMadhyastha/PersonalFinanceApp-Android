package io.pm.finlight

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantRenameRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: MerchantRenameRule)

    @Query("SELECT * FROM merchant_rename_rules")
    fun getAllRules(): Flow<List<MerchantRenameRule>>

    @Query("DELETE FROM merchant_rename_rules WHERE originalName = :originalName")
    suspend fun deleteByOriginalName(originalName: String)
}
