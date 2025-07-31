package io.pm.finlight

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val categoryName: String,
    val amount: Double,
    val month: Int, // e.g., 6 for June
    val year: Int, // e.g., 2024
)
