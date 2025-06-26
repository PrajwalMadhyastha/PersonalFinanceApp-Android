package com.example.personalfinanceapp

/**
 * A simple data class to hold the results of a GROUP BY query,
 * containing the name of a category and the total amount spent in it.
 */
data class CategorySpending(
    val categoryName: String,
    val totalAmount: Double,
)
