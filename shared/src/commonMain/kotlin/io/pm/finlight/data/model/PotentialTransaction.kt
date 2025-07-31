package io.pm.finlight.data.model

import kotlinx.serialization.Serializable

/**
 * A DTO to hold the structured information extracted from an SMS message.
 * This is a temporary object, created before a full 'Transaction' is saved to the database.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 */
@Serializable
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
    val detectedCurrencyCode: String? = null
)
