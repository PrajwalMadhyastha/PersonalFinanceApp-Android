package com.example.personalfinanceapp

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ReviewReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("ReviewReminderWorker", "Worker starting...")
        return withContext(Dispatchers.IO) {
            try {
                // 1. Get database instance and DAOs
                val db = AppDatabase.getInstance(context)
                val transactionDao = db.transactionDao()
                val merchantMappingDao = db.merchantMappingDao()

                // 2. Fetch all SMS messages from the device
                val smsRepository = SmsRepository(context)
                val allSms = smsRepository.fetchAllSms()
                Log.d("ReviewReminderWorker", "Fetched ${allSms.size} SMS messages.")

                // 3. Get existing merchant mappings and already-imported SMS IDs for de-duplication
                val existingMappings = merchantMappingDao.getAllMappings().first()
                    .associateBy({ it.smsSender }, { it.merchantName })

                // CORRECTED: Called the correct DAO method: getAllTransactions()
                val existingSmsIds = transactionDao.getAllTransactions().first()
                    .mapNotNull { it.transaction.sourceSmsId }
                    .toSet()
                Log.d("ReviewReminderWorker", "Found ${existingSmsIds.size} already imported SMS IDs.")

                // 4. Parse all SMS messages and filter out the ones already imported
                val newPotentialTransactions = allSms.mapNotNull { sms ->
                    SmsParser.parse(sms, existingMappings)
                }.filter { potentialTransaction ->
                    potentialTransaction.sourceSmsId?.let { !existingSmsIds.contains(it) } ?: true
                }

                // 5. If there are transactions to review, send a notification
                val reviewCount = newPotentialTransactions.size
                Log.d("ReviewReminderWorker", "Found $reviewCount new transactions to review.")
                if (reviewCount > 0) {
                    sendNotification(reviewCount)
                }

                Log.d("ReviewReminderWorker", "Worker finished successfully.")
                Result.success()
            } catch (e: Exception) {
                Log.e("ReviewReminderWorker", "Worker failed", e)
                Result.retry()
            }
        }
    }

    private fun sendNotification(reviewCount: Int) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.w("ReviewReminderWorker", "Notification permission not granted. Cannot send reminder.")
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = MainApplication.REMINDER_CHANNEL_ID

        val intent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            "app://personalfinanceapp.example.com/review_sms".toUri(),
            context,
            MainActivity::class.java
        )

        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Pending Transactions")
            .setContentText("You have $reviewCount new transactions to review.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
        Log.d("ReviewReminderWorker", "Reminder notification sent.")
    }
}
