package io.pm.finlight.data.db.entity

import kotlinx.serialization.Serializable

/**
 * Represents a user's savings goal.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 * @Serializable allows it to be used in JSON backups.
 */
@Serializable
data class Goal(
    val id: Int = 0,
    val name: String,
    val targetAmount: Double,
    var savedAmount: Double,
    val targetDate: Long?,
    val accountId: Int
)

