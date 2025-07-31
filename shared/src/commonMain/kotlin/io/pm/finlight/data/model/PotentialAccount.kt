package io.pm.finlight.data.model

import kotlinx.serialization.Serializable

/**
 * A DTO to hold information about a potential account parsed from an SMS.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 */
@Serializable
data class PotentialAccount(
    val formattedName: String,
    val accountType: String,
)
