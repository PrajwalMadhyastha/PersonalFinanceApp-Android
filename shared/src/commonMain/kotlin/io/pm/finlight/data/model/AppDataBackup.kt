package io.pm.finlight.data.model

import io.pm.finlight.data.db.entity.Account
import io.pm.finlight.data.db.entity.Budget
import io.pm.finlight.data.db.entity.Category
import io.pm.finlight.data.db.entity.MerchantMapping
import io.pm.finlight.data.db.entity.SplitTransaction
import io.pm.finlight.data.db.entity.Transaction
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
    val splitTransactions: List<SplitTransaction> = emptyList()
)
