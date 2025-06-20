package com.example.personalfinanceapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val type: String
    // The balance field is intentionally removed from the database entity.
    // It will be calculated on-the-fly.
)
