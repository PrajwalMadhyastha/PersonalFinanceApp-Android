package com.example.personalfinanceapp

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "recurring_transactions",
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["categoryId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val description: String,
    val amount: Double,
    val transactionType: String, // "income" or "expense"
    val recurrenceInterval: String, // e.g., "Monthly", "Weekly", "Yearly"
    val startDate: Long, // Timestamp for the first occurrence
    val accountId: Int,
    val categoryId: Int?
)
