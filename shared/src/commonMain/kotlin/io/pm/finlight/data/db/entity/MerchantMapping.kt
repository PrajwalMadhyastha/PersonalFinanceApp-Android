package io.pm.finlight.data.db.entity

import kotlinx.serialization.Serializable

/**
 * Maps an SMS sender address to a user-friendly merchant name.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 * @Serializable allows it to be used in JSON backups.
 */
@Serializable
data class MerchantMapping(
    val smsSender: String, // e.g., "AM-HDFCBK"
    val merchantName: String, // e.g., "McDonald's"
)

