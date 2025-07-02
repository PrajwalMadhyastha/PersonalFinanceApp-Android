package io.pm.finlight

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores a user-defined rule to rename a parsed merchant name to a more
 * user-friendly one.
 * @param originalName The name originally extracted by the parser's regex.
 * @param newName The name the user wants to see instead.
 */
@Entity(tableName = "merchant_rename_rules")
data class MerchantRenameRule(
    @PrimaryKey
    val originalName: String,
    val newName: String
)
