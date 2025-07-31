package io.pm.finlight.data.model

import kotlinx.serialization.Serializable

/**
 * A DTO to hold aggregated spending data for a specific merchant.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 */
@Serializable
data class MerchantSpendingSummary(
    val merchantName: String,
    val totalAmount: Double,
    val transactionCount: Int
)
