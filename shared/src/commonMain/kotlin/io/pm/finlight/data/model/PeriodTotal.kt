package io.pm.finlight.data.model

import kotlinx.serialization.Serializable

/**
 * A DTO to hold the results of a GROUP BY query for weekly or monthly totals.
 */
@Serializable
data class PeriodTotal(
    val period: String,
    val totalAmount: Double
)
