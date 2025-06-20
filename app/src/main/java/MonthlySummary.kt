package com.example.personalfinanceapp

/**
 * A data class to hold the results of a GROUP BY query for monthly summaries.
 */
data class MonthlySummary(
    val year: Int,
    val month: Int,
    val totalAmount: Double
)
