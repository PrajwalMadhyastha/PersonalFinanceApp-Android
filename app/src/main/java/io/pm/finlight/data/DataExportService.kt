package io.pm.finlight.data

import android.content.Context
import android.net.Uri
import android.util.Log
import io.pm.finlight.AppDataBackup
import io.pm.finlight.TransactionDetails
import io.pm.finlight.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.forEach

object DataExportService {
    private val json =
        Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        }

    // --- UPDATED: Add "Tags" to the CSV template header ---
    fun getCsvTemplateString(): String {
        return "Date,Description,Amount,Type,Category,Account,Notes,IsExcluded,Tags\n"
    }

    suspend fun exportToJsonString(context: Context): String? {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.Companion.getInstance(context)

                val backupData =
                    AppDataBackup(
                        transactions = db.transactionDao().getAllTransactionsSimple().first(),
                        accounts = db.accountDao().getAllAccounts().first(),
                        categories = db.categoryDao().getAllCategories().first(),
                        budgets = db.budgetDao().getAllBudgets().first(),
                        merchantMappings = db.merchantMappingDao().getAllMappings().first(),
                    )

                json.encodeToString(backupData)
            } catch (e: Exception) {
                Log.e("DataExportService", "Error exporting to JSON", e)
                null
            }
        }
    }

    suspend fun importDataFromJson(
        context: Context,
        uri: Uri,
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()
                    .use { it?.readText() }
                if (jsonString == null) return@withContext false

                val backupData = json.decodeFromString<AppDataBackup>(jsonString)

                val db = AppDatabase.Companion.getInstance(context)
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
                Log.e("DataExportService", "Error importing from JSON", e)
                false
            }
        }
    }

    suspend fun exportToCsvString(context: Context): String? {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.Companion.getInstance(context)
                val transactionDao = db.transactionDao()
                val transactions = transactionDao.getAllTransactions().first()
                val csvBuilder = StringBuilder()

                csvBuilder.append(getCsvTemplateString())

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                transactions.forEach { details: TransactionDetails ->
                    val date = dateFormat.format(Date(details.transaction.date))
                    val description = escapeCsvField(details.transaction.description)
                    val amount = details.transaction.amount.toString()
                    val type = details.transaction.transactionType
                    val category = escapeCsvField(details.categoryName ?: "N/A")
                    val account = escapeCsvField(details.accountName ?: "N/A")
                    val notes = escapeCsvField(details.transaction.notes ?: "")
                    val isExcluded = details.transaction.isExcluded.toString()

                    // --- NEW: Fetch and append tags ---
                    val tags = transactionDao.getTagsForTransactionSimple(details.transaction.id)
                    val tagsString =
                        tags.joinToString("|") { it.name } // Use pipe as a safe delimiter
                    val escapedTags = escapeCsvField(tagsString)

                    csvBuilder.append("$date,$description,$amount,$type,$category,$account,$notes,$isExcluded,$escapedTags\n")
                }
                csvBuilder.toString()
            } catch (e: Exception) {
                Log.e("DataExportService", "Error exporting to CSV", e)
                null
            }
        }
    }

    // --- NOTE: The importFromCsv function has been moved to SettingsViewModel ---
    // This file now only handles data formatting (export).

    private fun escapeCsvField(field: String): String {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"${field.replace("\"", "\"\"")}\""
        }
        return field
    }
}