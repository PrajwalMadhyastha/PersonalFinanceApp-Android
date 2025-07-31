package io.pm.finlight.data.db.entity

import kotlinx.serialization.Serializable

/**
 * Represents a user-defined rule for parsing specific SMS formats.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 * @Serializable allows it to be used in JSON backups.
 */
@Serializable
data class CustomSmsRule(
    val id: Int = 0,
    val triggerPhrase: String,
    val merchantRegex: String?,
    val amountRegex: String?,
    val accountRegex: String?,
    val merchantNameExample: String?,
    val amountExample: String?,
    val accountNameExample: String?,
    val priority: Int,
    val sourceSmsBody: String
)