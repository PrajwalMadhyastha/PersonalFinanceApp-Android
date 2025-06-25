package com.example.personalfinanceapp

/**
 * A data class to hold the full results of a CSV validation process.
 */
data class CsvValidationReport(
    val validRows: List<ValidatedRow> = emptyList(),
    val invalidRows: List<InvalidRow> = emptyList(),
    // --- ADDED: A new list for rows that require creating new entities ---
    val rowsWithNewEntities: List<RowForCreation> = emptyList(),
    val totalRowCount: Int = 0
)

/**
 * Represents a single row from the CSV that is perfect and ready for import.
 */
data class ValidatedRow(
    val lineNumber: Int,
    val transaction: Transaction,
    val categoryName: String,
    val accountName: String
)

/**
 * Represents a single row from the CSV that has malformed data.
 */
data class InvalidRow(
    val lineNumber: Int,
    val rowData: String,
    val errorMessage: String
)

/**
 * Represents a row that is valid but requires creating a new Account or Category.
 */
data class RowForCreation(
    val lineNumber: Int,
    val rawData: List<String>,
    val message: String
)
