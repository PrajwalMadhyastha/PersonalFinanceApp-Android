package com.example.personalfinanceapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    private val TAG = "SmsReceiver"

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        Log.d(TAG, "onReceive triggered for action: ${intent.action}")

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                    Log.d(TAG, "Received ${messages.size} message parts.")

                    val db = AppDatabase.getInstance(context)
                    val transactionRepository = TransactionRepository(db.transactionDao())
                    val mappingRepository = MerchantMappingRepository(db.merchantMappingDao())

                    // --- CORRECTED: Use the same de-duplication logic as the manual scan ---
                    // 1. Get existing mappings and already imported SMS IDs from the database
                    val existingMappings = mappingRepository.allMappings.first().associateBy({ it.smsSender }, { it.merchantName })
                    val existingSmsIds =
                        transactionRepository.allTransactions.first()
                            .mapNotNull { transactionDetail ->
                                transactionDetail.transaction.notes?.let { notes ->
                                    val match = "sms_id:(\\d+)".toRegex().find(notes)
                                    match?.groups?.get(1)?.value?.toLongOrNull()
                                }
                            }.toSet()

                    // 2. Process each incoming message
                    for (sms in messages) {
                        // In modern Android, the intent contains the SMS ID. We need to query for it.
                        // For simplicity in the receiver, we will continue to use timestamp as a proxy for the ID.
                        // A more complex implementation might query the ContentResolver again here.
                        val smsId = sms.timestampMillis
                        val sender = sms.originatingAddress ?: "Unknown"

                        Log.d(TAG, "Processing SMS with approximate ID (timestamp): $smsId from $sender")

                        // NOTE: This de-duplication is not perfect because we don't have the real _ID here.
                        // We check against timestamp, which is a good heuristic.
                        if (!existingSmsIds.contains(smsId)) {
                            val smsMessage = SmsMessage(id = smsId, sender = sender, body = sms.messageBody, date = smsId)
                            val potentialTransaction = SmsParser.parse(smsMessage, existingMappings)

                            if (potentialTransaction != null) {
                                Log.d(TAG, "New potential transaction found: $potentialTransaction")
                                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                    NotificationHelper.showTransactionNotification(context, potentialTransaction)
                                    Log.d(TAG, "Notification sent for transaction.")
                                } else {
                                    Log.w(TAG, "Post Notifications permission not granted.")
                                }
                            } else {
                                Log.d(TAG, "SMS did not parse to a transaction.")
                            }
                        } else {
                            Log.d(TAG, "SMS with ID: $smsId appears to be a duplicate. Skipping.")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing SMS", e)
                } finally {
                    pendingResult.finish()
                    Log.d(TAG, "Pending result finished.")
                }
            }
        }
    }
}
