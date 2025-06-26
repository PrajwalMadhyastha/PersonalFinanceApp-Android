package com.example.personalfinanceapp

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "merchant_mappings")
data class MerchantMapping(
    @PrimaryKey
    val smsSender: String, // e.g., "AM-HDFCBK"
    val merchantName: String, // e.g., "McDonald's"
)
