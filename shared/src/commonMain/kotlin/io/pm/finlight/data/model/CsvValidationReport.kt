package io.pm.finlight.data.model

import kotlinx.serialization.Serializable

/**
 * An enum to represent the validation status of a single row from the CSV.
 */
@Serializable
enum class CsvRowStatus {
    VALID,
    INVALID_COLUMN_COUNT,
    INVALID_DATE,
    INVALID_AMOUNT,
    NEEDS_ACCOUNT_CREATION,
    NEEDS_CATEGORY_CREATION,
    NEEDS_BOTH_CREATION,
}

/**
 * A DTO representing a single, reviewable row from the imported CSV file.
 */
@Serializable
data class ReviewableRow(
    val lineNumber: Int,
    var rowData: List<String>,
    var status: CsvRowStatus,
    var statusMessage: String,
)

/**
 * A DTO to hold the full results of a CSV validation process.
 */
@Serializable
data class CsvValidationReport(
    val header: List<String> = emptyList(),
    val reviewableRows: List<ReviewableRow> = emptyList(),
    val totalRowCount: Int = 0,
)
