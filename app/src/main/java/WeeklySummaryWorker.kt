package com.example.personalfinanceapp

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * A background worker that calculates the user's financial summary for the past 7 days
 * and displays it as a system notification.
 */
class WeeklySummaryWorker(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("WeeklySummaryWorker", "Worker starting...")
                val db = AppDatabase.getInstance(context)
                val transactionDao = db.transactionDao()

                // 1. Calculate the start and end timestamps for the last 7 days.
                val calendar = Calendar.getInstance()
                val endDate = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val startDate = calendar.timeInMillis

                // 2. Fetch transactions for the last week using the existing DAO method.
                val transactions = transactionDao.getTransactionDetailsForRange(startDate, endDate).first()
                Log.d("WeeklySummaryWorker", "Found ${transactions.size} transactions in the last 7 days.")

                // 3. Calculate total income and expenses.
                var totalIncome = 0.0
                var totalExpenses = 0.0
                transactions.forEach { details ->
                    if (details.transaction.transactionType == "income") {
                        totalIncome += details.transaction.amount
                    } else {
                        totalExpenses += details.transaction.amount
                    }
                }

                // 4. Send the summary notification via the helper.
                NotificationHelper.showWeeklySummaryNotification(context, totalIncome, totalExpenses)

                Log.d("WeeklySummaryWorker", "Worker finished successfully.")
                Result.success()
            } catch (e: Exception) {
                Log.e("WeeklySummaryWorker", "Worker failed", e)
                Result.retry() // Retry the job if it fails
            }
        }
    }
}
