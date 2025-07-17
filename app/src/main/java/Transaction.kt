// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/Transaction.kt
// REASON: FEATURE - Added a new nullable `smsSignature` field. This will store
// the normalized hash of the original SMS message, which is the key to identifying
// potential recurring transactions automatically. An index is added for efficient lookups.
// =================================================================================
package io.pm.finlight

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["accountId"]),
        Index(value = ["smsSignature"]) // --- NEW: Index for faster lookups
    ],
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val description: String,
    val categoryId: Int?,
    val amount: Double,
    val date: Long,
    val accountId: Int,
    val notes: String?,
    val transactionType: String = "expense",
    val sourceSmsId: Long? = null,
    val sourceSmsHash: String? = null,
    val source: String = "Manual Entry",
    val originalDescription: String? = null,
    val isExcluded: Boolean = false,
    // --- NEW: Field to store the SMS signature for pattern detection ---
    val smsSignature: String? = null
)
