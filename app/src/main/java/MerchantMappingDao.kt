package io.pm.finlight

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantMappingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mapping: MerchantMapping)

    // --- NEW: Function to insert a list of mappings during import ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mappings: List<MerchantMapping>)

    // --- NEW: Function to clear the table during import ---
    @Query("DELETE FROM merchant_mappings")
    suspend fun deleteAll()

    @Query("SELECT * FROM merchant_mappings")
    fun getAllMappings(): Flow<List<MerchantMapping>>

    @Query("SELECT * FROM merchant_mappings WHERE smsSender = :sender")
    suspend fun getMappingForSender(sender: String): MerchantMapping?
}
