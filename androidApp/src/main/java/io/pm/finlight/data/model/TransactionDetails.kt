package io.pm.finlight

import androidx.room.Embedded
import androidx.room.Relation

data class TransactionDetails(
    @Embedded
    val transaction: Transaction,
    @Relation(
        parentColumn = "id",
        entityColumn = "transactionId"
    )
    val images: List<TransactionImage>, // --- NEW: Add relation to images ---
    val accountName: String?,
    val categoryName: String?,
    val categoryIconKey: String?,
    val categoryColorKey: String?
)
