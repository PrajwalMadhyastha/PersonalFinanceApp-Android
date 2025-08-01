// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/data/DataExportService.kt
// REASON: FEATURE - The CSV export logic has been completely rewritten to
// support split transactions. It now includes "Id" and "ParentId" columns.
// Split items are exported as separate rows linked to their parent via the
// "ParentId", ensuring full data fidelity on re-import. The JSON backup logic
// is also updated to include the split_transactions table.
// =================================================================================
package io.pm.finlight.data

import android.content.Context
import android.net.Uri
import android.util.Log
import io.pm.finlight.AppDataBackup
import io.pm.finlight.TransactionDetails
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

    // --- UPDATED: Add "Id" and "ParentId" to the CSV template header ---
    fun getCsvTemplateString(): String {
        return "Id,ParentId,Date,Description,Amount,Type,Category,Account,Notes,IsExcluded,Tags\n"
    }

    suspend fun exportToJsonString(context: Context): String? {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(context)

                val backupData =
                    AppDataBackup(
                        transactions = db.transactionDao().getAllTransactionsSimple().first(),
                        accounts = db.accountDao().getAllAccounts().first(),
                        categories = db.categoryDao().getAllCategories().first(),
                        budgets = db.budgetDao().getAllBudgets().first(),
                        merchantMappings = db.merchantMappingDao().getAllMappings().first(),
                        // --- NEW: Include split transactions in the backup ---
                        splitTransactions = db.splitTransactionDao().getAllSplits().first()
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

                val db = AppDatabase.getInstance(context)
                // Clear all data in the correct order (respecting foreign keys)
                db.splitTransactionDao().deleteAll()
                db.transactionDao().deleteAll()
                db.accountDao().deleteAll()
                db.categoryDao().deleteAll()
                db.budgetDao().deleteAll()
                db.merchantMappingDao().deleteAll()


                // Insert new data
                db.accountDao().insertAll(backupData.accounts)
                db.categoryDao().insertAll(backupData.categories)
                db.budgetDao().insertAll(backupData.budgets)
                db.merchantMappingDao().insertAll(backupData.merchantMappings)
                db.transactionDao().insertAll(backupData.transactions)
                // --- NEW: Import split transactions ---
                db.splitTransactionDao().insertAll(backupData.splitTransactions)


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
                val db = AppDatabase.getInstance(context)
                val transactionDao = db.transactionDao()
                val splitTransactionDao = db.splitTransactionDao()
                val transactions = transactionDao.getAllTransactions().first()
                val csvBuilder = StringBuilder()

                csvBuilder.append(getCsvTemplateString())

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                transactions.forEach { details: TransactionDetails ->
                    val transaction = details.transaction
                    val date = dateFormat.format(Date(transaction.date))
                    val description = escapeCsvField(transaction.description)
                    val amount = transaction.amount.toString()
                    val type = transaction.transactionType
                    val account = escapeCsvField(details.accountName ?: "N/A")
                    val notes = escapeCsvField(transaction.notes ?: "")
                    val isExcluded = transaction.isExcluded.toString()
                    val tags = transactionDao.getTagsForTransactionSimple(transaction.id)
                    val tagsString = tags.joinToString("|") { it.name }
                    val escapedTags = escapeCsvField(tagsString)

                    if (transaction.isSplit) {
                        // This is a parent transaction
                        val category = "Split Transaction" // Parent has a special category
                        csvBuilder.append("${transaction.id},,$date,$description,$amount,$type,$category,$account,$notes,$isExcluded,$escapedTags\n")

                        // Now fetch and append its children
                        val splits = splitTransactionDao.getSplitsForParentSimple(transaction.id)
                        splits.forEach { splitDetails ->
                            val split = splitDetails.splitTransaction
                            // Use notes as description for splits, fallback to category name
                            val splitDescription = escapeCsvField(split.notes ?: splitDetails.categoryName ?: "")
                            val splitAmount = split.amount.toString()
                            val splitCategory = escapeCsvField(splitDetails.categoryName ?: "N/A")
                            // Child rows have no ID of their own in this context, but link to the parent
                            csvBuilder.append(",${transaction.id},${dateFormat.format(Date(transaction.date))},$splitDescription,$splitAmount,$type,$splitCategory,$account,${escapeCsvField(split.notes ?: "")},$isExcluded,\n")
                        }
                    } else {
                        // This is a standard, non-split transaction
                        val category = escapeCsvField(details.categoryName ?: "N/A")
                        csvBuilder.append("${transaction.id},,$date,$description,$amount,$type,$category,$account,$notes,$isExcluded,$escapedTags\n")
                    }
                }
                csvBuilder.toString()
            } catch (e: Exception) {
                Log.e("DataExportService", "Error exporting to CSV", e)
                null
            }
        }
    }

    private fun escapeCsvField(field: String): String {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"${field.replace("\"", "\"\"")}\""
        }
        return field
    }
}
