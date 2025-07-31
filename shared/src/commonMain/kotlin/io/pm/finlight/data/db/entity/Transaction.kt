package io.pm.finlight.data.db.entity

import kotlinx.serialization.Serializable

/**
 * Represents a single financial transaction.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 * @Serializable allows it to be used in JSON backups.
 */
@Serializable
data class Transaction(
    val id: Int = 0,
    val description: String,
    val categoryId: Int?,
    val amount: Double, // ALWAYS in home currency
    val date: Long,
    val accountId: Int,
    val notes: String?,
    val transactionType: String = "expense",
    val sourceSmsId: Long? = null,
    val sourceSmsHash: String? = null,
    val source: String = "Manual Entry",
    val originalDescription: String? = null,
    val isExcluded: Boolean = false,
    val smsSignature: String? = null,
    val originalAmount: Double? = null,
    val currencyCode: String? = null,
    val conversionRate: Double? = null,
    val isSplit: Boolean = false
)