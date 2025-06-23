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
import kotlin.math.abs

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository: SettingsRepository
    private val transactionRepository: TransactionRepository
    private val merchantMappingRepository: MerchantMappingRepository

    val overallBudget: StateFlow<Float>

    private val _potentialTransactions = MutableStateFlow<List<PotentialTransaction>>(emptyList())
    val potentialTransactions: StateFlow<List<PotentialTransaction>> = _potentialTransactions.asStateFlow()

    // --- RE-ADDED: StateFlow for the raw SMS messages for the debug screen ---
    private val _smsMessages = MutableStateFlow<List<SmsMessage>>(emptyList())
    val smsMessages: StateFlow<List<SmsMessage>> = _smsMessages.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        settingsRepository = SettingsRepository(application)
        transactionRepository = TransactionRepository(db.transactionDao())
        merchantMappingRepository = MerchantMappingRepository(db.merchantMappingDao())

        overallBudget = settingsRepository.getOverallBudgetForCurrentMonth()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = 0f)
    }

    fun rescanAllSmsMessages() {
        val context = getApplication<Application>().applicationContext
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        viewModelScope.launch {
            _isScanning.value = true

            val existingMappings = withContext(Dispatchers.IO) {
                merchantMappingRepository.allMappings.first().associateBy({ it.smsSender }, { it.merchantName })
            }
            val existingSmsIds = withContext(Dispatchers.IO) {
                transactionRepository.getAllTransactionsSimple().first().mapNotNull { it.sourceSmsId }.toSet()
            }

            val rawMessages = withContext(Dispatchers.IO) {
                val messageList = mutableListOf<SmsMessage>()
                val projection = arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
                val cursor = context.contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI, projection, null, null, null
                )
                cursor?.use { c ->
                    val idIndex = c.getColumnIndexOrThrow(Telephony.Sms._ID)
                    val addressIndex = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                    val bodyIndex = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
                    val dateIndex = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
                    while (c.moveToNext()) {
                        messageList.add(SmsMessage(id = c.getLong(idIndex), sender = c.getString(addressIndex), body = c.getString(bodyIndex), date = c.getLong(dateIndex)))
                    }
                }
                messageList
            }

            // CORRECTED: Ensure the raw SMS list is updated for the debug screen
            _smsMessages.value = rawMessages

            val parsedList = withContext(Dispatchers.Default) {
                rawMessages.mapNotNull { SmsParser.parse(it, existingMappings) }
            }

            val newPotentialTransactions = parsedList.filter { potential ->
                !existingSmsIds.contains(potential.sourceSmsId)
            }

            _potentialTransactions.value = newPotentialTransactions
            _isScanning.value = false
        }
    }

    fun dismissPotentialTransaction(transaction: PotentialTransaction) {
        val currentList = _potentialTransactions.value.toMutableList()
        currentList.remove(transaction)
        _potentialTransactions.value = currentList
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
