package com.example.personalfinanceapp

import androidx.room.Embedded

data class AccountWithBalance(
    @Embedded
    val account: Account,
    val balance: Double
)