package io.pm.finlight.data.model

import io.pm.finlight.data.db.entity.Budget
import kotlinx.serialization.Serializable

/**
 * A DTO that combines a Budget with the actual amount spent in that category for the period.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 */
@Serializable
data class BudgetWithSpending(
    val budget: Budget,
    val spent: Double,
    val iconKey: String?,
    val colorKey: String?
)
