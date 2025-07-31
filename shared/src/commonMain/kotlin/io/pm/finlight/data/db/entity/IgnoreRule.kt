package io.pm.finlight.data.db.entity

import kotlinx.serialization.Serializable

/**
 * Enum to define the type of content an IgnoreRule should match against.
 */
@Serializable
enum class RuleType {
    SENDER,
    BODY_PHRASE
}

/**
 * Represents a user-defined rule to ignore an SMS during parsing.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 * @Serializable allows it to be used in JSON backups.
 */
@Serializable
data class IgnoreRule(
    val id: Int = 0,
    val type: RuleType = RuleType.BODY_PHRASE,
    val pattern: String,
    var isEnabled: Boolean = true,
    val isDefault: Boolean = false
)