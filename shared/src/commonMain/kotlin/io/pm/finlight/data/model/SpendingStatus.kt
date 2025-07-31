package io.pm.finlight.data.model

import kotlinx.serialization.Serializable

/**
 * Represents the spending status for a single day relative to the daily budget.
 */
@Serializable
enum class SpendingStatus {
    NO_SPEND,
    WITHIN_LIMIT,
    OVER_LIMIT,
    NO_DATA
}