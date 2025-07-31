package io.pm.finlight.data.model

import kotlinx.serialization.Serializable

/**
 * A DTO to hold the results of a GROUP BY query, containing the name of a category,
 * its visual identifiers, and the total amount spent in it.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 */
@Serializable
data class CategorySpending(
    val categoryName: String,
    val totalAmount: Double,
    val colorKey: String?,
    val iconKey: String?
)
