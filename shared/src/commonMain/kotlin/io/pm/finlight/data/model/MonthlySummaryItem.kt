package io.pm.finlight.data.model

import kotlinx.serialization.Serializable

/**
 * A DTO to hold a summary for a specific month, used in month scrollers.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 * Note: The 'Calendar' object is replaced with a 'monthTimestamp' (Long)
 * to remove Android-specific dependencies.
 */
@Serializable
data class MonthlySummaryItem(
    val monthTimestamp: Long,
    val totalSpent: Double
)
