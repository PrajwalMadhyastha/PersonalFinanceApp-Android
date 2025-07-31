package io.pm.finlight.data.model

import kotlinx.serialization.Serializable

/**
 * Enum representing different time periods for reports.
 */
@Serializable
enum class TimePeriod {
    DAILY,
    WEEKLY,
    MONTHLY
}
