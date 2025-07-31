package io.pm.finlight.data.db.entity

import kotlinx.serialization.Serializable

/**
 * Represents a single item within a split transaction.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 * @Serializable allows it to be used in JSON backups.
 */
@Serializable
data class SplitTransaction(
    val id: Int = 0,
    val parentTransactionId: Int,
    val amount: Double, // ALWAYS in home currency
    val categoryId: Int?,
    val notes: String?,
    val originalAmount: Double? = null
)

