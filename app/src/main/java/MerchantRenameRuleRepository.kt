// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/MerchantRenameRuleRepository.kt
// REASON: FIX - The unused `getAllRules` function has been removed to resolve
// the "UnusedSymbol" warning, cleaning up the repository's public API.
// =================================================================================
package io.pm.finlight

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository that abstracts access to the MerchantRenameRule data source.
 */
class MerchantRenameRuleRepository(private val dao: MerchantRenameRuleDao) {
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
