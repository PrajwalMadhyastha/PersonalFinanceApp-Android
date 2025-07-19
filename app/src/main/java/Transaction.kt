// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/Transaction.kt
// REASON: FEATURE (Splitting) - Added a new `isSplit` boolean column. This will
// act as a simple flag to quickly identify "parent" transactions that have been
// split into multiple items, which is crucial for UI logic and preventing direct
// edits to a split transaction's amount.
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
        Index(value = ["smsSignature"])
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
    val amount: Double, // ALWAYS in home currency
    val date: Long,
    val accountId: Int,
    val notes: String?,
    val transactionType: String = "expense",
    val sourceSmsId: Long? = null,
    val sourceSmsHash: String? = null,
    val source: String = "Manual Entry",
    val originalDescription: String? = null,
    val isExcluded: Boolean = false,
    val smsSignature: String? = null,
    val originalAmount: Double? = null,
    val currencyCode: String? = null,
    val conversionRate: Double? = null,
    // --- NEW: Flag to indicate this is a parent transaction ---
    val isSplit: Boolean = false
)
