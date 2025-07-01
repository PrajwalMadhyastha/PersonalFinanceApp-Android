// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/WeeklySummaryWorker.kt
// REASON: Added a call to ReminderManager.scheduleWeeklySummary at the end of the
// worker's execution to create a continuous chain of precisely scheduled tasks.
// =================================================================================
package io.pm.finlight

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar

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

                val calendar = Calendar.getInstance()
                val endDate = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val startDate = calendar.timeInMillis

                val transactions = transactionDao.getTransactionDetailsForRange(
                    startDate = startDate,
                    endDate = endDate,
                    keyword = null,
                    accountId = null,
                    categoryId = null
                ).first()
                Log.d("WeeklySummaryWorker", "Found ${transactions.size} transactions in the last 7 days.")

                var totalIncome = 0.0
                var totalExpenses = 0.0
                transactions.forEach { details ->
                    if (details.transaction.transactionType == "income") {
                        totalIncome += details.transaction.amount
                    } else {
                        totalExpenses += details.transaction.amount
                    }
                }

                NotificationHelper.showWeeklySummaryNotification(context, totalIncome, totalExpenses)

                // --- NEW: Re-schedule the next week's report ---
                ReminderManager.scheduleWeeklySummary(context)

                Log.d("WeeklySummaryWorker", "Worker finished successfully and rescheduled.")
                Result.success()
            } catch (e: Exception) {
                Log.e("WeeklySummaryWorker", "Worker failed", e)
                Result.retry()
            }
        }
    }
}
