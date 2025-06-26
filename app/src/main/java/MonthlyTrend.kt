package io.pm.finlight

data class MonthlyTrend(
    val monthYear: String, // Format: "YYYY-MM"
    val totalIncome: Double,
    val totalExpenses: Double,
)
