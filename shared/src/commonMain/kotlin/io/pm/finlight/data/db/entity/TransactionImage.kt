package io.pm.finlight.data.db.entity

import kotlinx.serialization.Serializable

/**
 * Represents an image file attached to a transaction.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 * @Serializable allows it to be used in JSON backups.
 */
@Serializable
data class TransactionImage(
    val id: Int = 0,
    val transactionId: Int,
    val imageUri: String
)

