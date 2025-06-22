package com.example.personalfinanceapp

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

    /**
     * NEW: Wipes the entire database and restores it from a JSON backup.
     * @param context The application context.
     * @param uri The URI of the JSON file selected by the user.
     * @return True if successful, false otherwise.
     */
    suspend fun importDataFromJson(context: Context, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Step 1: Read the JSON content from the file URI
                val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
                if (jsonString == null) {
                    return@withContext false
                }

                // Step 2: Parse the JSON into our backup data class
                val backupData = json.decodeFromString<AppDataBackup>(jsonString)

                // Step 3: Get database instance and DAOs
                val db = AppDatabase.getInstance(context)
                val transactionDao = db.transactionDao()
                val accountDao = db.accountDao()
                val categoryDao = db.categoryDao()
                val budgetDao = db.budgetDao()
                val merchantMappingDao = db.merchantMappingDao()

                // Step 4: WIPE ALL EXISTING DATA
                transactionDao.deleteAll()
                accountDao.deleteAll()
                categoryDao.deleteAll()
                budgetDao.deleteAll()
                merchantMappingDao.deleteAll()

                // Step 5: Insert all the data from the backup
                accountDao.insertAll(backupData.accounts)
                categoryDao.insertAll(backupData.categories)
                budgetDao.insertAll(backupData.budgets)
                merchantMappingDao.insertAll(backupData.merchantMappings)
                transactionDao.insertAll(backupData.transactions)

                true // Return success
            } catch (e: Exception) {
                e.printStackTrace()
                false // Return failure
            }
        }
    }
}
