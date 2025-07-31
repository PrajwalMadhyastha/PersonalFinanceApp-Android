package io.pm.finlight.data.db.entity

import kotlinx.serialization.Serializable

/**
 * Stores a user-defined mapping between a parsed merchant name and a specific category.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 * @Serializable allows it to be used in JSON backups.
 */
@Serializable
data class MerchantCategoryMapping(
    val parsedName: String,
    val categoryId: Int
)