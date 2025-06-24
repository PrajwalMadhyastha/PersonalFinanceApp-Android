package com.example.personalfinanceapp

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object ReminderManager {

    private const val REMINDER_WORK_TAG = "daily_review_reminder_work"

    fun scheduleDailyReminder(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresDeviceIdle(true) // More battery friendly
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val reminderRequest = PeriodicWorkRequestBuilder<ReviewReminderWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            REMINDER_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP, // Keep the existing work if it's already scheduled
            reminderRequest
        )
    }

    fun cancelDailyReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(REMINDER_WORK_TAG)
    }
}
