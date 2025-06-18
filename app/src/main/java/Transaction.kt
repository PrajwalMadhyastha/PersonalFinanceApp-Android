package com.example.personalfinanceapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val accountId: Int,

    val description: String,

    val amount: Double,

    // We store the date as a Long (Unix timestamp in milliseconds)
    // This is the most efficient and reliable way to store dates in SQLite.
    val date: Long,

    // We can add more fields later, like a 'type' (income/expense)
    // val type: String
)