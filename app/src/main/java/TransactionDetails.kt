package io.pm.finlight

import androidx.room.Embedded

/**
 * A data class to hold the combined result of a database query.
 * It includes the full Transaction object, plus the names, icon, and color
 * of the linked Account and Category for easy display in the UI.
 */
data class TransactionDetails(
    @Embedded
    val transaction: Transaction,
    val accountName: String?,
    val categoryName: String?,
    // --- NEW: Add fields for category icon and color ---
    val categoryIconKey: String?,
    val categoryColorKey: String?
)
