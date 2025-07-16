// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/SmsReceiver.kt
// REASON: FIX - The private property 'TAG' has been renamed to 'tag' to comply
// with Kotlin's naming conventions for private constants. Its usage has been
// updated accordingly throughout the file.
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    // --- FIX: Renamed TAG to tag for Kotlin conventions ---
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
                        val transactionDao = db.transactionDao()
                        val accountDao = db.accountDao()
                        val mappingRepository = MerchantMappingRepository(db.merchantMappingDao())
                        val merchantCategoryMappingDao = db.merchantCategoryMappingDao()
                        val ignoreRuleDao = db.ignoreRuleDao()
                        val settingsRepository = SettingsRepository(context)

                        val existingMappings = mappingRepository.allMappings.first().associateBy({ it.smsSender }, { it.merchantName })
                        val existingSmsHashes = transactionDao.getAllSmsHashes().first().toSet()

                        val smsMessage = SmsMessage(id = smsId, sender = sender, body = fullBody, date = smsId)
                        // --- UPDATED: Pass the merchantCategoryMappingDao to the parser ---
                        val potentialTxn = SmsParser.parse(
                            sms = smsMessage,
                            mappings = existingMappings,
                            customSmsRuleDao = db.customSmsRuleDao(),
                            merchantRenameRuleDao = db.merchantRenameRuleDao(),
                            ignoreRuleDao = ignoreRuleDao,
                            merchantCategoryMappingDao = merchantCategoryMappingDao
                        )

                        if (potentialTxn != null && !existingSmsHashes.contains(potentialTxn.sourceSmsHash)) {
                            Log.d(tag, "New potential transaction found: $potentialTxn. Saving automatically.")

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
                                    // --- UPDATED: Use the categoryId from the parser result ---
                                    categoryId = potentialTxn.categoryId,
                                    notes = "",
                                    transactionType = potentialTxn.transactionType,
                                    sourceSmsId = potentialTxn.sourceSmsId,
                                    sourceSmsHash = potentialTxn.sourceSmsHash,
                                    source = "Auto-Captured"
                                )
                                val newTransactionId = transactionDao.insert(newTransaction)
                                Log.d(tag, "Transaction saved successfully with ID: $newTransactionId")

                                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                    val savedTransaction = newTransaction.copy(id = newTransactionId.toInt())
                                    // --- UPDATED: Check if the parser found a category ---
                                    if (potentialTxn.categoryId == null && settingsRepository.isUnknownTransactionPopupEnabledBlocking()) {
                                        NotificationHelper.showTransactionNotification(context, savedTransaction)
                                    } else {
                                        NotificationHelper.showAutoSaveConfirmationNotification(context, savedTransaction)
                                    }
                                }

                            } else {
                                Log.e(tag, "Failed to find or create an account for the transaction.")
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
