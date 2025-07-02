// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/DailyReportWorker.kt
// REASON: BUG FIX - The date calculation logic has been made more precise. The
// start of "yesterday" is now explicitly set to the first millisecond (00:00:00.000)
// and the end is set to the last millisecond (23:59:59.999). This ensures that
// the `BETWEEN` clause in the database query correctly includes all transactions
// from the entire day, fixing a bug where transactions from late in the day
// could be missed.
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

                // --- FIX: Set to the very end of yesterday for full inclusivity ---
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endDate = calendar.timeInMillis

                // --- FIX: Set to the very start of yesterday for full inclusivity ---
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

                // 3. Calculate total expenses.
                val totalExpenses =
                    transactions
                        .filter { it.transaction.transactionType == "expense" }
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
