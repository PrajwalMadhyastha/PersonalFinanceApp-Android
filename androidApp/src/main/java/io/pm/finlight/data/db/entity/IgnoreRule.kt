// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/IgnoreRule.kt
// REASON: FEATURE - The entity has been enhanced to support different rule
// types. A new `RuleType` enum (SENDER, BODY_PHRASE) and a corresponding `type`
// column have been added. The `phrase` column has been renamed to `pattern` to
// better reflect its new, more generic purpose.
// =================================================================================
package io.pm.finlight

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Enum to define the type of content an IgnoreRule should match against.
 */
enum class RuleType {
    SENDER,
    BODY_PHRASE
}

/**
 * Represents a user-defined rule to ignore an SMS.
 *
 * @param id The unique identifier for the rule.
 * @param type The type of rule (SENDER or BODY_PHRASE).
 * @param pattern The text pattern to match against (e.g., "*JioBal*" for sender, "invoice of" for body).
 * @param isEnabled Whether this rule is currently active.
 * @param isDefault True if this is a pre-populated rule, false if user-added.
 */
@Entity(
    tableName = "ignore_rules",
    indices = [Index(value = ["pattern"], unique = true)]
)
data class IgnoreRule(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: RuleType = RuleType.BODY_PHRASE,
    val pattern: String,
    var isEnabled: Boolean = true,
    val isDefault: Boolean = false
)
