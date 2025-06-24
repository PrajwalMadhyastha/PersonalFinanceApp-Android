package com.example.personalfinanceapp

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object ReminderManager {

    private const val REVIEW_REMINDER_WORK_TAG = "daily_review_reminder_work"
    // --- ADDED: A unique tag for our new weekly worker ---
    private const val WEEKLY_SUMMARY_WORK_TAG = "weekly_summary_work"

    fun scheduleDailyReminder(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresDeviceIdle(true)
            .build()

        val reminderRequest = PeriodicWorkRequestBuilder<ReviewReminderWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            REVIEW_REMINDER_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )
    }

    fun cancelDailyReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(REVIEW_REMINDER_WORK_TAG)
    }

    // --- ADDED: Functions to schedule and cancel the weekly summary job ---
    fun scheduleWeeklySummary(context: Context) {
        val reminderRequest = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(7, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WEEKLY_SUMMARY_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )
    }

    fun cancelWeeklySummary(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WEEKLY_SUMMARY_WORK_TAG)
    }
}
