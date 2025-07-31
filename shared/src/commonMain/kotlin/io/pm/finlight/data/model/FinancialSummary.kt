package io.pm.finlight.data.model

import kotlinx.serialization.Serializable

/**
 * A DTO to hold a financial summary, typically for a specific date range.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 */
@Serializable
data class FinancialSummary(
    val totalIncome: Double,
    val totalExpenses: Double
)
