package io.pm.finlight

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object (DAO) for the MerchantCategoryMapping entity.
 */
@Dao
interface MerchantCategoryMappingDao {

    /**
     * Inserts a new merchant-category mapping. If a mapping for the merchant
     * already exists, it will be replaced with the new one.
     *
     * @param mapping The MerchantCategoryMapping object to insert or update.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mapping: MerchantCategoryMapping)

    /**
     * Retrieves the learned category ID for a given merchant name.
     *
     * @param parsedName The name of the merchant to look up.
     * @return The associated category ID (Int), or null if no mapping exists.
     */
    @Query("SELECT categoryId FROM merchant_category_mapping WHERE parsedName = :parsedName")
    suspend fun getCategoryIdForMerchant(parsedName: String): Int?
}
