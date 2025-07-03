package io.pm.finlight

/**
 * Repository that abstracts access to the MerchantCategoryMapping data source.
 * This provides a clean API for the ViewModel to interact with the learning feature.
 */
class MerchantCategoryMappingRepository(private val dao: MerchantCategoryMappingDao) {

    /**
     * Inserts or updates a merchant-category mapping.
     *
     * @param mapping The mapping to save.
     */
    suspend fun insert(mapping: MerchantCategoryMapping) {
        dao.insert(mapping)
    }

    /**
     * Retrieves the learned category ID for a given merchant name.
     *
     * @param parsedName The name of the merchant.
     * @return The associated category ID, or null if none is found.
     */
    suspend fun getCategoryIdForMerchant(parsedName: String): Int? {
        return dao.getCategoryIdForMerchant(parsedName)
    }
}
