package io.pm.finlight.data.model

import io.pm.finlight.data.db.entity.Transaction
import io.pm.finlight.data.db.entity.TransactionImage
import kotlinx.serialization.Serializable

/**
 * A Data Transfer Object (DTO) that combines a Transaction with its related
 * Account and Category details for display in the UI.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 */
@Serializable
data class TransactionDetails(
    val transaction: Transaction,
    val images: List<TransactionImage>,
    val accountName: String?,
    val categoryName: String?,
    val categoryIconKey: String?,
    val categoryColorKey: String?
)