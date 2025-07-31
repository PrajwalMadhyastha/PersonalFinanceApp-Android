// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/data/model/AppDataBackup.kt
// REASON: FEATURE - The backup data model has been updated to include a list of
// `SplitTransaction` entities. This ensures that detailed split transaction
// data is correctly included in JSON backups and restores.
// =================================================================================
package io.pm.finlight

import kotlinx.serialization.Serializable

/**
 * A top-level container for all application data to be exported.
 * This class is designed to be easily converted to a single JSON object.
 */
@Serializable
data class AppDataBackup(
    val transactions: List<Transaction>,
    val accounts: List<Account>,
    val categories: List<Category>,
    val budgets: List<Budget>,
    val merchantMappings: List<MerchantMapping>,
    // --- NEW: Add split transactions to the backup model ---
    val splitTransactions: List<SplitTransaction> = emptyList()
)
