// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ReminderManager.kt
// REASON: Refactored scheduleDailyReport to use a OneTimeWorkRequest instead of
// a periodic one. It now calculates the precise delay until the next user-defined
// report time, creating a more accurate and configurable scheduling system.
// =================================================================================
package io.pm.finlight

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderManager {
    private const val DAILY_EXPENSE_REPORT_WORK_TAG = "daily_expense_report_work"
    private const val WEEKLY_SUMMARY_WORK_TAG = "weekly_summary_work"
    private const val MONTHLY_SUMMARY_WORK_TAG = "monthly_summary_work"

    // --- REFACTORED: To use OneTimeWorkRequest for precise timing ---
    fun scheduleDailyReport(context: Context) {
        // This manager needs to be self-sufficient because the worker will call it.
        // It reads the user's preferred time directly from SharedPreferences.
        val prefs = context.getSharedPreferences("finance_app_settings", Context.MODE_PRIVATE)
        val hour = prefs.getInt("daily_report_hour", 9) // Default to 9 AM
        val minute = prefs.getInt("daily_report_minute", 0)

        val now = Calendar.getInstance()
        val nextRun = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the calculated run time for today is in the past, schedule it for tomorrow.
        if (nextRun.before(now)) {
            nextRun.add(Calendar.DAY_OF_YEAR, 1)
        }

        val initialDelay = nextRun.timeInMillis - now.timeInMillis

        val dailyReportRequest = OneTimeWorkRequestBuilder<DailyReportWorker>()
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            DAILY_EXPENSE_REPORT_WORK_TAG,
            ExistingWorkPolicy.REPLACE, // Replace any existing scheduled work
            dailyReportRequest,
        )
        Log.d("ReminderManager", "Daily report scheduled for ${nextRun.time} (in $initialDelay ms)")
    }

    fun cancelDailyReport(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(DAILY_EXPENSE_REPORT_WORK_TAG)
    }

    fun scheduleWeeklySummary(context: Context) {
        val reminderRequest =
            PeriodicWorkRequestBuilder<WeeklySummaryWorker>(7, TimeUnit.DAYS)
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WEEKLY_SUMMARY_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest,
        )
    }

    fun cancelWeeklySummary(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WEEKLY_SUMMARY_WORK_TAG)
    }

    fun scheduleMonthlySummary(context: Context) {
        val reminderRequest =
            PeriodicWorkRequestBuilder<MonthlySummaryWorker>(30, TimeUnit.DAYS)
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MONTHLY_SUMMARY_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )
    }
}
