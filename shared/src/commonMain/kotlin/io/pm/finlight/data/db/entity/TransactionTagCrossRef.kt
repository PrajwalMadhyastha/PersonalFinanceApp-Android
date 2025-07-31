package io.pm.finlight.data.db.entity

import kotlinx.serialization.Serializable

/**
 * A "join table" entity for the many-to-many relationship between Transactions and Tags.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 * @Serializable allows it to be used in JSON backups.
 */
@Serializable
data class TransactionTagCrossRef(
    val transactionId: Int,
    val tagId: Int
)

