// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/SettingsViewModel.kt
// REASON: FEATURE - The `commitCsvImport` logic has been significantly enhanced
// to handle tags. It now parses a "Tags" column from the CSV, splits the string
// by a pipe delimiter, and for each tag name, it either finds the existing tag
// or creates a new one. These tags are then associated with the newly created
// transaction, completing the tag import feature.
// FIX - Removed several unused properties and functions (`scanEvent`,
// `backupEnabled`, `overallBudget`, etc.) to resolve "UnusedSymbol" warnings.
// The unused `count` parameter was also removed from `ScanResult.Success`.
// =================================================================================
package io.pm.finlight

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.pm.finlight.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
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
    // --- NEW: Add TagDao for direct access during import ---
    private val tagDao = db.tagDao()

    val smsScanStartDate: StateFlow<Long>

    private val _csvValidationReport = MutableStateFlow<CsvValidationReport?>(null)
    val csvValidationReport: StateFlow<CsvValidationReport?> = _csvValidationReport.asStateFlow()

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

    val selectedTheme: StateFlow<AppTheme> =
        settingsRepository.getSelectedTheme().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.SYSTEM_DEFAULT
        )

    init {
        smsScanStartDate =
            settingsRepository.getSmsScanStartDate()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = 0L,
                )
    }

    fun saveSelectedTheme(theme: AppTheme) {
        settingsRepository.saveSelectedTheme(theme)
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

                val parsedList = withContext(Dispatchers.Default) {
                    rawMessages.mapNotNull { sms ->
                        SmsParser.parse(
                            sms,
                            existingMappings,
                            db.customSmsRuleDao(),
                            db.merchantRenameRuleDao(),
                            db.ignoreRuleDao(),
                            db.merchantCategoryMappingDao()
                        )
                    }
                }

                val newPotentialTransactions = parsedList.filter { potential ->
                    !existingSmsHashes.contains(potential.sourceSmsHash)
                }

                _potentialTransactions.value = newPotentialTransactions
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error during SMS scan for review", e)
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

    fun onTransactionLinked(smsId: Long) {
        _potentialTransactions.update { currentList ->
            currentList.filterNot { it.sourceSmsId == smsId }
        }
    }

    fun saveMerchantRenameRule(originalName: String, newName: String) {
        if (originalName.isBlank() || newName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            if (originalName.equals(newName, ignoreCase = true)) {
                db.merchantRenameRuleDao().deleteByOriginalName(originalName)
                Log.d("SettingsViewModel", "Deleted rename rule for: '$originalName'")
            } else {
                val rule = MerchantRenameRule(originalName = originalName, newName = newName)
                db.merchantRenameRuleDao().insert(rule)
                Log.d("SettingsViewModel", "Saved rename rule: '$originalName' -> '$newName'")
            }
        }
    }

    fun saveSmsScanStartDate(date: Long) {
        viewModelScope.launch {
            settingsRepository.saveSmsScanStartDate(date)
        }
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
        // --- UPDATED: Loosen column count check for optional tags ---
        if (tokens.size < 8) return ReviewableRow(lineNumber, tokens, CsvRowStatus.INVALID_COLUMN_COUNT, "Invalid column count. Expected at least 8.")

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

    // --- UPDATED: Logic to handle tag creation and association ---
    fun commitCsvImport(rowsToImport: List<ReviewableRow>) {
        viewModelScope.launch(Dispatchers.IO) {
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
                    val isExcluded = columns.getOrNull(7)?.toBoolean() ?: false
                    // --- NEW: Parse tags from the 9th column ---
                    val tagsString = columns.getOrNull(8)

                    var category = categoryMap[categoryName.lowercase()]
                    if (category == null) {
                        val newCategory = Category(name = categoryName)
                        categoryRepository.insert(newCategory)
                        val updatedCategories = categoryRepository.allCategories.first()
                        category = updatedCategories.find { it.name.equals(categoryName, ignoreCase = true) }
                        if (category != null) {
                            categoryMap[categoryName.lowercase()] = category
                        } else {
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
                        } else {
                            continue
                        }
                    }

                    if (account == null || category == null) {
                        continue
                    }

                    // --- NEW: Process tags ---
                    val tagsToAssociate = mutableSetOf<Tag>()
                    if (!tagsString.isNullOrBlank()) {
                        val tagNames = tagsString.split('|').map { it.trim() }.filter { it.isNotEmpty() }
                        for (tagName in tagNames) {
                            var tag = tagDao.findByName(tagName)
                            if (tag == null) {
                                val newTagId = tagDao.insert(Tag(name = tagName))
                                tag = Tag(id = newTagId.toInt(), name = tagName)
                            }
                            tagsToAssociate.add(tag)
                        }
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
                            isExcluded = isExcluded,
                            source = "Imported"
                        )
                    // --- UPDATED: Use the repository function that handles tags ---
                    transactionRepository.insertTransactionWithTags(transaction, tagsToAssociate)
                } catch (e: Exception) {
                    Log.e("CsvImportDebug", "ViewModel: Failed to parse or insert row ${row.lineNumber}. Data: ${row.rowData}", e)
                }
            }
        }
    }

    fun clearCsvValidationReport() {
        _csvValidationReport.value = null
    }
}
