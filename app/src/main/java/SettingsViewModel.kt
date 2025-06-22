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
            // Step 1: Fetch raw SMS messages
            val rawMessages = withContext(Dispatchers.IO) {
                // ... (SMS fetching logic remains the same) ...
                val messageList = mutableListOf<SmsMessage>()
                val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
                val cursor = context.contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${Telephony.Sms.DATE} DESC LIMIT 200"
                )

                cursor?.use { c ->
                    val addressIndex = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                    val bodyIndex = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
                    val dateIndex = c.getColumnIndexOrThrow(Telephony.Sms.DATE)

                    while (c.moveToNext()) {
                        messageList.add(
                            SmsMessage(
                                sender = c.getString(addressIndex),
                                body = c.getString(bodyIndex),
                                date = c.getLong(dateIndex)
                            )
                        )
                    }
                }
                messageList
            }
            _smsMessages.value = rawMessages

            // Step 2: Parse all fetched messages
            val parsedList = withContext(Dispatchers.Default) {
                rawMessages.mapNotNull { SmsParser.parse(it.body) }
            }

            // --- NEW: Step 3: Filter out duplicates ---
            val existingTransactions = transactionRepository.allTransactions.first() // Get current list from db
            val newPotentialTransactions = parsedList.filter { potential ->
                // A transaction is considered a duplicate if we find one in the DB
                // with the same amount and a description that matches the parsed merchant name.
                // This is a simple heuristic and can be improved later.
                existingTransactions.none { existing ->
                    val descriptionMatch = existing.transaction.description == potential.merchantName
                    val amountMatch = existing.transaction.amount == potential.amount
                    descriptionMatch && amountMatch
                }
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
}
