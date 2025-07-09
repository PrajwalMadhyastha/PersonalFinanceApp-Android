// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/SmsReceiver.kt
// REASON: BUG FIX - The receiver now calls the updated
// `showTransactionNotification` function, passing the newly saved `Transaction`
// object (with its ID) instead of the old `PotentialTransaction`. This ensures
// the notification's deep link correctly points to the `TransactionDetailScreen`,
// resolving the navigation bug.
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
                        val merchantCategoryMappingDao = db.merchantCategoryMappingDao()
                        val ignoreRuleDao = db.ignoreRuleDao()
                        val settingsRepository = SettingsRepository(context)

                        val existingMappings = mappingRepository.allMappings.first().associateBy({ it.smsSender }, { it.merchantName })
                        val existingSmsHashes = transactionDao.getAllSmsHashes().first().toSet()

                        val smsMessage = SmsMessage(id = smsId, sender = sender, body = fullBody, date = smsId)
                        val potentialTxn = SmsParser.parse(smsMessage, existingMappings, db.customSmsRuleDao(), db.merchantRenameRuleDao(), ignoreRuleDao)

                        if (potentialTxn != null && !existingSmsHashes.contains(potentialTxn.sourceSmsHash)) {
                            Log.d(TAG, "New potential transaction found: $potentialTxn. Saving automatically.")

                            var mappedCategoryId: Int? = null
                            potentialTxn.merchantName?.let { merchantName ->
                                mappedCategoryId = merchantCategoryMappingDao.getCategoryIdForMerchant(merchantName)
                                if (mappedCategoryId != null) {
                                    Log.d(TAG, "Found learned category ID $mappedCategoryId for merchant '$merchantName'")
                                }
                            }

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
                                    categoryId = mappedCategoryId,
                                    notes = "",
                                    transactionType = potentialTxn.transactionType,
                                    sourceSmsId = potentialTxn.sourceSmsId,
                                    sourceSmsHash = potentialTxn.sourceSmsHash,
                                    source = if (mappedCategoryId != null) "Auto-Imported" else "Needs Review"
                                )
                                val newTransactionId = transactionDao.insert(newTransaction)
                                Log.d(TAG, "Transaction saved successfully with ID: $newTransactionId")

                                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                    val savedTransaction = newTransaction.copy(id = newTransactionId.toInt())
                                    if (mappedCategoryId == null && settingsRepository.isUnknownTransactionPopupEnabledBlocking()) {
                                        NotificationHelper.showTransactionNotification(context, savedTransaction)
                                    } else {
                                        NotificationHelper.showAutoSaveConfirmationNotification(context, savedTransaction)
                                    }
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
