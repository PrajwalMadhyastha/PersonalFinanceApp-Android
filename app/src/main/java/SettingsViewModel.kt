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
    private val db = AppDatabase.getInstance(application)
    private val transactionRepository = TransactionRepository(db.transactionDao())
    private val merchantMappingRepository = MerchantMappingRepository(db.merchantMappingDao())
    private val context = application

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
        if (enabled) ReminderManager.scheduleDailyReminder(context) else ReminderManager.cancelDailyReminder(context)
    }

    fun setWeeklySummaryEnabled(enabled: Boolean) {
        settingsRepository.saveWeeklySummaryEnabled(enabled)
        if (enabled) ReminderManager.scheduleWeeklySummary(context) else ReminderManager.cancelWeeklySummary(context)
    }

    fun setAppLockEnabled(enabled: Boolean) {
        settingsRepository.saveAppLockEnabled(enabled)
    }

    fun setUnknownTransactionPopupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveUnknownTransactionPopupEnabled(enabled)
        }
    }

    fun validateCsvFile(uri: Uri) {
        viewModelScope.launch {
            _csvValidationReport.value = null
            withContext(Dispatchers.IO) {
                try {
                    val report = generateValidationReport(uri)
                    _csvValidationReport.value = report
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "CSV validation failed", e)
                }
            }
        }
    }

    private suspend fun generateValidationReport(uri: Uri): CsvValidationReport {
        val accountsMap = db.accountDao().getAllAccounts().first().associateBy { it.name }
        val categoriesMap = db.categoryDao().getAllCategories().first().associateBy { it.name }
        val reviewableRows = mutableListOf<ReviewableRow>()
        var lineNumber = 1

        context.contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
            lines.drop(1).forEach { line ->
                lineNumber++
                val tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.trim().removeSurrounding("\"") }
                reviewableRows.add(createReviewableRow(lineNumber, tokens, accountsMap, categoriesMap))
            }
        }
        return CsvValidationReport(reviewableRows, lineNumber - 1)
    }

    private fun createReviewableRow(lineNumber: Int, tokens: List<String>, accounts: Map<String, Account>, categories: Map<String, Category>): ReviewableRow {
        if (tokens.size < 6) return ReviewableRow(lineNumber, tokens, CsvRowStatus.INVALID_COLUMN_COUNT, "Invalid column count.")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = try { dateFormat.parse(tokens[0]) } catch (e: Exception) { null }
        if (date == null) return ReviewableRow(lineNumber, tokens, CsvRowStatus.INVALID_DATE, "Invalid date format.")

        val amount = tokens[2].toDoubleOrNull()
        if (amount == null || amount <= 0) return ReviewableRow(lineNumber, tokens, CsvRowStatus.INVALID_AMOUNT, "Invalid amount.")

        val categoryName = tokens[4]
        val accountName = tokens[5]

        val categoryExists = categories.containsKey(categoryName)
        val accountExists = accounts.containsKey(accountName)

        val status = when {
            !accountExists && !categoryExists -> CsvRowStatus.NEEDS_BOTH_CREATION
            !accountExists -> CsvRowStatus.NEEDS_ACCOUNT_CREATION
            !categoryExists -> CsvRowStatus.NEEDS_CATEGORY_CREATION
            else -> CsvRowStatus.VALID
        }
        val message = when (status) {
            CsvRowStatus.NEEDS_BOTH_CREATION -> "New Account & Category will be created."
            CsvRowStatus.NEEDS_ACCOUNT_CREATION -> "New Account '$accountName' will be created."
            CsvRowStatus.NEEDS_CATEGORY_CREATION -> "New Category '$categoryName' will be created."
            else -> "Ready to import."
        }
        return ReviewableRow(lineNumber, tokens, status, message)
    }

    fun commitCsvImport(rows: List<ReviewableRow>) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(context)
            val transactionsToInsert = mutableListOf<Transaction>()

            rows.forEach { row ->
                val tokens = row.rowData
                var account = db.accountDao().findByName(tokens[5])
                if (account == null) {
                    db.accountDao().insert(Account(name = tokens[5], type = "Imported"))
                    account = db.accountDao().findByName(tokens[5])
                }
                var category = db.categoryDao().findByName(tokens[4])
                if (category == null) {
                    db.categoryDao().insert(Category(name = tokens[4]))
                    category = db.categoryDao().findByName(tokens[4])
                }

                if (account != null && category != null) {
                    val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(tokens[0])?.time ?: 0L
                    transactionsToInsert.add(Transaction(
                        description = tokens[1],
                        amount = tokens[2].toDouble(),
                        date = date,
                        transactionType = tokens[3],
                        accountId = account.id,
                        categoryId = category.id,
                        notes = tokens.getOrNull(6)
                    ))
                }
            }
            db.transactionDao().insertAll(transactionsToInsert)
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
