// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/IgnoreRule.kt
// REASON: NEW FILE - This entity class defines the schema for the new
// `ignore_rules` table, which will store user-defined phrases to prevent
// non-transactional SMS messages from being parsed.
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
 */
@Entity(
    tableName = "ignore_rules",
    indices = [Index(value = ["phrase"], unique = true)]
)
data class IgnoreRule(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val phrase: String
)
