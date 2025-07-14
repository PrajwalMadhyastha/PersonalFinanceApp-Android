// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/DataExportService.kt
// REASON: BUG FIX - The `forEach` loop in `exportToCsvString` now explicitly
// specifies the type of the loop variable (`details: TransactionDetails`). This
// resolves the compiler's type inference ambiguity and fixes all related
// "Unresolved reference" errors.
// FEATURE - The CSV export has been enhanced to include the new `isExcluded`
// field, providing a more complete data export.
// FEATURE - Added a function to get the CSV template string for the new import dialog.
// =================================================================================
package io.pm.finlight

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

object DataExportService {
    private val json =
        Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        }

    // --- NEW: Function to provide the CSV template header ---
    fun getCsvTemplateString(): String {
        return "Date,Description,Amount,Type,Category,Account,Notes,IsExcluded\n"
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
                val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
                if (jsonString == null) return@withContext false

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
                Log.e("DataExportService", "Error importing from JSON", e)
                false
            }
        }
    }

    suspend fun exportToCsvString(context: Context): String? {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(context)
                val transactions = db.transactionDao().getAllTransactions().first()
                val csvBuilder = StringBuilder()

                // Use the template function for the header
                csvBuilder.append(getCsvTemplateString())

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                // Explicitly define the type of 'details' to resolve compiler ambiguity
                transactions.forEach { details: TransactionDetails ->
                    val date = dateFormat.format(Date(details.transaction.date))
                    val description = escapeCsvField(details.transaction.description)
                    val amount = details.transaction.amount.toString()
                    val type = details.transaction.transactionType
                    val category = escapeCsvField(details.categoryName ?: "N/A")
                    val account = escapeCsvField(details.accountName ?: "N/A")
                    val notes = escapeCsvField(details.transaction.notes ?: "")
                    val isExcluded = details.transaction.isExcluded.toString()

                    csvBuilder.append("$date,$description,$amount,$type,$category,$account,$notes,$isExcluded\n")
                }
                csvBuilder.toString()
            } catch (e: Exception) {
                Log.e("DataExportService", "Error exporting to CSV", e)
                null
            }
        }
    }

    suspend fun importFromCsv(
        context: Context,
        uri: Uri,
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(context)
                val accountDao = db.accountDao()
                val categoryDao = db.categoryDao()
                val transactionDao = db.transactionDao()

                val newTransactions = mutableListOf<Transaction>()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                context.contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
                    // Use .iterator() to avoid issues with concurrent modification
                    val lineIterator = lines.iterator()
                    // Skip header row
                    if (lineIterator.hasNext()) {
                        lineIterator.next()
                    }

                    while (lineIterator.hasNext()) {
                        val line = lineIterator.next()
                        val tokens =
                            line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                                .map { it.trim().removeSurrounding("\"") }

                        if (tokens.size >= 6) {
                            val date = dateFormat.parse(tokens[0])?.time ?: System.currentTimeMillis()
                            val description = tokens[1]
                            val amount = tokens[2].toDoubleOrNull() ?: 0.0
                            val type = tokens[3]
                            val categoryName = tokens[4]
                            val accountName = tokens[5]
                            val notes = if (tokens.size > 6) tokens[6] else null

                            var category: Category? = categoryDao.findByName(categoryName)
                            if (category == null && categoryName.isNotBlank()) {
                                categoryDao.insert(Category(name = categoryName))
                                category = categoryDao.findByName(categoryName) // Query again to get the object with the ID
                            }

                            var account: Account? = accountDao.findByName(accountName)
                            if (account == null && accountName.isNotBlank()) {
                                accountDao.insert(Account(name = accountName, type = "Imported"))
                                account = accountDao.findByName(accountName) // Query again to get the object with the ID
                            }

                            // Ensure account and category were successfully found or created before adding transaction
                            if (account != null && category != null) {
                                newTransactions.add(
                                    Transaction(
                                        description = description,
                                        amount = amount,
                                        date = date,
                                        transactionType = type,
                                        accountId = account.id,
                                        categoryId = category.id,
                                        notes = notes,
                                    ),
                                )
                            } else {
                                Log.w("DataExportService", "Skipping row due to missing account/category: $line")
                            }
                        }
                    }
                }

                if (newTransactions.isNotEmpty()) {
                    transactionDao.insertAll(newTransactions)
                }
                true
            } catch (e: Exception) {
                Log.e("DataExportService", "Error importing from CSV", e)
                false
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
