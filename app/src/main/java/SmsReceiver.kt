// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/SmsReceiver.kt
// REASON: REFACTOR - The call to `SmsParser.parse` has been updated to pass the
// full database instance instead of just a single DAO, enabling the parser to
// access all necessary rule tables.
// FEATURE: When creating a new `Transaction` from a parsed SMS, the new
// `originalDescription` field is now populated with the parsed merchant name.
// =================================================================================
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
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                    val messagesBySender = messages.groupBy { it.originatingAddress }

                    for ((sender, parts) in messagesBySender) {
                        if (sender == null) continue

                        val fullBody = parts.joinToString("") { it.messageBody }
                        val smsId = parts.first().timestampMillis

                        val db = AppDatabase.getInstance(context)
                        val transactionDao = db.transactionDao()
                        val accountDao = db.accountDao()
                        val mappingRepository = MerchantMappingRepository(db.merchantMappingDao())

                        val existingMappings = mappingRepository.allMappings.first().associateBy({ it.smsSender }, { it.merchantName })
                        val existingSmsHashes = transactionDao.getAllSmsHashes().first().toSet()

                        val smsMessage = SmsMessage(id = smsId, sender = sender, body = fullBody, date = smsId)
                        val potentialTxn = SmsParser.parse(smsMessage, existingMappings, db)

                        if (potentialTxn != null && !existingSmsHashes.contains(potentialTxn.sourceSmsHash)) {
                            Log.d(TAG, "New potential transaction found: $potentialTxn. Saving automatically.")

                            val accountName = potentialTxn.potentialAccount?.formattedName ?: "Unknown Account"
                            val accountType = potentialTxn.potentialAccount?.accountType ?: "General"

                            var account = accountDao.findByName(accountName)
                            if (account == null) {
                                val newAccount = Account(name = accountName, type = accountType)
                                accountDao.insert(newAccount)
                                account = accountDao.findByName(accountName)
                            }

                            if (account != null) {
                                val newTransaction = Transaction(
                                    description = potentialTxn.merchantName ?: "Unknown Merchant",
                                    originalDescription = potentialTxn.merchantName,
                                    amount = potentialTxn.amount,
                                    date = System.currentTimeMillis(),
                                    accountId = account.id,
                                    categoryId = null,
                                    notes = "", // Keep notes empty for user
                                    transactionType = potentialTxn.transactionType,
                                    sourceSmsId = potentialTxn.sourceSmsId,
                                    sourceSmsHash = potentialTxn.sourceSmsHash,
                                    source = "Auto-Imported"
                                )
                                val newTransactionId = transactionDao.insert(newTransaction)
                                Log.d(TAG, "Transaction saved successfully with ID: $newTransactionId")

                                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                    NotificationHelper.showAutoSaveConfirmationNotification(context, newTransaction.copy(id = newTransactionId.toInt()))
                                }

                            } else {
                                Log.e(TAG, "Failed to find or create an account for the transaction.")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing SMS", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
