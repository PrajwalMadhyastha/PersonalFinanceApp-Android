package io.pm.finlight.data.db.entity

import kotlinx.serialization.Serializable

/**
 * Defines a rule to rename a parsed merchant name to a more user-friendly one.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 * @Serializable allows it to be used in JSON backups.
 */
@Serializable
data class MerchantRenameRule(
    val originalName: String,
    val newName: String
)

