package io.pm.finlight

import androidx.room.Embedded

data class BudgetWithSpending(
    @Embedded
    val budget: Budget,
    val spent: Double,
)
