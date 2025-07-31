package io.pm.finlight.data.db.entity

import kotlinx.serialization.Serializable

/**
 * Represents a user's budget for a specific category in a given month.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 * @Serializable allows it to be used in JSON backups.
 */
@Serializable
data class Budget(
    val id: Int = 0,
    val categoryName: String,
    val amount: Double,
    val month: Int, // e.g., 6 for June
    val year: Int, // e.g., 2024
)

