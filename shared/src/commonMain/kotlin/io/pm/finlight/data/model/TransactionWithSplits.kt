package io.pm.finlight.data.model

import io.pm.finlight.data.db.entity.SplitTransaction
import io.pm.finlight.data.db.entity.Transaction
import kotlinx.serialization.Serializable

/**
 * A DTO used to return a parent transaction along with its list of child split items.
 *
 * This is a pure Kotlin data class for multiplatform compatibility.
 */
@Serializable
data class TransactionWithSplits(
    val transaction: Transaction,
    val splits: List<SplitTransaction>
)
