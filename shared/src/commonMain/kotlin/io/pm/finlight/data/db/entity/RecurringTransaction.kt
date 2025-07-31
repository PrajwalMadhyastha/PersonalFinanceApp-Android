package io.pm.finlight.data.db.entity

import kotlinx.serialization.Serializable

/**
 * Represents a rule for a recurring transaction (e.g., monthly bill, weekly income).
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 * @Serializable allows it to be used in JSON backups.
 */
@Serializable
data class RecurringTransaction(
    val id: Int = 0,
    val description: String,
    val amount: Double,
    val transactionType: String, // "income" or "expense"
    val recurrenceInterval: String, // e.g., "Daily", "Weekly", "Monthly", "Yearly"
    val startDate: Long, // Timestamp for the first occurrence
    val accountId: Int,
    val categoryId: Int?,
    val lastRunDate: Long? = null
)

