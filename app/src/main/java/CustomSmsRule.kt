// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/CustomSmsRule.kt
// REASON: ARCHITECTURAL REFACTOR - The entity has been completely redesigned to
// support a more robust, trigger-based parsing system. It no longer relies on
// the fragile smsSender. Instead, it uses a stable 'triggerPhrase' from the
// SMS body to identify when a rule should be applied. It also consolidates
// merchant and amount patterns into a single, more efficient rule object.
// =================================================================================
package io.pm.finlight

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a user-defined parsing rule that is independent of the SMS sender.
 * The rule is activated when its 'triggerPhrase' is found within an SMS body.
 *
 * @param id The unique identifier for the rule.
 * @param triggerPhrase A stable, unique piece of text from an SMS that identifies
 * when this rule should be applied (e.g., "spent on your SBI Credit Card").
 * @param merchantRegex The regex pattern to extract the merchant name. Can be null.
 * @param amountRegex The regex pattern to extract the transaction amount. Can be null.
 * @param priority The execution priority. Higher numbers are checked first.
 */
@Entity(
    tableName = "custom_sms_rules",
    indices = [Index(value = ["triggerPhrase"], unique = true)]
)
data class CustomSmsRule(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val triggerPhrase: String,
    val merchantRegex: String?,
    val amountRegex: String?,
    val priority: Int
)
