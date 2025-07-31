package io.pm.finlight.data.db.entity

import kotlinx.serialization.Serializable

/**
 * Represents a potential recurring transaction pattern identified from SMS messages.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 * @Serializable allows it to be used in JSON backups.
 */
@Serializable
data class RecurringPattern(
    val smsSignature: String,
    val description: String,
    val amount: Double,
    val transactionType: String,
    val accountId: Int,
    val categoryId: Int?,
    var occurrences: Int,
    val firstSeen: Long,
    var lastSeen: Long
)