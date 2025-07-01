package io.pm.finlight

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * A background worker that calculates the user's financial summary for the previous month
 * and displays it as a system notification. This is scheduled to run periodically.
 */
class MonthlySummaryWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("MonthlySummaryWorker", "Worker starting...")
                val db = AppDatabase.getInstance(context)
                val transactionDao = db.transactionDao()

                // 1. Calculate the start and end timestamps for the entire previous month.
                val calendar = Calendar.getInstance()
                // Move calendar to the first day of the current month
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                // Move calendar to the last moment of the previous month
                calendar.add(Calendar.MILLISECOND, -1)
                val endDate = calendar.timeInMillis

                // Move calendar to the first day of the previous month
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val startDate = calendar.timeInMillis

                // 2. Fetch the summary for that period.
                val summary = transactionDao.getFinancialSummaryForRange(startDate, endDate)

                // 3. Send the notification if a summary was found.
                if (summary != null) {
                    NotificationHelper.showMonthlySummaryNotification(
                        context,
                        summary.totalIncome,
                        summary.totalExpenses,
                        calendar // Pass the calendar to get the correct month name
                    )
                    Log.d("MonthlySummaryWorker", "Summary found for ${calendar.time}: Income=${summary.totalIncome}, Expenses=${summary.totalExpenses}")
                } else {
                    Log.d("MonthlySummaryWorker", "No summary data found for the previous month.")
                }

                Log.d("MonthlySummaryWorker", "Worker finished successfully.")
                Result.success()
            } catch (e: Exception) {
                Log.e("MonthlySummaryWorker", "Worker failed", e)
                Result.retry()
            }
        }
    }
}
