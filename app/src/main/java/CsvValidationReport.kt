package io.pm.finlight

/**
 * An enum to represent the validation status of a single row from the CSV.
 */
enum class CsvRowStatus {
    VALID,
    INVALID_COLUMN_COUNT,
    INVALID_DATE,
    INVALID_AMOUNT,
    ACCOUNT_NOT_FOUND,
    CATEGORY_NOT_FOUND,
    NEEDS_ACCOUNT_CREATION,
    NEEDS_CATEGORY_CREATION,
    NEEDS_BOTH_CREATION,
}

/**
 * A data class representing a single, reviewable row from the imported CSV file.
 * It holds the original data and its current validation status.
 */
data class ReviewableRow(
    val lineNumber: Int,
    var rowData: List<String>,
    var status: CsvRowStatus,
    var statusMessage: String,
)

/**
 * A data class to hold the full results of a CSV validation process.
 */
data class CsvValidationReport(
    val reviewableRows: List<ReviewableRow> = emptyList(),
    val totalRowCount: Int = 0,
)
