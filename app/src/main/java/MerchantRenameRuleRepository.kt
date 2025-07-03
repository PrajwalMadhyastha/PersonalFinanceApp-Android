// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/MerchantRenameRuleRepository.kt
// REASON: NEW FILE - This repository centralizes access to merchant renaming
// rules. It provides a clean, reusable way for different ViewModels to get all
// aliases as a simple Map, avoiding code duplication and promoting a clean
// architecture.
// =================================================================================
package io.pm.finlight

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository that abstracts access to the MerchantRenameRule data source.
 */
class MerchantRenameRuleRepository(private val dao: MerchantRenameRuleDao) {
    /**
     * Retrieves all rename rules from the database.
     */
    fun getAllRules(): Flow<List<MerchantRenameRule>> = dao.getAllRules()

    /**
     * Retrieves all rename rules and transforms them into a key-value map
     * for efficient lookups at display time.
     * @return A Flow emitting a Map where the key is the original name and the value is the new name.
     */
    fun getAliasesAsMap(): Flow<Map<String, String>> {
        return dao.getAllRules().map { rules ->
            rules.associate { it.originalName to it.newName }
        }
    }
}
