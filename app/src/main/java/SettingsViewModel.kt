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
            _csvValidationReport.value = null // Reset report
            withContext(Dispatchers.IO) {
                try {
                    val db = AppDatabase.getInstance(context)
                    val accountDao = db.accountDao()
                    val categoryDao = db.categoryDao()

                    val validRows = mutableListOf<ValidatedRow>()
                    val invalidRows = mutableListOf<InvalidRow>()
                    val rowsWithNewEntities = mutableListOf<RowForCreation>()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    var lineNumber = 1

                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
                        lines.drop(1).forEach { line ->
                            lineNumber++
                            val tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.trim().removeSurrounding("\"") }

                            // Basic validation for column count, date, and amount
                            if (tokens.size < 6) { invalidRows.add(InvalidRow(lineNumber, line, "Invalid column count.")); return@forEach }
                            val date = try { dateFormat.parse(tokens[0]) } catch (e: Exception) { null }
                            if (date == null) { invalidRows.add(InvalidRow(lineNumber, line, "Invalid date format.")); return@forEach }
                            val amount = tokens[2].toDoubleOrNull()
                            if (amount == null || amount <= 0) { invalidRows.add(InvalidRow(lineNumber, line, "Invalid amount.")); return@forEach }

                            val categoryName = tokens[4]
                            val accountName = tokens[5]

                            val category = if (categoryName.isNotBlank()) categoryDao.findByName(categoryName) else null
                            val account = if (accountName.isNotBlank()) accountDao.findByName(accountName) else null

                            if (account != null && category != null) {
                                // Case 1: Everything exists.
                                validRows.add(ValidatedRow(
                                    lineNumber = lineNumber,
                                    transaction = Transaction(description = tokens[1], amount = amount, date = date.time, transactionType = tokens[3], accountId = account.id, categoryId = category.id, notes = tokens.getOrNull(6)),
                                    categoryName = categoryName,
                                    accountName = accountName
                                ))
                            } else {
                                // Case 2: Data is valid, but an entity is missing.
                                var message = "This row is valid."
                                if (category == null && categoryName.isNotBlank()) message += " A new category '$categoryName' will be created."
                                if (account == null && accountName.isNotBlank()) message += " A new account '$accountName' will be created."
                                rowsWithNewEntities.add(RowForCreation(lineNumber, tokens, message))
                            }
                        }
                    }
                    _csvValidationReport.value = CsvValidationReport(validRows, invalidRows, rowsWithNewEntities, lineNumber - 1)
                } catch (e: Exception) {
                    _csvValidationReport.value = CsvValidationReport(invalidRows = listOf(InvalidRow(0, "", "Error reading file: ${e.message}")))
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
