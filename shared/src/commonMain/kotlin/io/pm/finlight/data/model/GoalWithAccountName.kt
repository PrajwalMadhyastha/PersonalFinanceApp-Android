package io.pm.finlight.data.model

import kotlinx.serialization.Serializable

/**
 * A DTO to hold a Goal and its associated account's name.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 */
@Serializable
data class GoalWithAccountName(
    val id: Int,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val targetDate: Long?,
    val accountId: Int,
    val accountName: String
)
