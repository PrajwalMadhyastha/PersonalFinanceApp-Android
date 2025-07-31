package io.pm.finlight.data.db.entity

import kotlinx.serialization.Serializable

/**
 * Represents a transaction category (e.g., Food, Groceries, Bills).
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 * @Serializable allows it to be used in JSON backups.
 */
@Serializable
data class Category(
    val id: Int = 0,
    val name: String,
    val iconKey: String = "category",
    val colorKey: String = "gray"
)

