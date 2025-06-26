package com.example.personalfinanceapp

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object ReminderManager {
    private const val DAILY_EXPENSE_REPORT_WORK_TAG = "daily_expense_report_work"
    private const val WEEKLY_SUMMARY_WORK_TAG = "weekly_summary_work"

    fun scheduleDailyReport(context: Context) {
        val constraints =
            Constraints.Builder()
                .setRequiresDeviceIdle(true)
                .build()

        // Schedule the new DailyReportWorker to run periodically.
        val dailyReportRequest =
            PeriodicWorkRequestBuilder<DailyReportWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_EXPENSE_REPORT_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyReportRequest,
        )
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
}
