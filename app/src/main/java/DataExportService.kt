package com.example.personalfinanceapp

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A service class responsible for exporting user data to a JSON format.
 */
object DataExportService {

    // Configure the JSON encoder for pretty printing
    private val json = Json { prettyPrint = true }

    /**
     * Gathers all data from the database and encodes it into a JSON string.
     * This operation is performed on a background thread.
     *
     * @param context The application context to access the database.
     * @return A String containing all app data in JSON format, or null on error.
     */
    suspend fun exportToJsonString(context: Context): String? {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(context)

                // Fetch all data from each table
                val transactions = db.transactionDao().getAllTransactionsSimple().first()
                val accounts = db.accountDao().getAllAccounts().first()
                val categories = db.categoryDao().getAllCategories().first()
                val budgets = db.budgetDao().getAllBudgets().first() // A new DAO method is needed here
                val mappings = db.merchantMappingDao().getAllMappings().first()

                // Assemble the backup data object
                val backupData = AppDataBackup(
                    transactions = transactions,
                    accounts = accounts,
                    categories = categories,
                    budgets = budgets,
                    merchantMappings = mappings
                )

                // Encode the data object into a JSON string
                json.encodeToString(backupData)

            } catch (e: Exception) {
                // Log the error in a real app
                e.printStackTrace()
                null
            }
        }
    }
}
