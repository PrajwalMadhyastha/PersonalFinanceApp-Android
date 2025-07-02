// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/SettingsViewModel.kt
// REASON: Added StateFlow and a corresponding save function for the new monthly
// summary toggle to manage its state and trigger worker scheduling.
// =================================================================================
package io.pm.finlight

import android.Manifest
import android.app.Application
import android.app.backup.BackupManager
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

sealed class ScanResult {
    data class Success(val count: Int) : ScanResult()
    object Error : ScanResult()
}


class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val db = AppDatabase.getInstance(application)
    private val transactionRepository = TransactionRepository(db.transactionDao())
    private val merchantMappingRepository = MerchantMappingRepository(db.merchantMappingDao())
    private val context = application
    private val accountRepository = AccountRepository(db.accountDao())
    private val categoryRepository = CategoryRepository(db.categoryDao())
    val smsScanStartDate: StateFlow<Long>

    private val _scanEvent = Channel<ScanResult>()
    val scanEvent = _scanEvent.receiveAsFlow()

    private val _csvValidationReport = MutableStateFlow<CsvValidationReport?>(null)
    val csvValidationReport: StateFlow<CsvValidationReport?> = _csvValidationReport.asStateFlow()

    val overallBudget: StateFlow<Float>

    val dailyReportEnabled: StateFlow<Boolean> =
        settingsRepository.getDailyReportEnabled().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    val weeklySummaryEnabled: StateFlow<Boolean> =
        settingsRepository.getWeeklySummaryEnabled().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true,
        )

    // --- NEW: StateFlow for the monthly summary toggle ---
    val monthlySummaryEnabled: StateFlow<Boolean> =
        settingsRepository.getMonthlySummaryEnabled().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val appLockEnabled: StateFlow<Boolean> =
        settingsRepository.getAppLockEnabled().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    val unknownTransactionPopupEnabled: StateFlow<Boolean> =
        settingsRepository.getUnknownTransactionPopupEnabled().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true,
        )

    val backupEnabled: StateFlow<Boolean> =
        settingsRepository.getBackupEnabled().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true,
        )

    private val _potentialTransactions = MutableStateFlow<List<PotentialTransaction>>(emptyList())
    val potentialTransactions: StateFlow<List<PotentialTransaction>> = _potentialTransactions.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    val dailyReportTime: StateFlow<Pair<Int, Int>> =
        settingsRepository.getDailyReportTime().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Pair(9, 0)
        )

    val weeklyReportTime: StateFlow<Triple<Int, Int, Int>> =
        settingsRepository.getWeeklyReportTime().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Triple(Calendar.MONDAY, 9, 0)
        )

    val monthlyReportTime: StateFlow<Triple<Int, Int, Int>> =
        settingsRepository.getMonthlyReportTime().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Triple(1, 9, 0)
        )

    init {
        smsScanStartDate =
            settingsRepository.getSmsScanStartDate()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = 0L,
                )

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1

        overallBudget =
            settingsRepository.getOverallBudgetForMonth(currentYear, currentMonth).stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0f,
            )
    }

    fun setBackupEnabled(enabled: Boolean) {
        settingsRepository.saveBackupEnabled(enabled)
        val backupManager = BackupManager(context)
        backupManager.dataChanged()
    }

    fun rescanSmsForReview(startDate: Long?) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        viewModelScope.launch {
            _isScanning.value = true
            try {
                val rawMessages = withContext(Dispatchers.IO) {
                    SmsRepository(context).fetchAllSms(startDate)
                }

                val existingMappings = withContext(Dispatchers.IO) {
                    merchantMappingRepository.allMappings.first().associateBy({ it.smsSender }, { it.merchantName })
                }

                val existingSmsHashes = withContext(Dispatchers.IO) {
                    transactionRepository.getAllSmsHashes().first().toSet()
                }

                val customSmsRuleDao = db.customSmsRuleDao() // Get DAO instance

                val parsedList = withContext(Dispatchers.Default) {
                    rawMessages.mapNotNull { sms ->
                        // --- UPDATED: Pass the custom rule DAO to the parser ---
                        SmsParser.parse(sms, existingMappings, customSmsRuleDao)
                    }
                }

                val newPotentialTransactions = parsedList.filter { potential ->
                    !existingSmsHashes.contains(potential.sourceSmsHash)
                }

                _potentialTransactions.value = newPotentialTransactions
                _scanEvent.send(ScanResult.Success(newPotentialTransactions.size))
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error during SMS scan for review", e)
                _scanEvent.send(ScanResult.Error)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun dismissPotentialTransaction(transaction: PotentialTransaction) {
        _potentialTransactions.value = _potentialTransactions.value.filter { it != transaction }
    }

    fun onTransactionApproved(smsId: Long) {
        _potentialTransactions.update { currentList ->
            currentList.filterNot { it.sourceSmsId == smsId }
        }
    }

    fun saveMerchantMapping(
        sender: String,
        merchantName: String,
    ) {
        viewModelScope.launch {
            merchantMappingRepository.insert(MerchantMapping(smsSender = sender, merchantName = merchantName))
        }
    }

    fun saveSmsScanStartDate(date: Long) {
        viewModelScope.launch {
            settingsRepository.saveSmsScanStartDate(date)
        }
    }

    fun saveOverallBudget(budget: String) {
        val budgetFloat = budget.toFloatOrNull() ?: 0f
        settingsRepository.saveOverallBudgetForCurrentMonth(budgetFloat)
    }

    fun setDailyReportEnabled(enabled: Boolean) {
        settingsRepository.saveDailyReportEnabled(enabled)
        if (enabled) ReminderManager.scheduleDailyReport(context) else ReminderManager.cancelDailyReport(context)
    }

    fun saveDailyReportTime(hour: Int, minute: Int) {
        settingsRepository.saveDailyReportTime(hour, minute)
        if (dailyReportEnabled.value) {
            ReminderManager.scheduleDailyReport(context)
        }
    }

    fun setWeeklySummaryEnabled(enabled: Boolean) {
        settingsRepository.saveWeeklySummaryEnabled(enabled)
        if (enabled) ReminderManager.scheduleWeeklySummary(context) else ReminderManager.cancelWeeklySummary(context)
    }

    fun saveWeeklyReportTime(dayOfWeek: Int, hour: Int, minute: Int) {
        settingsRepository.saveWeeklyReportTime(dayOfWeek, hour, minute)
        if (weeklySummaryEnabled.value) {
            ReminderManager.scheduleWeeklySummary(context)
        }
    }

    // --- NEW: Function to handle the monthly summary toggle ---
    fun setMonthlySummaryEnabled(enabled: Boolean) {
        settingsRepository.saveMonthlySummaryEnabled(enabled)
        if (enabled) {
            ReminderManager.scheduleMonthlySummary(context)
        } else {
            ReminderManager.cancelMonthlySummary(context)
        }
    }

    fun saveMonthlyReportTime(dayOfMonth: Int, hour: Int, minute: Int) {
        settingsRepository.saveMonthlyReportTime(dayOfMonth, hour, minute)
        if (monthlySummaryEnabled.value) {
            ReminderManager.scheduleMonthlySummary(context)
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

    private suspend fun generateValidationReport(
        uri: Uri,
        initialData: List<ReviewableRow>? = null,
    ): CsvValidationReport {
        val accountsMap = db.accountDao().getAllAccounts().first().associateBy { it.name }
        val categoriesMap = db.categoryDao().getAllCategories().first().associateBy { it.name }

        if (initialData != null) {
            val revalidatedRows =
                initialData.map {
                    createReviewableRow(it.lineNumber, it.rowData, accountsMap, categoriesMap)
                }
            return CsvValidationReport(revalidatedRows, revalidatedRows.size)
        }

        val reviewableRows = mutableListOf<ReviewableRow>()
        var lineNumber = 1

        getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
            lines.drop(1).forEach { line ->
                lineNumber++
                val tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.trim().removeSurrounding("\"") }
                reviewableRows.add(createReviewableRow(lineNumber, tokens, accountsMap, categoriesMap))
            }
        }
        return CsvValidationReport(reviewableRows, lineNumber - 1)
    }

    private fun createReviewableRow(
        lineNumber: Int,
        tokens: List<String>,
        accounts: Map<String, Account>,
        categories: Map<String, Category>,
    ): ReviewableRow {
        if (tokens.size < 6) return ReviewableRow(lineNumber, tokens, CsvRowStatus.INVALID_COLUMN_COUNT, "Invalid column count.")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        try {
            dateFormat.parse(tokens[0])
        } catch (
            e: Exception,
        ) {
            return ReviewableRow(lineNumber, tokens, CsvRowStatus.INVALID_DATE, "Invalid date format.")
        }

        val amount = tokens[2].toDoubleOrNull()
        if (amount == null || amount <= 0) return ReviewableRow(lineNumber, tokens, CsvRowStatus.INVALID_AMOUNT, "Invalid amount.")

        val categoryName = tokens[4]
        val accountName = tokens[5]

        val categoryExists = categories.containsKey(categoryName)
        val accountExists = accounts.containsKey(accountName)

        val status =
            when {
                !accountExists && !categoryExists -> CsvRowStatus.NEEDS_BOTH_CREATION
                !accountExists -> CsvRowStatus.NEEDS_ACCOUNT_CREATION
                !categoryExists -> CsvRowStatus.NEEDS_CATEGORY_CREATION
                else -> CsvRowStatus.VALID
            }
        val message =
            when (status) {
                CsvRowStatus.VALID -> "Ready to import."
                CsvRowStatus.NEEDS_BOTH_CREATION -> "New Account & Category will be created."
                CsvRowStatus.NEEDS_ACCOUNT_CREATION -> "New Account '$accountName' will be created."
                CsvRowStatus.NEEDS_CATEGORY_CREATION -> "New Category '$categoryName' will be created."
                else -> "This row has errors and will be skipped."
            }
        return ReviewableRow(lineNumber, tokens, status, message)
    }

    fun removeRowFromReport(rowToRemove: ReviewableRow) {
        _csvValidationReport.value?.let { currentReport ->
            val updatedRows = currentReport.reviewableRows.filter { it.lineNumber != rowToRemove.lineNumber }
            _csvValidationReport.value = currentReport.copy(reviewableRows = updatedRows)
        }
    }

    fun updateAndRevalidateRow(
        lineNumber: Int,
        correctedData: List<String>,
    ) {
        viewModelScope.launch {
            _csvValidationReport.value?.let { currentReport ->
                val currentRows = currentReport.reviewableRows.toMutableList()
                val indexToUpdate = currentRows.indexOfFirst { it.lineNumber == lineNumber }

                if (indexToUpdate != -1) {
                    val revalidatedRow =
                        withContext(Dispatchers.IO) {
                            val accountsMap = db.accountDao().getAllAccounts().first().associateBy { it.name }
                            val categoriesMap = db.categoryDao().getAllCategories().first().associateBy { it.name }
                            createReviewableRow(lineNumber, correctedData, accountsMap, categoriesMap)
                        }
                    currentRows[indexToUpdate] = revalidatedRow
                    _csvValidationReport.value = currentReport.copy(reviewableRows = currentRows)
                }
            }
        }
    }

    fun commitCsvImport(rowsToImport: List<ReviewableRow>) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("CsvImportDebug", "ViewModel: commitCsvImport called with ${rowsToImport.size} rows.")

            val allAccounts = accountRepository.allAccounts.first()
            val allCategories = categoryRepository.allCategories.first()
            val accountMap = allAccounts.associateBy { it.name.lowercase() }.toMutableMap()
            val categoryMap = allCategories.associateBy { it.name.lowercase() }.toMutableMap()

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            for (row in rowsToImport) {
                try {
                    val columns = row.rowData
                    val date = dateFormat.parse(columns[0]) ?: Date()
                    val description = columns[1]
                    val amount = columns[2].toDouble()
                    val type = columns[3].lowercase(Locale.getDefault())
                    val categoryName = columns[4]
                    val accountName = columns[5]
                    val notes = columns.getOrNull(6)

                    var category = categoryMap[categoryName.lowercase()]
                    if (category == null) {
                        val newCategory = Category(name = categoryName)
                        categoryRepository.insert(newCategory)
                        val updatedCategories = categoryRepository.allCategories.first()
                        category = updatedCategories.find { it.name.equals(categoryName, ignoreCase = true) }
                        if (category != null) {
                            categoryMap[categoryName.lowercase()] = category
                            Log.d("CsvImportDebug", "ViewModel: Created and found new category '$categoryName' with ID ${category.id}")
                        } else {
                            Log.e("CsvImportDebug", "ViewModel: FAILED to create and re-find new category '$categoryName'")
                            continue
                        }
                    }

                    var account = accountMap[accountName.lowercase()]
                    if (account == null) {
                        val newAccount = Account(name = accountName, type = "Imported")
                        accountRepository.insert(newAccount)
                        val updatedAccounts = accountRepository.allAccounts.first()
                        account = updatedAccounts.find { it.name.equals(accountName, ignoreCase = true) }
                        if (account != null) {
                            accountMap[accountName.lowercase()] = account
                            Log.d("CsvImportDebug", "ViewModel: Created and found new account '$accountName' with ID ${account.id}")
                        } else {
                            Log.e("CsvImportDebug", "ViewModel: FAILED to create and re-find new account '$accountName'")
                            continue
                        }
                    }

                    if (account == null || category == null) {
                        Log.e("CsvImportDebug", "ViewModel: Could not find or create account/category for row ${row.lineNumber}. Skipping.")
                        continue
                    }

                    val transaction =
                        Transaction(
                            date = date.time,
                            amount = amount,
                            description = description,
                            notes = notes,
                            transactionType = type,
                            accountId = account.id,
                            categoryId = category.id,
                        )
                    transactionRepository.insert(transaction)
                    Log.d("CsvImportDebug", "ViewModel: Inserted transaction for row ${row.lineNumber}: '${transaction.description}'")
                } catch (e: Exception) {
                    Log.e("CsvImportDebug", "ViewModel: Failed to parse or insert row ${row.lineNumber}. Data: ${row.rowData}", e)
                }
            }
            Log.d("CsvImportDebug", "ViewModel: Finished commitCsvImport.")
        }
    }

    fun clearCsvValidationReport() {
        _csvValidationReport.value = null
    }
}
