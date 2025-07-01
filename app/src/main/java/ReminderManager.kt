// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ReminderManager.kt
// REASON: Refactored all scheduling functions to use OneTimeWorkRequest for
// precise timing based on user preferences, ensuring accurate report delivery.
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

    fun scheduleDailyReport(context: Context) {
        val prefs = context.getSharedPreferences("finance_app_settings", Context.MODE_PRIVATE)
        val hour = prefs.getInt("daily_report_hour", 9)
        val minute = prefs.getInt("daily_report_minute", 0)

        val now = Calendar.getInstance()
        val nextRun = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (nextRun.before(now)) {
            nextRun.add(Calendar.DAY_OF_YEAR, 1)
        }

        val initialDelay = nextRun.timeInMillis - now.timeInMillis
        val dailyReportRequest = OneTimeWorkRequestBuilder<DailyReportWorker>()
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            DAILY_EXPENSE_REPORT_WORK_TAG,
            ExistingWorkPolicy.REPLACE,
            dailyReportRequest,
        )
        Log.d("ReminderManager", "Daily report scheduled for ${nextRun.time}")
    }

    fun cancelDailyReport(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(DAILY_EXPENSE_REPORT_WORK_TAG)
    }

    fun scheduleWeeklySummary(context: Context) {
        val prefs = context.getSharedPreferences("finance_app_settings", Context.MODE_PRIVATE)
        val dayOfWeek = prefs.getInt("weekly_report_day", Calendar.MONDAY)
        val hour = prefs.getInt("weekly_report_hour", 9)
        val minute = prefs.getInt("weekly_report_minute", 0)

        val now = Calendar.getInstance()
        val nextRun = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (nextRun.before(now)) {
            nextRun.add(Calendar.WEEK_OF_YEAR, 1)
        }

        val initialDelay = nextRun.timeInMillis - now.timeInMillis
        val weeklyReportRequest = OneTimeWorkRequestBuilder<WeeklySummaryWorker>()
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WEEKLY_SUMMARY_WORK_TAG,
            ExistingWorkPolicy.REPLACE,
            weeklyReportRequest,
        )
        Log.d("ReminderManager", "Weekly summary scheduled for ${nextRun.time}")
    }


    fun cancelWeeklySummary(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WEEKLY_SUMMARY_WORK_TAG)
    }

    fun scheduleMonthlySummary(context: Context) {
        val prefs = context.getSharedPreferences("finance_app_settings", Context.MODE_PRIVATE)
        val dayOfMonth = prefs.getInt("monthly_report_day", 1)
        val hour = prefs.getInt("monthly_report_hour", 9)
        val minute = prefs.getInt("monthly_report_minute", 0)

        val now = Calendar.getInstance()
        val nextRun = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (nextRun.before(now)) {
            nextRun.add(Calendar.MONTH, 1)
        }

        val initialDelay = nextRun.timeInMillis - now.timeInMillis
        val monthlyReportRequest = OneTimeWorkRequestBuilder<MonthlySummaryWorker>()
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            MONTHLY_SUMMARY_WORK_TAG,
            ExistingWorkPolicy.REPLACE,
            monthlyReportRequest,
        )
        Log.d("ReminderManager", "Monthly summary scheduled for ${nextRun.time}")
    }
}
