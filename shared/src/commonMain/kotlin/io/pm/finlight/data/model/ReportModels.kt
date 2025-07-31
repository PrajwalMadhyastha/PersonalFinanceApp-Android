package io.pm.finlight.data.model

import kotlinx.serialization.Serializable

/**
 * Enum to represent the selectable time periods on the Reports screen.
 */
@Serializable
enum class ReportPeriod(val displayName: String) {
    WEEK("This Week"),
    MONTH("This Month"),
    QUARTER("3 Months"),
    ALL_TIME("All Time")
}

/**
 * DTO to hold key insights calculated for the selected period.
 */
@Serializable
data class ReportInsights(
    val percentageChange: Int?,
    val topCategory: CategorySpending?
)

/**
 * DTO to hold all the computed data for the reports screen.
 * Note: Android-specific chart data types have been replaced with platform-agnostic data lists.
 */
@Serializable
data class ReportScreenData(
    val categorySpending: List<CategorySpending>,
    val monthlyTrends: List<MonthlyTrend>,
    val periodTitle: String,
    val insights: ReportInsights?
)
