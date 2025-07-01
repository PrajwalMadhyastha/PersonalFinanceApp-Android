package io.pm.finlight

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a custom parsing rule created by the user to improve SMS transaction detection.
 * Each rule is scoped to a specific SMS sender and has a priority to determine its execution order.
 *
 * @param id The unique identifier for the rule.
 * @param smsSender The SMS sender address (e.g., "AM-HDFCBK") this rule applies to.
 * @param ruleType The type of data this rule is designed to extract (e.g., "MERCHANT", "AMOUNT").
 * @param regexPattern The regular expression used to find and extract the data from the SMS body.
 * @param priority The execution priority of the rule. Higher numbers are executed first.
 */
@Entity(tableName = "custom_sms_rules")
data class CustomSmsRule(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val smsSender: String,
    val ruleType: String,
    val regexPattern: String,
    val priority: Int
)
