// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/PotentialTransaction.kt
// REASON: FEATURE - Added a new nullable `detectedCurrencyCode` field. This will
// be populated by the SmsParser if it can confidently identify a currency code
// (e.g., "INR", "MYR") next to the amount in an SMS, forming the basis for the
// new smart currency detection feature.
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
 * @param smsSignature A stable hash of the SMS body used for pattern detection.
 * @param isForeignCurrency A flag passed from the notification to indicate user's currency choice.
 * @param detectedCurrencyCode The currency code (e.g., "INR", "USD") found in the SMS.
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
    val categoryId: Int? = null,
    val smsSignature: String? = null,
    val isForeignCurrency: Boolean? = null,
    // --- NEW: Field to store the currency code found in the SMS ---
    val detectedCurrencyCode: String? = null
)
