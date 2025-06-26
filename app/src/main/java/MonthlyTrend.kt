package com.example.personalfinanceapp

data class MonthlyTrend(
    val monthYear: String, // Format: "YYYY-MM"
    val totalIncome: Double,
    val totalExpenses: Double,
)
