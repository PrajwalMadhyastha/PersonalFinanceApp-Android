// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/IgnoreRule.kt
// REASON: FEATURE - The entity has been updated with `isEnabled` and `isDefault`
// fields. This allows the app to distinguish between pre-populated default
// rules and user-added rules, and gives users the ability to toggle the
// default rules on or off without deleting them.
// =================================================================================
package io.pm.finlight

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a user-defined rule to ignore an SMS based on a specific phrase.
 * If this phrase is found in an SMS body, the parser will skip it.
 *
 * @param id The unique identifier for the rule.
 * @param phrase The text that, if found, will cause the SMS to be ignored (e.g., "invoice of").
 * @param isEnabled Whether this rule is currently active.
 * @param isDefault True if this is a pre-populated rule, false if user-added.
 */
@Entity(
    tableName = "ignore_rules",
    indices = [Index(value = ["phrase"], unique = true)]
)
data class IgnoreRule(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val phrase: String,
    var isEnabled: Boolean = true,
    val isDefault: Boolean = false
)
