package io.pm.finlight.data.db.entity

import kotlinx.serialization.Serializable

/**
 * Represents a user-defined Tag (e.g., "Work Trip", "Vacation 2025").
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 * @Serializable allows it to be used in JSON backups.
 */
@Serializable
data class Tag(
    val id: Int = 0,
    val name: String
)

