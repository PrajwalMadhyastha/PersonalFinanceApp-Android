package com.example.personalfinanceapp

import androidx.room.Embedded

data class BudgetWithSpending(
    @Embedded
    val budget: Budget,
    val spent: Double
)