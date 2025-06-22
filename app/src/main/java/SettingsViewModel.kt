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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository: SettingsRepository

    val overallBudget: StateFlow<Float>

    // StateFlow for raw SMS messages (for debug screen)
    private val _smsMessages = MutableStateFlow<List<SmsMessage>>(emptyList())
    val smsMessages: StateFlow<List<SmsMessage>> = _smsMessages.asStateFlow()

    // --- NEW: StateFlow for parsed transactions ---
    private val _potentialTransactions = MutableStateFlow<List<PotentialTransaction>>(emptyList())
    val potentialTransactions: StateFlow<List<PotentialTransaction>> = _potentialTransactions.asStateFlow()


    init {
        settingsRepository = SettingsRepository(application)
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

    /**
     * Loads SMS messages and then parses them to find potential transactions.
     * Updates both the raw SMS list and the potential transactions list.
     */
    fun loadAndParseSms() {
        val context = getApplication<Application>().applicationContext

        // Ensure permission is granted before proceeding
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            _smsMessages.value = emptyList()
            _potentialTransactions.value = emptyList()
            return
        }

        viewModelScope.launch {
            // Step 1: Fetch raw SMS messages on an I/O thread
            val rawMessages = withContext(Dispatchers.IO) {
                val messageList = mutableListOf<SmsMessage>()
                val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
                val cursor = context.contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${Telephony.Sms.DATE} DESC LIMIT 200" // Limit for performance
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

            // Update the raw messages StateFlow (for the debug screen)
            _smsMessages.value = rawMessages

            // Step 2: Parse the messages on a computational thread
            val parsedList = withContext(Dispatchers.Default) {
                rawMessages.mapNotNull { SmsParser.parse(it.body) }
            }

            // Step 3: Update the potential transactions StateFlow
            _potentialTransactions.value = parsedList
        }
    }
}
