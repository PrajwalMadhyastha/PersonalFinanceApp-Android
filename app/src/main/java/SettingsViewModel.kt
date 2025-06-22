package com.example.personalfinanceapp

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository: SettingsRepository
    private val transactionRepository: TransactionRepository
    private val merchantMappingRepository: MerchantMappingRepository

    val overallBudget: StateFlow<Float>

    // StateFlow for raw SMS messages (for debug screen)
    private val _smsMessages = MutableStateFlow<List<SmsMessage>>(emptyList())
    val smsMessages: StateFlow<List<SmsMessage>> = _smsMessages.asStateFlow()

    private val _potentialTransactions = MutableStateFlow<List<PotentialTransaction>>(emptyList())
    val potentialTransactions: StateFlow<List<PotentialTransaction>> = _potentialTransactions.asStateFlow()

    private val _selectedTransactionForApproval = MutableStateFlow<PotentialTransaction?>(null)
    // --- CORRECTED: The type is now a single nullable PotentialTransaction, not a List ---
    val selectedTransactionForApproval: StateFlow<PotentialTransaction?> = _selectedTransactionForApproval.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        settingsRepository = SettingsRepository(application)
        transactionRepository = TransactionRepository(db.transactionDao())
        merchantMappingRepository = MerchantMappingRepository(db.merchantMappingDao())

        overallBudget = settingsRepository.getOverallBudgetForCurrentMonth()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = 0f)
    }

    fun loadAndParseSms() {
        val context = getApplication<Application>().applicationContext
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            _potentialTransactions.value = emptyList()
            return
        }

        viewModelScope.launch {
            // Step 1: Get existing mappings and already imported SMS IDs (which are timestamps)
            val existingMappings = withContext(Dispatchers.IO) {
                merchantMappingRepository.allMappings.first().associateBy({ it.smsSender }, { it.merchantName })
            }
            val existingSmsIds = withContext(Dispatchers.IO) {
                transactionRepository.getAllTransactionsSimple().first().mapNotNull { it.sourceSmsId }.toSet()
            }


            // Step 2: Fetch raw SMS messages from the device
            val rawMessages = withContext(Dispatchers.IO) {
                val messageList = mutableListOf<SmsMessage>()
                // Use DATE for timestamp, which is our unique ID
                val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
                val cursor = context.contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI, projection, null, null, "${Telephony.Sms.DATE} DESC LIMIT 200"
                )
                cursor?.use { c ->
                    val addressIndex = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                    val bodyIndex = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
                    val dateIndex = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
                    while (c.moveToNext()) {
                        val smsTimestamp = c.getLong(dateIndex)
                        messageList.add(SmsMessage(id = smsTimestamp, sender = c.getString(addressIndex), body = c.getString(bodyIndex), date = smsTimestamp))
                    }
                }
                messageList
            }

            _smsMessages.value = rawMessages

            // Step 3: Parse all messages
            val parsedList = withContext(Dispatchers.Default) {
                rawMessages.mapNotNull { SmsParser.parse(it, existingMappings) }
            }

            // Step 4: Filter out any transaction whose timestamp we've already saved
            val newPotentialTransactions = parsedList.filter { potential ->
                !existingSmsIds.contains(potential.sourceSmsId)
            }

            _potentialTransactions.value = newPotentialTransactions
        }
    }

    fun dismissPotentialTransaction(transaction: PotentialTransaction) {
        val currentList = _potentialTransactions.value.toMutableList()
        currentList.remove(transaction)
        _potentialTransactions.value = currentList
    }

    fun selectTransactionForApproval(transaction: PotentialTransaction) {
        _selectedTransactionForApproval.value = transaction
    }

    fun saveMerchantMapping(sender: String, merchantName: String) = viewModelScope.launch(Dispatchers.IO) {
        if(sender.isNotBlank() && merchantName.isNotBlank()){
            merchantMappingRepository.insert(MerchantMapping(smsSender = sender, merchantName = merchantName))
        }
    }

    fun saveOverallBudget(amountStr: String) {
        val amount = amountStr.toFloatOrNull() ?: 0f
        settingsRepository.saveOverallBudgetForCurrentMonth(amount)
    }
}
