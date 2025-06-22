package com.example.personalfinanceapp

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri

object NotificationHelper {

    private const val DEEP_LINK_URI = "app://personalfinanceapp.example.com"

    fun showTransactionNotification(
        context: Context,
        potentialTransaction: PotentialTransaction
    ) {
        // Create the deep link URI with the transaction details as query parameters
        val approveUri = (
                "$DEEP_LINK_URI/approve?amount=${potentialTransaction.amount}" +
                        "&type=${potentialTransaction.transactionType}" +
                        "&merchant=${potentialTransaction.merchantName ?: "Unknown"}" +
                        "&smsId=${potentialTransaction.sourceSmsId}" +
                        "&smsSender=${potentialTransaction.smsSender}"
                ).toUri()

        // Create an explicit intent for the deep link
        val intent = Intent(Intent.ACTION_VIEW, approveUri).apply {
            `package` = context.packageName
        }

        // Create the PendingIntent
        val pendingIntent = PendingIntent.getActivity(
            context,
            potentialTransaction.sourceSmsId.toInt(), // Use SMS ID for a unique request code
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // --- CORRECTED: Replaced custom drawable with a built-in Android system icon ---
        // You can later create your own icon and place it in res/drawable,
        // then change this back to R.drawable.your_icon_name
        val notificationIcon = android.R.drawable.ic_dialog_info

        val builder = NotificationCompat.Builder(context, MainApplication.TRANSACTION_CHANNEL_ID)
            .setSmallIcon(notificationIcon)
            .setContentTitle("New Transaction Found")
            .setContentText("Tap to review and categorize a transaction from ${potentialTransaction.merchantName ?: "Unknown Sender"}.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Expense of ₹${"%.2f".format(potentialTransaction.amount)} from ${potentialTransaction.merchantName ?: "Unknown Sender"} detected. Tap to add this to your transaction history."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(notificationIcon, "Review & Categorize", pendingIntent)

        with(NotificationManagerCompat.from(context)) {
            notify(potentialTransaction.sourceSmsId.toInt(), builder.build())
        }
    }
}
