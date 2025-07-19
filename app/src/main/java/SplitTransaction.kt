// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/SplitTransaction.kt
// REASON: FEATURE (Travel Mode Splitting) - Added a new nullable `originalAmount`
// column. This will store the split amount in the foreign currency if the parent
// transaction was made in Travel Mode, allowing the UI to display the correct
// values while the `amount` column always stores the home currency value.
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
    val amount: Double, // ALWAYS in home currency
    val categoryId: Int?,
    val notes: String?,
    // --- NEW: Store the amount in the original foreign currency, if applicable ---
    val originalAmount: Double? = null
)
