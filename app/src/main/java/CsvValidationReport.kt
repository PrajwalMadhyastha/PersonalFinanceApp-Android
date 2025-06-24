package com.example.personalfinanceapp

/**
 * A data class to hold the full results of a CSV validation process.
 * @param validRows A list of rows that passed all validation checks.
 * @param invalidRows A list of rows that failed validation, including the reason.
 * @param totalRowCount The total number of data rows processed in the CSV file.
 */
data class CsvValidationReport(
    val validRows: List<ValidatedRow> = emptyList(),
    val invalidRows: List<InvalidRow> = emptyList(),
    val totalRowCount: Int = 0
)

/**
 * Represents a single row from the CSV that has been successfully parsed and validated.
 * @param lineNumber The original line number in the file.
 * @param transaction The parsed Transaction object ready for import.
 * @param categoryName The name of the category found.
 * @param accountName The name of the account found.
 */
data class ValidatedRow(
    val lineNumber: Int,
    val transaction: Transaction,
    val categoryName: String,
    val accountName: String
)

/**
 * Represents a single row from the CSV that failed validation.
 * @param lineNumber The original line number in the file.
 * @param rowData The original, raw data from the CSV row.
 * @param errorMessage A clear message explaining why the validation failed.
 */
data class InvalidRow(
    val lineNumber: Int,
    val rowData: String,
    val errorMessage: String
)
