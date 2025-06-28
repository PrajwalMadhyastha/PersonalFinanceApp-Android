package io.pm.finlight

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.google.gson.Gson
import java.net.URLEncoder

object NotificationHelper {
    private const val DEEP_LINK_URI_APPROVE = "app://finlight.pm.io/approve_sms"
    // --- NEW: A deep link to directly edit a transaction ---
    private const val DEEP_LINK_URI_EDIT = "app://finlight.pm.io/edit_transaction"

    // --- NEW: Informational notification for auto-saved transactions ---
    fun showAutoSaveConfirmationNotification(
        context: Context,
        transaction: Transaction
    ) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val editUri = "$DEEP_LINK_URI_EDIT/${transaction.id}".toUri()
        val intent = Intent(Intent.ACTION_VIEW, editUri).apply {
            `package` = context.packageName
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            transaction.id, // Use transaction ID for unique pending intent
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, MainApplication.TRANSACTION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Transaction Auto-Saved")
            .setContentText("Saved ${transaction.description} (₹${"%.2f".format(transaction.amount)}). Tap to edit or categorize.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_edit, "Edit", pendingIntent)

        with(NotificationManagerCompat.from(context)) {
            notify(transaction.id, builder.build())
        }
    }

    // --- RETAINED: Notification for manual review flow ---
    fun showTransactionNotification(
        context: Context,
        potentialTransaction: PotentialTransaction,
    ) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val potentialTxnJson = Gson().toJson(potentialTransaction)
        val encodedJson = URLEncoder.encode(potentialTxnJson, "UTF-8")
        val approveUri = "$DEEP_LINK_URI_APPROVE?potentialTxnJson=$encodedJson".toUri()

        val intent = Intent(Intent.ACTION_VIEW, approveUri).apply {
            `package` = context.packageName
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            potentialTransaction.sourceSmsId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notificationIcon = android.R.drawable.ic_dialog_info
        val typeText = potentialTransaction.transactionType.replaceFirstChar { it.uppercase() }
        val bigText = "$typeText of ₹${"%.2f".format(potentialTransaction.amount)} from ${potentialTransaction.merchantName ?: "Unknown Sender"} detected. Tap to add."

        val builder = NotificationCompat.Builder(context, MainApplication.TRANSACTION_CHANNEL_ID)
            .setSmallIcon(notificationIcon)
            .setContentTitle("New Transaction Found")
            .setContentText("Tap to review a transaction from ${potentialTransaction.merchantName ?: "Unknown Sender"}.")
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
        totalExpenses: Double,
    ) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

        val pendingIntent = PendingIntent.getActivity(context, 100, intent, PendingIntent.FLAG_IMMUTABLE)

        val reportText =
            if (totalExpenses > 0) {
                "You spent a total of ₹${"%.2f".format(totalExpenses)} yesterday."
            } else {
                "Great job! You had no expenses yesterday."
            }

        val notification =
            NotificationCompat.Builder(context, MainApplication.DAILY_REPORT_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Your Daily Report")
                .setContentText(reportText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

        NotificationManagerCompat.from(context).notify(2, notification)
    }

    fun showWeeklySummaryNotification(
        context: Context,
        totalIncome: Double,
        totalExpenses: Double,
    ) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

        val pendingIntent = PendingIntent.getActivity(context, 101, intent, PendingIntent.FLAG_IMMUTABLE)

        val summaryText = "Income: ₹${"%.2f".format(totalIncome)} | Expenses: ₹${"%.2f".format(totalExpenses)}"

        val notification =
            NotificationCompat.Builder(context, MainApplication.SUMMARY_CHANNEL_ID)
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
