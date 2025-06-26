package com.example.personalfinanceapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MainApplication : Application() {
    companion object {
        const val TRANSACTION_CHANNEL_ID = "transaction_channel"
        const val DAILY_REPORT_CHANNEL_ID = "daily_report_channel"
        const val SUMMARY_CHANNEL_ID = "summary_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createTransactionNotificationChannel()
        createDailyReportNotificationChannel()
        createSummaryNotificationChannel()
    }

    private fun createTransactionNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Transactions"
            val descriptionText = "Notifications for newly detected transactions"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                NotificationChannel(TRANSACTION_CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createDailyReportNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Reports"
            val descriptionText = "Daily summary of your spending."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                NotificationChannel(DAILY_REPORT_CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // --- ADDED: Function to create the new weekly summary channel ---
    private fun createSummaryNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Weekly Summaries"
            val descriptionText = "A weekly summary of your financial activity."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                NotificationChannel(SUMMARY_CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
