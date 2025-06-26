package io.pm.finlight

import androidx.room.Embedded

/**
 * A data class to hold the combined result of a database query.
 * It includes the full Transaction object, plus the names of the
 * linked Account and Category for easy display in the UI.
 */
data class TransactionDetails(
    @Embedded
    val transaction: Transaction,
    val accountName: String?,
    val categoryName: String?,
)
