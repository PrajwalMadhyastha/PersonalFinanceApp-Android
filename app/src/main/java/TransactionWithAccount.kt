package com.example.personalfinanceapp

import androidx.room.Embedded

data class TransactionWithAccount(
    // This tells Room to treat all fields of the Transaction class
    // as if they were fields of this class.
    @Embedded
    val transaction: Transaction,

    // This will hold the result of our JOIN query.
    val accountName: String? // Nullable in case the account was deleted
)