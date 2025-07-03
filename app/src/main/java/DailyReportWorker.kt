// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/DailyReportWorker.kt
// REASON: FIX - Added a `!it.transaction.isExcluded` filter to the `totalExpenses`
// calculation. The underlying DAO query now returns all transactions for display
// flexibility, so this worker must now explicitly filter out excluded transactions
// to ensure the daily report is accurate.
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

/**
 * A background worker that calculates the user's total expenses from the previous day
 * and displays it as a system notification.
 */
class DailyReportWorker(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("DailyReportWorker", "Worker starting...")
                val db = AppDatabase.getInstance(context)
                val transactionDao = db.transactionDao()

                // 1. Calculate the start and end timestamps for "yesterday".
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -1) // Go back to yesterday

                // Set to the very end of yesterday for full inclusivity
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endDate = calendar.timeInMillis

                // Set to the very start of yesterday for full inclusivity
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.timeInMillis

                // 2. Fetch transactions for yesterday.
                val transactions = transactionDao.getTransactionDetailsForRange(
                    startDate = startDate,
                    endDate = endDate,
                    keyword = null,
                    accountId = null,
                    categoryId = null
                ).first()
                Log.d("DailyReportWorker", "Found ${transactions.size} transactions for yesterday.")

                // 3. Calculate total expenses, ignoring excluded transactions.
                val totalExpenses =
                    transactions
                        .filter { it.transaction.transactionType == "expense" && !it.transaction.isExcluded }
                        .sumOf { it.transaction.amount }

                // 4. Send the summary notification via the helper.
                NotificationHelper.showDailyReportNotification(context, totalExpenses)

                // 5. Re-schedule the next day's report.
                ReminderManager.scheduleDailyReport(context)

                Log.d("DailyReportWorker", "Worker finished successfully and rescheduled.")
                Result.success()
            } catch (e: Exception) {
                Log.e("DailyReportWorker", "Worker failed", e)
                Result.retry() // Retry the job if it fails
            }
        }
    }
}
