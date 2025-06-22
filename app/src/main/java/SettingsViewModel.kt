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

    // --- NEW: StateFlow to hold the list of SMS messages ---
    private val _smsMessages = MutableStateFlow<List<SmsMessage>>(emptyList())
    val smsMessages: StateFlow<List<SmsMessage>> = _smsMessages.asStateFlow()

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
     * Loads SMS messages from the device's inbox.
     * This function checks for permission before querying the ContentProvider.
     */
    fun loadSmsMessages() {
        val context = getApplication<Application>().applicationContext

        // 1. Check if permission is granted. If not, do nothing.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            // In a real app, you might show a message to the user.
            _smsMessages.value = emptyList() // Ensure the list is clear if permission is revoked.
            return
        }

        viewModelScope.launch {
            // Use withContext(Dispatchers.IO) for a blocking call like contentResolver.query
            val messages = withContext(Dispatchers.IO) {
                val messageList = mutableListOf<SmsMessage>()

                // 2. Define the columns you want to retrieve from the SMS table.
                val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)

                // 3. Query the SMS ContentProvider.
                val cursor = context.contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${Telephony.Sms.DATE} DESC" // Get newest messages first
                )

                // 4. Safely process the cursor.
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
                messageList // Return the populated list
            }

            // 5. Update the StateFlow with the new list of messages.
            _smsMessages.value = messages
        }
    }
}
