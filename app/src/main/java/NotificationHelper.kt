// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/NotificationHelper.kt
// REASON: BUG FIX - Replaced references to non-existent drawable resources
// (`ic_shopping_cart_24`, `ic_review_24`) with standard, available ones. The
// small icon is now the app's launcher icon for consistency, and the action
// icon is now a standard system icon (`ic_menu_view`). This resolves the
// "Unresolved reference" compiler errors.
// BUG FIX - The `createEnhancedSummaryNotification` function now accepts a `deepLinkUri`
// parameter, making it flexible. `showDailyReportNotification` now passes the correct
// URI for the new daily report screen, fixing the navigation bug.
// =================================================================================
package io.pm.finlight

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.net.toUri
import com.google.gson.Gson
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

object NotificationHelper {
    private const val DEEP_LINK_URI_APPROVE = "app://finlight.pm.io/approve_sms"
    private const val DEEP_LINK_URI_EDIT = "app://finlight.pm.io/transaction_detail"
    private const val DEEP_LINK_URI_REPORTS = "app://finlight.pm.io/reports"
    // --- NEW: Add a deep link for the daily report screen ---
    private const val DEEP_LINK_URI_DAILY_REPORT = "app://finlight.pm.io/daily_report"


    private fun createEnhancedSummaryNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        periodText: String,
        totalExpenses: Double,
        percentageChange: Int?,
        topCategories: List<CategorySpending>,
        // --- NEW: Accept the URI as a parameter ---
        deepLinkUri: String
    ) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val intent = Intent(
            Intent.ACTION_VIEW,
            // --- FIX: Use the passed-in URI ---
            deepLinkUri.toUri(),
            context,
            MainActivity::class.java
        )

        val pendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(notificationId, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val title = when {
            percentageChange == null -> "Your $periodText Summary"
            percentageChange == 0 -> "Spends same as last period"
            percentageChange > 0 -> "Spends up by $percentageChange% this $periodText"
            else -> "Spends down by ${abs(percentageChange)}% this $periodText"
        }

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val bigContentText = "You spent ${currencyFormat.format(totalExpenses)} in total."

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .setSummaryText("Got 2 mins to review?")

        if (topCategories.isNotEmpty()) {
            inboxStyle.addLine("Top spends:")
            for (category in topCategories) {
                inboxStyle.addLine("• ${category.categoryName}: ${currencyFormat.format(category.totalAmount)}")
            }
        } else {
            inboxStyle.addLine("No expenses recorded for this period.")
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use a standard, available icon
            .setContentTitle(title)
            .setContentText(bigContentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setStyle(inboxStyle)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_view, "Review", pendingIntent) // Use a standard, available icon

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }


    fun showDailyReportNotification(
        context: Context,
        totalExpenses: Double,
        percentageChange: Int?,
        topCategories: List<CategorySpending>
    ) {
        createEnhancedSummaryNotification(
            context,
            MainApplication.DAILY_REPORT_CHANNEL_ID,
            2,
            "Day",
            totalExpenses,
            percentageChange,
            topCategories,
            // --- FIX: Pass the correct URI for the daily report ---
            DEEP_LINK_URI_DAILY_REPORT
        )
    }

    fun showWeeklySummaryNotification(
        context: Context,
        totalExpenses: Double,
        percentageChange: Int?,
        topCategories: List<CategorySpending>
    ) {
        createEnhancedSummaryNotification(
            context,
            MainApplication.SUMMARY_CHANNEL_ID,
            3,
            "Week",
            totalExpenses,
            percentageChange,
            topCategories,
            // Pass the general reports URI for weekly/monthly
            DEEP_LINK_URI_REPORTS
        )
    }

    fun showMonthlySummaryNotification(
        context: Context,
        calendar: Calendar, // To get the month name
        totalExpenses: Double,
        percentageChange: Int?,
        topCategories: List<CategorySpending>
    ) {
        val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: "Month"
        createEnhancedSummaryNotification(
            context,
            MainApplication.MONTHLY_SUMMARY_CHANNEL_ID,
            4,
            monthName,
            totalExpenses,
            percentageChange,
            topCategories,
            // Pass the general reports URI for weekly/monthly
            DEEP_LINK_URI_REPORTS
        )
    }


    fun showAutoSaveConfirmationNotification(
        context: Context,
        transaction: Transaction
    ) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val detailIntent = Intent(
            Intent.ACTION_VIEW,
            "$DEEP_LINK_URI_EDIT/${transaction.id}".toUri(),
            context,
            MainActivity::class.java
        )

        val pendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(detailIntent)
            getPendingIntent(transaction.id, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val groupKey = "finlight_transaction_group_${transaction.id}"

        val builder = NotificationCompat.Builder(context, MainApplication.TRANSACTION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Transaction Auto-Saved")
            .setContentText("Saved ${transaction.description} (₹${"%.2f".format(transaction.amount)}). Tap to edit or categorize.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(groupKey)
            .addAction(android.R.drawable.ic_menu_edit, "Edit", pendingIntent)

        with(NotificationManagerCompat.from(context)) {
            notify(transaction.id, builder.build())
        }
    }

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
}
