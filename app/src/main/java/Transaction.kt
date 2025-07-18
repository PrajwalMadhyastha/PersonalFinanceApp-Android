// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/Transaction.kt
// REASON: FEATURE - Added new nullable fields `originalAmount`, `currencyCode`,
// and `conversionRate` to support multi-currency transactions. The existing
// `amount` field will always store the value converted to the home currency.
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
    // --- NEW: Fields for multi-currency support ---
    val originalAmount: Double? = null, // Amount in foreign currency
    val currencyCode: String? = null, // e.g., "USD", "EUR"
    val conversionRate: Double? = null // Rate to convert originalAmount to home currency
)
