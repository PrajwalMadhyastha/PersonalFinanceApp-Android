// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/MerchantRenameRule.kt
// REASON: REFACTOR - The `originalName` primary key has been updated with
// `collate = NOCASE`. This makes the rule matching case-insensitive, so a rule
// for "zomato" will correctly apply to transactions parsed as "Zomato".
// =================================================================================
package io.pm.finlight

import androidx.room.ColumnInfo
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
    // --- UPDATED: Make the primary key case-insensitive ---
    @PrimaryKey
    @ColumnInfo(name = "originalName", collate = ColumnInfo.NOCASE)
    val originalName: String,
    val newName: String
)