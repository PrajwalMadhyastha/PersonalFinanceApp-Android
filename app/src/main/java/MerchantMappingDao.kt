package com.example.personalfinanceapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the merchant_mappings table.
 */
@Dao
interface MerchantMappingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mapping: MerchantMapping)

    @Query("SELECT * FROM merchant_mappings")
    fun getAllMappings(): Flow<List<MerchantMapping>>

    @Query("SELECT * FROM merchant_mappings WHERE smsSender = :sender")
    suspend fun getMappingForSender(sender: String): MerchantMapping?
}
