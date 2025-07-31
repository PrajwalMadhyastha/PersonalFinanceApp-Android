package io.pm.finlight.data.db.entity

import kotlinx.serialization.Serializable

/**
 * Represents a user's financial account (e.g., Savings, Credit Card).
 *
 * This is a pure Kotlin data class, making it compatible with all platforms (Android, iOS, etc.).
 * The @Serializable annotation allows it to be easily converted to/from JSON for backups.
 *
 * Database-specific annotations like @Entity are handled by the database implementation
 * on each platform (e.g., Room on Android, SwiftData on iOS).
 */
@Serializable
data class Account(
    val id: Int = 0,
    val name: String,
    val type: String,
)

