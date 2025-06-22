package com.example.personalfinanceapp

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores a user-defined mapping between an SMS sender address
 * and a clean merchant name. This acts as the app's "memory".
 */
@Entity(tableName = "merchant_mappings")
data class MerchantMapping(
    @PrimaryKey
    val smsSender: String, // e.g., "AM-HDFCBK"
    val merchantName: String // e.g., "McDonald's"
)
