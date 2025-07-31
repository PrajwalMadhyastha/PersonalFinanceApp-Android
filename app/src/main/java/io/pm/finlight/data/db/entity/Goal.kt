// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/Goal.kt
// REASON: NEW FILE - Defines the Room entity for a savings goal, including its
// name, target and saved amounts, target date, and the associated account.
// =================================================================================
package io.pm.finlight

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goals",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["accountId"])]
)
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val targetAmount: Double,
    var savedAmount: Double,
    val targetDate: Long?,
    val accountId: Int
)
