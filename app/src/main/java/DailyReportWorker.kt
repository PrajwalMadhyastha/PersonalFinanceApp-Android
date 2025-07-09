// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/DailyReportWorker.kt
// REASON: REFACTOR - The date calculation logic has been updated to use a
// rolling 24-hour window instead of a fixed "yesterday". The worker now fetches
// data from the last 24 hours and compares it against the 24 hours prior to that.
// This makes the daily report more timely and relevant to the user.
// BUG FIX: The time period logic has been changed from a rolling 24-hour window
// to the previous full calendar day ("yesterday"). This aligns the worker's
// calculation with the report screen the user sees, fixing the bug where the
// notification data did not match the on-screen data.
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
                Log.d("DailyReportWorker", "Worker starting for yesterday's report...")
                val transactionDao = AppDatabase.getInstance(context).transactionDao()

                // --- FIX: Calculate for "yesterday" (the previous full calendar day) ---
                val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                val endDate = (yesterday.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }.timeInMillis
                val startDate = (yesterday.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis

                // --- FIX: Calculate for the day before yesterday for comparison ---
                val dayBeforeYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -2) }
                val previousPeriodEndDate = (dayBeforeYesterday.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }.timeInMillis
                val previousPeriodStartDate = (dayBeforeYesterday.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis


                val currentPeriodSummary = transactionDao.getFinancialSummaryForRange(startDate, endDate)
                val currentPeriodExpenses = currentPeriodSummary?.totalExpenses ?: 0.0

                val previousPeriodSummary = transactionDao.getFinancialSummaryForRange(previousPeriodStartDate, previousPeriodEndDate)
                val previousPeriodExpenses = previousPeriodSummary?.totalExpenses ?: 0.0

                val topCategories = transactionDao.getTopSpendingCategoriesForRange(startDate, endDate)

                val percentageChange = if (previousPeriodExpenses > 0) {
                    ((currentPeriodExpenses - previousPeriodExpenses) / previousPeriodExpenses * 100).roundToInt()
                } else null

                // --- FIX: Pass yesterday's timestamp to the notification helper ---
                NotificationHelper.showDailyReportNotification(context, currentPeriodExpenses, percentageChange, topCategories, yesterday.timeInMillis)

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
