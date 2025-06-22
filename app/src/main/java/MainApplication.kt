package com.example.personalfinanceapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MainApplication : Application() {

    companion object {
        const val TRANSACTION_CHANNEL_ID = "transaction_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Transactions"
            val descriptionText = "Notifications for newly detected transactions"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(TRANSACTION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
