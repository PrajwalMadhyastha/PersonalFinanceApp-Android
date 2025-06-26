package com.example.personalfinanceapp

import androidx.room.Embedded

/**
 * A data class to hold an Account and its dynamically calculated balance.
 * This is used for display purposes in the UI and is not a database entity.
 */
data class AccountWithBalance(
    @Embedded
    val account: Account,
    val balance: Double,
)
