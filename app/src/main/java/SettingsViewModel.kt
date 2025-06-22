package com.example.personalfinanceapp

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository: SettingsRepository
    // --- NEW: Add dependency on TransactionRepository to check for duplicates ---
    private val transactionRepository: TransactionRepository
    private val merchantMappingRepository: MerchantMappingRepository

    val overallBudget: StateFlow<Float>

    private val _smsMessages = MutableStateFlow<List<SmsMessage>>(emptyList())
    val smsMessages: StateFlow<List<SmsMessage>> = _smsMessages.asStateFlow()

    private val _potentialTransactions = MutableStateFlow<List<PotentialTransaction>>(emptyList())
    val potentialTransactions: StateFlow<List<PotentialTransaction>> = _potentialTransactions.asStateFlow()

    private val _selectedTransactionForApproval = MutableStateFlow<PotentialTransaction?>(null)
    val selectedTransactionForApproval: StateFlow<PotentialTransaction?> = _selectedTransactionForApproval.asStateFlow()


    init {
        val db = AppDatabase.getInstance(application)
        settingsRepository = SettingsRepository(application)
        // --- NEW: Initialize TransactionRepository ---
        transactionRepository = TransactionRepository(db.transactionDao())
        merchantMappingRepository = MerchantMappingRepository(db.merchantMappingDao())

        overallBudget = settingsRepository.getOverallBudgetForCurrentMonth()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0f
            )
    }

    fun saveOverallBudget(amountStr: String) {
        val amount = amountStr.toFloatOrNull() ?: 0f
        settingsRepository.saveOverallBudgetForCurrentMonth(amount)
    }

    fun loadAndParseSms() {
        val context = getApplication<Application>().applicationContext
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            _potentialTransactions.value = emptyList()
            return
        }

        viewModelScope.launch {
            val existingMappings = withContext(Dispatchers.IO) {
                merchantMappingRepository.allMappings.first().associateBy({ it.smsSender }, { it.merchantName })
            }
            // Step 1: Get IDs of already imported SMS messages from our DB
            val existingSmsIds = withContext(Dispatchers.IO) {
                transactionRepository.allTransactions.first()
                    .mapNotNull { transactionDetail ->
                        // Extract the ID from the notes field
                        transactionDetail.transaction.notes?.let { notes ->
                            val match = "sms_id:(\\d+)".toRegex().find(notes)
                            match?.groups?.get(1)?.value?.toLongOrNull()
                        }
                    }.toSet() // Use a Set for efficient lookup
            }

            // Step 2: Fetch raw SMS messages from the device
            val rawMessages = withContext(Dispatchers.IO) {
                val messageList = mutableListOf<SmsMessage>()
                val projection = arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
                val cursor = context.contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI, projection, null, null, "${Telephony.Sms.DATE} DESC LIMIT 200"
                )
                cursor?.use { c ->
                    val idIndex = c.getColumnIndexOrThrow(Telephony.Sms._ID)
                    val addressIndex = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                    val bodyIndex = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
                    val dateIndex = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
                    while (c.moveToNext()) {
                        val smsId = c.getLong(idIndex)
                        // --- Step 3: The De-duplication ---
                        // Only add the message if its ID is not in our set of imported IDs
                        if (!existingSmsIds.contains(smsId)) {
                            messageList.add(SmsMessage(id = smsId, sender = c.getString(addressIndex), body = c.getString(bodyIndex), date = c.getLong(dateIndex)))
                        }
                    }
                }
                messageList
            }

            // Step 4: Parse the filtered, new messages
            val parsedList = withContext(Dispatchers.Default) {
                rawMessages.mapNotNull { SmsParser.parse(it, existingMappings) }
            }

            _potentialTransactions.value = parsedList
        }
    }

    fun saveMerchantMapping(sender: String, merchantName: String) = viewModelScope.launch(Dispatchers.IO) {
        if(sender.isNotBlank() && merchantName.isNotBlank()){
            merchantMappingRepository.insert(MerchantMapping(smsSender = sender, merchantName = merchantName))
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
}
