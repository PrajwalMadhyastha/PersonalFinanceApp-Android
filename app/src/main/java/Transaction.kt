// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/Transaction.kt
// REASON: FEATURE - Added a new nullable field, `originalDescription`. This field
// will store the merchant name exactly as it was first parsed from an SMS,
// preserving it even if the user later edits the main `description`. This is
// crucial for the new "merchant rename" feature.
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
    val originalDescription: String? = null
)
