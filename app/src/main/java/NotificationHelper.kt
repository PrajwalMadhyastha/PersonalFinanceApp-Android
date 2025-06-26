package com.example.personalfinanceapp

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import java.net.URLEncoder

object NotificationHelper {

    private const val DEEP_LINK_URI = "app://personalfinanceapp.example.com"

    fun showTransactionNotification(
        context: Context,
        potentialTransaction: PotentialTransaction
    ) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val encodedMerchant = URLEncoder.encode(potentialTransaction.merchantName ?: "Unknown", "UTF-8")
        val approveUri = (
                "$DEEP_LINK_URI/approve?amount=${potentialTransaction.amount}" +
                        "&type=${potentialTransaction.transactionType}" +
                        "&merchant=$encodedMerchant" +
                        "&smsId=${potentialTransaction.sourceSmsId}" +
                        "&smsSender=${potentialTransaction.smsSender}"
                ).toUri()

        val intent = Intent(Intent.ACTION_VIEW, approveUri).apply {
            `package` = context.packageName
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            potentialTransaction.sourceSmsId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationIcon = android.R.drawable.ic_dialog_info

        // --- BUG FIX: Dynamically set the notification text based on transaction type ---
        val typeText = potentialTransaction.transactionType.replaceFirstChar { it.uppercase() }
        val bigText = "$typeText of ₹${"%.2f".format(potentialTransaction.amount)} from ${potentialTransaction.merchantName ?: "Unknown Sender"} detected. Tap to add this to your transaction history."

        val builder = NotificationCompat.Builder(context, MainApplication.TRANSACTION_CHANNEL_ID)
            .setSmallIcon(notificationIcon)
            .setContentTitle("New Transaction Found")
            .setContentText("Tap to review and categorize a transaction from ${potentialTransaction.merchantName ?: "Unknown Sender"}.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(notificationIcon, "Review & Categorize", pendingIntent)

        with(NotificationManagerCompat.from(context)) {
            notify(potentialTransaction.sourceSmsId.toInt(), builder.build())
        }
    }

    fun showDailyReportNotification(
        context: Context,
        totalExpenses: Double
    ) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(context, 100, intent, PendingIntent.FLAG_IMMUTABLE)

        val reportText = if (totalExpenses > 0) {
            "You spent a total of ₹${"%.2f".format(totalExpenses)} yesterday."
        } else {
            "Great job! You had no expenses yesterday."
        }

        val notification = NotificationCompat.Builder(context, MainApplication.DAILY_REPORT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Your Daily Report")
            .setContentText(reportText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(2, notification)
    }

    /**
     * NEW: Creates and displays a notification for the weekly summary.
     */
    fun showWeeklySummaryNotification(
        context: Context,
        totalIncome: Double,
        totalExpenses: Double
    ) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(context, 101, intent, PendingIntent.FLAG_IMMUTABLE)

        val summaryText = "Income: ₹${"%.2f".format(totalIncome)} | Expenses: ₹${"%.2f".format(totalExpenses)}"

        val notification = NotificationCompat.Builder(context, MainApplication.SUMMARY_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("Your Weekly Financial Summary")
            .setContentText(summaryText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(3, notification)
    }
}
