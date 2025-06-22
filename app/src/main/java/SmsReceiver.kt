package com.example.personalfinanceapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive triggered for action: ${intent.action}")
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                    val db = AppDatabase.getInstance(context)
                    val transactionRepository = TransactionRepository(db.transactionDao())
                    val mappingRepository = MerchantMappingRepository(db.merchantMappingDao())

                    val existingMappings = mappingRepository.allMappings.first().associateBy({ it.smsSender }, { it.merchantName })
                    val existingSmsIds = transactionRepository.allTransactions.first()
                        .mapNotNull { it.transaction.notes?.let { notes -> "sms_id:(\\d+)".toRegex().find(notes)?.groups?.get(1)?.value?.toLongOrNull() } }
                        .toSet()

                    for (sms in messages) {
                        val smsId = sms.timestampMillis
                        if (!existingSmsIds.contains(smsId)) {
                            val smsMessage = SmsMessage(id = smsId, sender = sms.originatingAddress ?: "Unknown", body = sms.messageBody, date = smsId)
                            val potentialTransaction = SmsParser.parse(smsMessage, existingMappings)

                            if (potentialTransaction != null) {
                                // --- UPDATED: Show a notification instead of logging ---
                                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                    NotificationHelper.showTransactionNotification(context, potentialTransaction)
                                }
                            }
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
