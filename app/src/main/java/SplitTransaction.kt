// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/SplitTransaction.kt
// REASON: NEW FILE - Defines the Room entity for a single split item. This is
// the "child" in our parent-child data model. It holds its own amount and
// category, and is linked back to the original parent transaction via a
// foreign key.
// =================================================================================
package io.pm.finlight

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "split_transactions",
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["id"],
            childColumns = ["parentTransactionId"],
            onDelete = ForeignKey.CASCADE // If the parent is deleted, its splits are also deleted.
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL // If a category is deleted, the split remains but uncategorized.
        )
    ],
    indices = [
        Index(value = ["parentTransactionId"]),
        Index(value = ["categoryId"])
    ]
)
data class SplitTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val parentTransactionId: Int,
    val amount: Double,
    val categoryId: Int?,
    val notes: String?
)
