package io.pm.finlight

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telephony.SmsMessage as TelephonySmsMessage
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

                    val messagesBySender = messages.groupBy { it.originatingAddress }

                    for ((sender, parts) in messagesBySender) {
                        val fullBody = parts.joinToString("") { it.messageBody }
                        val firstMessage = parts.first()
                        val smsId = firstMessage.timestampMillis

                        // --- DIAGNOSTIC LOGGING ---
                        Log.d("DeDupeDebug", "--- RECEIVER ---")
                        Log.d("DeDupeDebug", "Receiver processing message from: $sender")
                        Log.d("DeDupeDebug", "Combined Body: '$fullBody'")
                        Log.d("DeDupeDebug", "----------------")


                        val db = AppDatabase.getInstance(context)
                        val transactionRepository = TransactionRepository(db.transactionDao())
                        val mappingRepository = MerchantMappingRepository(db.merchantMappingDao())

                        val existingMappings = mappingRepository.allMappings.first().associateBy({ it.smsSender }, { it.merchantName })
                        val existingSmsHashes = transactionRepository.getAllSmsHashes().first().toSet()

                        val smsMessage = SmsMessage(id = smsId, sender = sender ?: "Unknown", body = fullBody, date = smsId)
                        val potentialTransaction = SmsParser.parse(smsMessage, existingMappings)

                        if (potentialTransaction != null) {
                            if (!existingSmsHashes.contains(potentialTransaction.sourceSmsHash)) {
                                Log.d(TAG, "New potential transaction found: $potentialTransaction")
                                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                    NotificationHelper.showTransactionNotification(context, potentialTransaction)
                                    Log.d(TAG, "Notification sent for transaction.")
                                } else {
                                    Log.w(TAG, "Post Notifications permission not granted.")
                                }
                            } else {
                                Log.d(TAG, "SMS with hash: ${potentialTransaction.sourceSmsHash} is a duplicate. Skipping.")
                            }
                        } else {
                            Log.d(TAG, "SMS from $sender did not parse to a transaction.")
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
