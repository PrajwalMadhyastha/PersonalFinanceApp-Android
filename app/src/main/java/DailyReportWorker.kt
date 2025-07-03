// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/DailyReportWorker.kt
// REASON: REFACTOR - The date calculation logic has been updated to use a
// rolling 24-hour window instead of a fixed "yesterday". The worker now fetches
// data from the last 24 hours and compares it against the 24 hours prior to that.
// This makes the daily report more timely and relevant to the user.
// =================================================================================
package io.pm.finlight

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.roundToInt

class DailyReportWorker(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("DailyReportWorker", "Worker starting for last 24 hours...")
                val transactionDao = AppDatabase.getInstance(context).transactionDao()

                // --- REFACTORED: Calculate a rolling 24-hour window ---
                val endDate = Calendar.getInstance().timeInMillis
                val startDate = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, -24) }.timeInMillis

                // --- REFACTORED: Calculate the previous 24-hour window (24-48 hours ago) for comparison ---
                val previousPeriodEndDate = startDate - 1
                val previousPeriodStartDate = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, -48) }.timeInMillis

                val currentPeriodSummary = transactionDao.getFinancialSummaryForRange(startDate, endDate)
                val currentPeriodExpenses = currentPeriodSummary?.totalExpenses ?: 0.0

                val previousPeriodSummary = transactionDao.getFinancialSummaryForRange(previousPeriodStartDate, previousPeriodEndDate)
                val previousPeriodExpenses = previousPeriodSummary?.totalExpenses ?: 0.0

                val topCategories = transactionDao.getTopSpendingCategoriesForRange(startDate, endDate)

                val percentageChange = if (previousPeriodExpenses > 0) {
                    ((currentPeriodExpenses - previousPeriodExpenses) / previousPeriodExpenses * 100).roundToInt()
                } else null

                NotificationHelper.showDailyReportNotification(context, currentPeriodExpenses, percentageChange, topCategories)

                ReminderManager.scheduleDailyReport(context)
                Log.d("DailyReportWorker", "Worker finished and rescheduled.")
                Result.success()
            } catch (e: Exception) {
                Log.e("DailyReportWorker", "Worker failed", e)
                Result.retry()
            }
        }
    }
}
