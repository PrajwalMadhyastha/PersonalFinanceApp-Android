package io.pm.finlight.data.model

import kotlinx.serialization.Serializable

/**
 * Holds the processed status and data for a single day in the consistency calendar.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 * Note: The 'Date' object is replaced with a 'timestamp' (Long) to remove platform dependencies.
 */
@Serializable
data class CalendarDayStatus(
    val timestamp: Long,
    val status: SpendingStatus,
    val amountSpent: Double,
    val safeToSpend: Double
)
