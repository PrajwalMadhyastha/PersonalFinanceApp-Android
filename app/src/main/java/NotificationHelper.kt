// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/NotificationHelper.kt
// REASON: FEATURE - Added `showRecurringPatternDetectedNotification`. This
// function creates a specific notification to inform the user that the app has
// automatically identified a new recurring transaction. It includes a deep link
// to the "Add/Edit Recurring Rule" screen, pre-filled with the detected data,
// allowing the user to review, modify, or confirm the new rule.
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
    private const val DEEP_LINK_URI_EDIT = "app://finlight.pm.io/transaction_detail"
    private const val DEEP_LINK_URI_REPORT_BASE = "app://finlight.pm.io/report"
    private const val DEEP_LINK_URI_LINK_RECURRING = "app://finlight.pm.io/link_recurring"
    // --- NEW: Deep link for the new pattern detection flow ---
    private const val DEEP_LINK_URI_ADD_RECURRING = "app://finlight.pm.io/add_recurring_transaction"


    // --- NEW: Notification for when a recurring pattern is detected ---
    fun showRecurringPatternDetectedNotification(
        context: Context,
        rule: RecurringTransaction
    ) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        // The deep link will navigate to the add/edit screen with the new rule's ID
        val deepLinkUri = "$DEEP_LINK_URI_ADD_RECURRING?ruleId=${rule.id}".toUri()

        val intent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
            `package` = context.packageName
        }

        // Use the rule's ID for a unique notification ID
        val notificationId = "pattern_${rule.id}".hashCode()
        val pendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(notificationId, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val contentText = "We noticed a recurring ${rule.transactionType} for '${rule.description}'. We've created a rule for you. Tap to review."

        val builder = NotificationCompat.Builder(context, MainApplication.TRANSACTION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("New Recurring Transaction Found")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Review Rule", pendingIntent)

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }


    fun showRecurringTransactionDueNotification(
        context: Context,
        potentialTxn: PotentialTransaction
    ) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val json = Gson().toJson(potentialTxn)
        val encodedJson = URLEncoder.encode(json, "UTF-8")
        val deepLinkUri = "$DEEP_LINK_URI_LINK_RECURRING/$encodedJson".toUri()

        val intent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
            `package` = context.packageName
        }

        val pendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(potentialTxn.sourceSmsId.toInt(), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val contentText = "Your payment of ${currencyFormat.format(potentialTxn.amount)} for ${potentialTxn.merchantName} is due. Tap to confirm."

        val builder = NotificationCompat.Builder(context, MainApplication.TRANSACTION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Recurring Payment Due")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Confirm Payment", pendingIntent)

        with(NotificationManagerCompat.from(context)) {
            notify(potentialTxn.sourceSmsId.toInt(), builder.build())
        }
    }


    private fun createEnhancedSummaryNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        totalExpenses: Double,
        topCategories: List<CategorySpending>,
        deepLinkUri: String
    ) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val intent = Intent(Intent.ACTION_VIEW, deepLinkUri.toUri())
        intent.`package` = context.packageName

        val pendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(notificationId, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(bigContentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setStyle(inboxStyle)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_view, "Review", pendingIntent)

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }


    fun showDailyReportNotification(
        context: Context,
        totalExpenses: Double,
        percentageChange: Int?,
        topCategories: List<CategorySpending>,
        dateMillis: Long
    ) {
        val title = when {
            percentageChange == null -> "Yesterday's Summary"
            percentageChange == 0 -> "Spending same as day before"
            percentageChange > 0 -> "Spending up by $percentageChange% yesterday"
            else -> "Spending down by ${abs(percentageChange)}% yesterday"
        }
        val deepLinkUri = "$DEEP_LINK_URI_REPORT_BASE/${TimePeriod.DAILY}?date=$dateMillis"

        createEnhancedSummaryNotification(
            context,
            MainApplication.DAILY_REPORT_CHANNEL_ID,
            2,
            title,
            totalExpenses,
            topCategories,
            deepLinkUri
        )
    }

    fun showWeeklySummaryNotification(
        context: Context,
        totalExpenses: Double,
        percentageChange: Int?,
        topCategories: List<CategorySpending>
    ) {
        val title = when {
            percentageChange == null -> "Your Weekly Summary"
            percentageChange == 0 -> "Spends same as last week"
            percentageChange > 0 -> "Spends up by $percentageChange% this week"
            else -> "Spends down by ${abs(percentageChange)}% this week"
        }
        createEnhancedSummaryNotification(
            context,
            MainApplication.SUMMARY_CHANNEL_ID,
            3,
            title,
            totalExpenses,
            topCategories,
            "$DEEP_LINK_URI_REPORT_BASE/${TimePeriod.WEEKLY}"
        )
    }

    fun showMonthlySummaryNotification(
        context: Context,
        calendar: Calendar,
        totalExpenses: Double,
        percentageChange: Int?,
        topCategories: List<CategorySpending>
    ) {
        val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: "Month"
        val title = when {
            percentageChange == null -> "Your $monthName Summary"
            percentageChange == 0 -> "Spends same as last month"
            percentageChange > 0 -> "Spends up by $percentageChange% in $monthName"
            else -> "Spends down by ${abs(percentageChange)}% in $monthName"
        }
        createEnhancedSummaryNotification(
            context,
            MainApplication.MONTHLY_SUMMARY_CHANNEL_ID,
            4,
            title,
            totalExpenses,
            topCategories,
            "$DEEP_LINK_URI_REPORT_BASE/${TimePeriod.MONTHLY}"
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
        transaction: Transaction,
    ) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val detailUri = "$DEEP_LINK_URI_EDIT/${transaction.id}".toUri()

        val intent = Intent(Intent.ACTION_VIEW, detailUri).apply {
            `package` = context.packageName
        }

        val pendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(transaction.id, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val notificationIcon = android.R.drawable.ic_dialog_info
        val typeText = transaction.transactionType.replaceFirstChar { it.uppercase() }
        val bigText = "$typeText of ₹${"%.2f".format(transaction.amount)} from ${transaction.description} detected. Tap to review and categorize."

        val builder = NotificationCompat.Builder(context, MainApplication.TRANSACTION_CHANNEL_ID)
            .setSmallIcon(notificationIcon)
            .setContentTitle("New Transaction Found")
            .setContentText("Tap to review a transaction from ${transaction.description}.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(notificationIcon, "Review & Categorize", pendingIntent)

        with(NotificationManagerCompat.from(context)) {
            notify(transaction.id, builder.build())
        }
    }
}
