package com.example.personalfinanceapp

import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val transactionRepository = TransactionRepository(AppDatabase.getInstance(application).transactionDao())
    private val merchantMappingRepository = MerchantMappingRepository(AppDatabase.getInstance(application).merchantMappingDao())
    private val context = application

    // --- ADDED: StateFlow to hold the results of the CSV validation ---
    private val _csvValidationReport = MutableStateFlow<CsvValidationReport?>(null)
    val csvValidationReport: StateFlow<CsvValidationReport?> = _csvValidationReport.asStateFlow()

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
        settingsRepository.saveUnknownTransactionPopupEnabled(enabled)
    }

    /**
     * NEW: Validates a CSV file without importing it.
     * It checks for correct column count, valid data formats, and existing
     * accounts/categories, then updates the validation report StateFlow.
     */
    fun validateCsvFile(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val db = AppDatabase.getInstance(context)
                    val accountDao = db.accountDao()
                    val categoryDao = db.categoryDao()

                    val validRows = mutableListOf<ValidatedRow>()
                    val invalidRows = mutableListOf<InvalidRow>()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    var lineNumber = 1

                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
                        lines.drop(1).forEach { line ->
                            lineNumber++
                            val tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.trim().removeSurrounding("\"") }

                            if (tokens.size < 6) {
                                invalidRows.add(InvalidRow(lineNumber, line, "Invalid column count. Expected at least 6, found ${tokens.size}."))
                                return@forEach
                            }

                            val date = dateFormat.parse(tokens[0])
                            if (date == null) {
                                invalidRows.add(InvalidRow(lineNumber, line, "Invalid date format. Expected 'yyyy-MM-dd HH:mm:ss'."))
                                return@forEach
                            }

                            val amount = tokens[2].toDoubleOrNull()
                            if (amount == null || amount <= 0) {
                                invalidRows.add(InvalidRow(lineNumber, line, "Amount must be a valid, positive number."))
                                return@forEach
                            }

                            val categoryName = tokens[4]
                            val category = categoryDao.findByName(categoryName)
                            if (category == null) {
                                invalidRows.add(InvalidRow(lineNumber, line, "Category '$categoryName' not found."))
                                return@forEach
                            }

                            val accountName = tokens[5]
                            val account = accountDao.findByName(accountName)
                            if (account == null) {
                                invalidRows.add(InvalidRow(lineNumber, line, "Account '$accountName' not found."))
                                return@forEach
                            }

                            validRows.add(ValidatedRow(
                                lineNumber = lineNumber,
                                transaction = Transaction(description = tokens[1], amount = amount, date = date.time, transactionType = tokens[3], accountId = account.id, categoryId = category.id, notes = tokens.getOrNull(6)),
                                categoryName = categoryName,
                                accountName = accountName
                            ))
                        }
                    }
                    _csvValidationReport.value = CsvValidationReport(validRows, invalidRows, validRows.size + invalidRows.size)
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "CSV validation failed", e)
                    _csvValidationReport.value = CsvValidationReport(invalidRows = listOf(InvalidRow(0, "", "An error occurred during validation: ${e.message}")))
                }
            }
        }
    }

    fun clearCsvValidationReport() {
        _csvValidationReport.value = null
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
                val smsRepository = SmsRepository(context)
                smsRepository.fetchAllSms()
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
