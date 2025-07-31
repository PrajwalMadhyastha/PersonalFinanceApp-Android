package io.pm.finlight.data.model

import io.pm.finlight.data.db.entity.Account
import kotlinx.serialization.Serializable

/**
 * A DTO that holds an Account and its dynamically calculated balance.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 */
@Serializable
data class AccountWithBalance(
    val account: Account,
    val balance: Double,
)
