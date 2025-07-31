package io.pm.finlight.data.model

import kotlinx.serialization.Serializable

/**
 * A DTO to hold the results of a GROUP BY query, containing a date string
 * and the total amount spent on that day.
 */
@Serializable
data class DailyTotal(
    val date: String, // Format: "YYYY-MM-DD"
    val totalAmount: Double
)
