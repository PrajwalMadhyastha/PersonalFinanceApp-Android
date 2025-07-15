// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/PotentialTransaction.kt
// REASON: FEATURE - A new nullable `categoryId` field has been added. This
// allows the SmsParser to include the ID of a learned category when it creates
// a potential transaction, enabling automatic categorization.
// =================================================================================
package io.pm.finlight

/**
 * A data class to hold the structured information extracted from an SMS message.
 * This is a temporary object, created before a full 'Transaction' is saved to the database.
 *
 * @param amount The monetary value of the transaction.
 * @param transactionType The type of transaction, either 'expense' or 'income'.
 * @param merchantName The name of the merchant, if it can be determined.
 * @param originalMessage The original SMS body, for reference and debugging.
 * @param potentialAccount Holds the parsed account name and type, if found.
 * @param categoryId The ID of a learned category, if a mapping exists for the merchant.
 */
data class PotentialTransaction(
    val sourceSmsId: Long,
    val smsSender: String,
    val amount: Double,
    val transactionType: String,
    val merchantName: String?,
    val originalMessage: String,
    val potentialAccount: PotentialAccount? = null,
    val sourceSmsHash: String? = null,
    // --- NEW: Add field for learned category ---
    val categoryId: Int? = null
)