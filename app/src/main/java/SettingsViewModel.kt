package com.example.personalfinanceapp

import android.app.Application
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val transactionRepository = TransactionRepository(AppDatabase.getInstance(application).transactionDao())
    private val merchantMappingRepository = MerchantMappingRepository(AppDatabase.getInstance(application).merchantMappingDao())
    private val context = application

    val overallBudget: StateFlow<Float> = settingsRepository.getOverallBudgetForCurrentMonth().stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0f)

    val dailyReminderEnabled: StateFlow<Boolean> = settingsRepository.getDailyReminderEnabled().stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = false)

    val weeklySummaryEnabled: StateFlow<Boolean> = settingsRepository.getWeeklySummaryEnabled().stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = true)

    val appLockEnabled: StateFlow<Boolean> = settingsRepository.getAppLockEnabled().stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = false)

    val unknownTransactionPopupEnabled: StateFlow<Boolean> = settingsRepository.getUnknownTransactionPopupEnabled().stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = true)

    private val _potentialTransactions = MutableStateFlow<List<PotentialTransaction>>(emptyList())
    val potentialTransactions: StateFlow<List<PotentialTransaction>> = _potentialTransactions.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _smsMessages = MutableStateFlow<List<SmsMessage>>(emptyList())
    val smsMessages: StateFlow<List<SmsMessage>> = _smsMessages.asStateFlow()

    fun saveOverallBudget(budget: String) {
        val budgetFloat = budget.toFloatOrNull() ?: 0f
        settingsRepository.saveOverallBudgetForCurrentMonth(budgetFloat)
    }

    fun setDailyReminder(enabled: Boolean) {
        settingsRepository.saveDailyReminderEnabled(enabled)
        if (enabled) {
            ReminderManager.scheduleDailyReminder(context)
        } else {
            ReminderManager.cancelDailyReminder(context)
        }
    }

    fun setWeeklySummaryEnabled(enabled: Boolean) {
        settingsRepository.saveWeeklySummaryEnabled(enabled)
        if (enabled) {
            ReminderManager.scheduleWeeklySummary(context)
        } else {
            ReminderManager.cancelWeeklySummary(context)
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        settingsRepository.saveAppLockEnabled(enabled)
    }

    fun setUnknownTransactionPopupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveUnknownTransactionPopupEnabled(enabled)
        }
    }

    fun rescanAllSmsMessages() {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) { return }

        viewModelScope.launch {
            _isScanning.value = true
            val existingMappings = withContext(Dispatchers.IO) {
                merchantMappingRepository.allMappings.first().associateBy({ it.smsSender }, { it.merchantName })
            }
            val existingSmsIds = withContext(Dispatchers.IO) {
                transactionRepository.allTransactions.first().mapNotNull { it.transaction.sourceSmsId }.toSet()
            }
            val rawMessages = withContext(Dispatchers.IO) {
                val messageList = mutableListOf<SmsMessage>()
                val projection = arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
                val cursor = context.contentResolver.query(Telephony.Sms.Inbox.CONTENT_URI, projection, null, null, null)
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
            _smsMessages.value = rawMessages

            val parsedList = withContext(Dispatchers.Default) {
                rawMessages.mapNotNull { SmsParser.parse(it, existingMappings) }
            }

            _potentialTransactions.value = parsedList.filter { potential -> !existingSmsIds.contains(potential.sourceSmsId) }
            _isScanning.value = false
        }
    }

    fun dismissPotentialTransaction(transaction: PotentialTransaction) {
        _potentialTransactions.value = _potentialTransactions.value.filter { it != transaction }
    }

    fun saveMerchantMapping(sender: String, merchantName: String) {
        viewModelScope.launch {
            merchantMappingRepository.insert(MerchantMapping(smsSender = sender, merchantName = merchantName))
        }
    }
}
