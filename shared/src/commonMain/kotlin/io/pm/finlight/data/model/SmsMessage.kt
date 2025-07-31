package io.pm.finlight.data.model

import kotlinx.serialization.Serializable

/**
 * A simple data class to hold the relevant information from an SMS message.
 */
@Serializable
data class SmsMessage(
    val id: Long,
    val sender: String,
    val body: String,
    val date: Long,
)
