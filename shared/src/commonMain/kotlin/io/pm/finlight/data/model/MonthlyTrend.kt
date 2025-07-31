package io.pm.finlight.data.model

import kotlinx.serialization.Serializable

/**
 * A DTO to hold monthly trend data for income vs. expenses.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 */
@Serializable
data class MonthlyTrend(
    val monthYear: String, // Format: "YYYY-MM"
    val totalIncome: Double,
    val totalExpenses: Double,
)
