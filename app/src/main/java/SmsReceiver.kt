// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/SmsReceiver.kt
// REASON: FEATURE (Travel Mode SMS) - The receiver is now aware of Travel Mode.
// It checks the SettingsRepository. If travel mode is active, instead of
// auto-saving, it calls a new notification helper to show a currency
// clarification prompt to the user. Otherwise, it proceeds as before.
// =================================================================================
package io.pm.finlight

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

class SmsReceiver : BroadcastReceiver() {
    private val tag = "SmsReceiver"

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
                        val settingsRepository = SettingsRepository(context)
                        val transactionDao = db.transactionDao()
                        val accountDao = db.accountDao()
                        val mappingRepository = MerchantMappingRepository(db.merchantMappingDao())
                        val merchantCategoryMappingDao = db.merchantCategoryMappingDao()
                        val ignoreRuleDao = db.ignoreRuleDao()

                        val existingMappings = mappingRepository.allMappings.first().associateBy({ it.smsSender }, { it.merchantName })
                        val existingSmsHashes = transactionDao.getAllSmsHashes().first().toSet()

                        val smsMessage = SmsMessage(id = smsId, sender = sender, body = fullBody, date = smsId)
                        val potentialTxn = SmsParser.parse(
                            sms = smsMessage,
                            mappings = existingMappings,
                            customSmsRuleDao = db.customSmsRuleDao(),
                            merchantRenameRuleDao = db.merchantRenameRuleDao(),
                            ignoreRuleDao = ignoreRuleDao,
                            merchantCategoryMappingDao = merchantCategoryMappingDao
                        )

                        if (potentialTxn != null && !existingSmsHashes.contains(potentialTxn.sourceSmsHash)) {
                            Log.d(tag, "New potential transaction found: $potentialTxn.")

                            // --- UPDATED: Check for active Travel Mode ---
                            val travelSettings = settingsRepository.getTravelModeSettings().first()
                            val isTravelModeActive = travelSettings?.isEnabled == true &&
                                    Date().time in travelSettings.startDate..travelSettings.endDate

                            if (isTravelModeActive) {
                                // Travel mode is on, so ask the user for clarification.
                                Log.d(tag, "Travel mode is active. Showing currency clarification notification.")
                                NotificationHelper.showTravelModeSmsNotification(context, potentialTxn, travelSettings!!)
                            } else {
                                // Travel mode is off, proceed with auto-save.
                                Log.d(tag, "Travel mode is inactive. Saving automatically.")
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
                                        categoryId = potentialTxn.categoryId,
                                        notes = "",
                                        transactionType = potentialTxn.transactionType,
                                        sourceSmsId = potentialTxn.sourceSmsId,
                                        sourceSmsHash = potentialTxn.sourceSmsHash,
                                        source = "Auto-Captured",
                                        smsSignature = potentialTxn.smsSignature
                                    )
                                    val newTransactionId = transactionDao.insert(newTransaction)
                                    Log.d(tag, "Transaction saved successfully with ID: $newTransactionId")

                                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                        val workRequest = OneTimeWorkRequestBuilder<TransactionNotificationWorker>()
                                            .setInputData(workDataOf(TransactionNotificationWorker.KEY_TRANSACTION_ID to newTransactionId.toInt()))
                                            .build()
                                        WorkManager.getInstance(context).enqueue(workRequest)
                                    }
                                } else {
                                    Log.e(tag, "Failed to find or create an account for the transaction.")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error processing SMS", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
