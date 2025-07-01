// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/MonthlySummaryWorker.kt
// REASON: Added a call to ReminderManager.scheduleMonthlySummary at the end of the
// worker's execution to create a continuous chain of precisely scheduled tasks.
// =================================================================================
package io.pm.finlight

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

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

                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                calendar.add(Calendar.MILLISECOND, -1)
                val endDate = calendar.timeInMillis

                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val startDate = calendar.timeInMillis

                val summary = transactionDao.getFinancialSummaryForRange(startDate, endDate)

                if (summary != null) {
                    NotificationHelper.showMonthlySummaryNotification(
                        context,
                        summary.totalIncome,
                        summary.totalExpenses,
                        calendar
                    )
                    Log.d("MonthlySummaryWorker", "Summary found for ${calendar.time}: Income=${summary.totalIncome}, Expenses=${summary.totalExpenses}")
                } else {
                    Log.d("MonthlySummaryWorker", "No summary data found for the previous month.")
                }

                // --- NEW: Re-schedule the next month's report ---
                ReminderManager.scheduleMonthlySummary(context)

                Log.d("MonthlySummaryWorker", "Worker finished successfully and rescheduled.")
                Result.success()
            } catch (e: Exception) {
                Log.e("MonthlySummaryWorker", "Worker failed", e)
                Result.retry()
            }
        }
    }
}
