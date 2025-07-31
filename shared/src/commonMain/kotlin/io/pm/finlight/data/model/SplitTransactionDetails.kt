package io.pm.finlight.data.model

import io.pm.finlight.data.db.entity.SplitTransaction
import kotlinx.serialization.Serializable

/**
 * A DTO that combines a SplitTransaction with its corresponding Category details.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 */
@Serializable
data class SplitTransactionDetails(
    val splitTransaction: SplitTransaction,
    val categoryName: String?,
    val categoryIconKey: String?,
    val categoryColorKey: String?
)
