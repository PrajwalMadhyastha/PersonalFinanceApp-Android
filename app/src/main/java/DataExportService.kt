package com.example.personalfinanceapp

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

object DataExportService {

    private val json = Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true }

    suspend fun exportToJsonString(context: Context): String? {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(context)

                val backupData = AppDataBackup(
                    transactions = db.transactionDao().getAllTransactionsSimple().first(),
                    accounts = db.accountDao().getAllAccounts().first(),
                    categories = db.categoryDao().getAllCategories().first(),
                    budgets = db.budgetDao().getAllBudgets().first(),
                    merchantMappings = db.merchantMappingDao().getAllMappings().first()
                )

                json.encodeToString(backupData)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun importDataFromJson(context: Context, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
                if (jsonString == null) {
                    return@withContext false
                }

                val backupData = json.decodeFromString<AppDataBackup>(jsonString)

                val db = AppDatabase.getInstance(context)
                val transactionDao = db.transactionDao()
                val accountDao = db.accountDao()
                val categoryDao = db.categoryDao()
                val budgetDao = db.budgetDao()
                val merchantMappingDao = db.merchantMappingDao()

                transactionDao.deleteAll()
                accountDao.deleteAll()
                categoryDao.deleteAll()
                budgetDao.deleteAll()
                merchantMappingDao.deleteAll()

                accountDao.insertAll(backupData.accounts)
                categoryDao.insertAll(backupData.categories)
                budgetDao.insertAll(backupData.budgets)
                merchantMappingDao.insertAll(backupData.merchantMappings)
                transactionDao.insertAll(backupData.transactions)

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * NEW: Exports all transactions to a single CSV-formatted string.
     * Fetches all transaction details and builds a CSV string with a header row.
     * @return A string containing the data in CSV format, or null on error.
     */
    suspend fun exportToCsvString(context: Context): String? {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(context)
                val transactions = db.transactionDao().getAllTransactions().first()
                val csvBuilder = StringBuilder()

                // Append header row
                csvBuilder.append("Date,Description,Amount,Type,Category,Account,Notes\n")

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                // Append each transaction as a new row
                transactions.forEach { details ->
                    val date = dateFormat.format(Date(details.transaction.date))
                    val description = escapeCsvField(details.transaction.description)
                    val amount = details.transaction.amount.toString()
                    val type = details.transaction.transactionType
                    val category = escapeCsvField(details.categoryName ?: "N/A")
                    val account = escapeCsvField(details.accountName ?: "N/A")
                    val notes = escapeCsvField(details.transaction.notes ?: "")

                    csvBuilder.append("$date,$description,$amount,$type,$category,$account,$notes\n")
                }
                csvBuilder.toString()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Helper function to properly escape a field for CSV format.
     * If a field contains a comma, a quote, or a newline, it wraps the field in double quotes
     * and doubles up any existing double quotes within the field.
     */
    private fun escapeCsvField(field: String): String {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"${field.replace("\"", "\"\"")}\""
        }
        return field
    }
}
