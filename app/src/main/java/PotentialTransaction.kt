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
 */
data class PotentialTransaction(
    val sourceSmsId: Long,
    val smsSender: String,
    val amount: Double,
    val transactionType: String,
    val merchantName: String?,
    val originalMessage: String,
    // --- NEW: Add a field for the automatically parsed account details ---
    val potentialAccount: PotentialAccount? = null,
    // --- BUG FIX: Add a stable hash to uniquely identify an SMS for de-duplication ---
    val sourceSmsHash: String? = null,
)
