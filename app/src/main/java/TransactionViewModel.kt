// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/TransactionViewModel.kt
// REASON: FEATURE - Added the `updateTransactionExclusion` function. This new
// function is called from the TransactionDetailScreen when the user toggles the
// "Exclude from totals" switch, and it updates the `isExcluded` flag for the
// corresponding transaction in the database.
// =================================================================================
package io.pm.finlight

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val TAG = "TransactionViewModel"

data class TransactionFilterState(
    val keyword: String = "",
    val account: Account? = null,
    val category: Category? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionRepository: TransactionRepository
    val accountRepository: AccountRepository
    val categoryRepository: CategoryRepository
    private val tagRepository: TagRepository
    private val settingsRepository: SettingsRepository
    private val smsRepository: SmsRepository
    private val context = application

    private val db = AppDatabase.getInstance(application)
    private var areTagsLoadedForCurrentTxn = false
    private var currentTxnIdForTags: Int? = null

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance())
    val selectedMonth: StateFlow<Calendar> = _selectedMonth.asStateFlow()

    private val _filterState = MutableStateFlow(TransactionFilterState())
    val filterState: StateFlow<TransactionFilterState> = _filterState.asStateFlow()

    private val combinedState: Flow<Pair<Calendar, TransactionFilterState>> =
        _selectedMonth.combine(_filterState) { month, filters ->
            Pair(month, filters)
        }

    val transactionsForSelectedMonth: StateFlow<List<TransactionDetails>> = combinedState.flatMapLatest { (calendar, filters) ->
        val monthStart = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
        val monthEnd = (calendar.clone() as Calendar).apply { add(Calendar.MONTH, 1); set(Calendar.DAY_OF_MONTH, 1); add(Calendar.DAY_OF_MONTH, -1); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
        transactionRepository.getTransactionDetailsForRange(monthStart, monthEnd, filters.keyword.takeIf { it.isNotBlank() }, filters.account?.id, filters.category?.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyIncome: StateFlow<Double> = transactionsForSelectedMonth.map { txns ->
        txns.filter { it.transaction.transactionType == "income" }.sumOf { it.transaction.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyExpenses: StateFlow<Double> = transactionsForSelectedMonth.map { txns ->
        txns.filter { it.transaction.transactionType == "expense" }.sumOf { it.transaction.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val categorySpendingForSelectedMonth: StateFlow<List<CategorySpending>> = combinedState.flatMapLatest { (calendar, filters) ->
        val monthStart = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
        val monthEnd = (calendar.clone() as Calendar).apply { add(Calendar.MONTH, 1); set(Calendar.DAY_OF_MONTH, 1); add(Calendar.DAY_OF_MONTH, -1); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
        transactionRepository.getSpendingByCategoryForMonth(monthStart, monthEnd, filters.keyword.takeIf { it.isNotBlank() }, filters.account?.id, filters.category?.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val merchantSpendingForSelectedMonth: StateFlow<List<MerchantSpendingSummary>> = combinedState.flatMapLatest { (calendar, filters) ->
        val monthStart = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
        val monthEnd = (calendar.clone() as Calendar).apply { add(Calendar.MONTH, 1); set(Calendar.DAY_OF_MONTH, 1); add(Calendar.DAY_OF_MONTH, -1); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
        transactionRepository.getSpendingByMerchantForMonth(monthStart, monthEnd, filters.keyword.takeIf { it.isNotBlank() }, filters.account?.id, filters.category?.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overallMonthlyBudget: StateFlow<Float>
    val amountRemaining: StateFlow<Float>
    val safeToSpendPerDay: StateFlow<Float>
    val allTransactions: StateFlow<List<TransactionDetails>>
    val allAccounts: StateFlow<List<Account>>
    val allCategories: Flow<List<Category>>
    val allTags: StateFlow<List<Tag>>
    private val _validationError = MutableStateFlow<String?>(null)
    val validationError = _validationError.asStateFlow()
    private val _selectedTags = MutableStateFlow<Set<Tag>>(emptySet())
    val selectedTags = _selectedTags.asStateFlow()
    private val _transactionImages = MutableStateFlow<List<TransactionImage>>(emptyList())
    val transactionImages: StateFlow<List<TransactionImage>> = _transactionImages.asStateFlow()
    val monthlySummaries: StateFlow<List<MonthlySummaryItem>>

    private val _defaultAccount = MutableStateFlow<Account?>(null)
    val defaultAccount: StateFlow<Account?> = _defaultAccount.asStateFlow()

    private val _originalSmsText = MutableStateFlow<String?>(null)
    val originalSmsText: StateFlow<String?> = _originalSmsText.asStateFlow()

    init {
        transactionRepository = TransactionRepository(db.transactionDao())
        accountRepository = AccountRepository(db.accountDao())
        categoryRepository = CategoryRepository(db.categoryDao())
        tagRepository = TagRepository(db.tagDao(), db.transactionDao())
        settingsRepository = SettingsRepository(application)
        smsRepository = SmsRepository(application)
        allTransactions = transactionRepository.allTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        allAccounts = accountRepository.allAccounts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        allCategories = categoryRepository.allCategories
        allTags = tagRepository.allTags.onEach {
            Log.d(TAG, "allTags flow collected new data. Count: ${it.size}")
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        val twelveMonthsAgo = Calendar.getInstance().apply { add(Calendar.YEAR, -1) }.timeInMillis
        monthlySummaries = transactionRepository.getMonthlyTrends(twelveMonthsAgo)
            .map { trends ->
                val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val monthMap = trends.associate {
                    val cal = Calendar.getInstance().apply {
                        time = dateFormat.parse(it.monthYear) ?: Date()
                    }
                    (cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)) to it.totalExpenses
                }

                (0..11).map { i ->
                    val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -i) }
                    val key = cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)
                    val spent = monthMap[key] ?: 0.0
                    MonthlySummaryItem(calendar = cal, totalSpent = spent)
                }.reversed()
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        overallMonthlyBudget = _selectedMonth.flatMapLatest { settingsRepository.getOverallBudgetForMonth(it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)
        amountRemaining = combine(overallMonthlyBudget, monthlyExpenses) { budget, expenses -> budget - expenses.toFloat() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)
        safeToSpendPerDay = combine(amountRemaining, _selectedMonth) { remaining, calendar ->
            val today = Calendar.getInstance()
            val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val remainingDays = if (today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) && today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)) { (lastDayOfMonth - today.get(Calendar.DAY_OF_MONTH) + 1).coerceAtLeast(1) } else if (calendar.after(today)) { lastDayOfMonth } else { 1 }
            if (remaining > 0) remaining / remainingDays else 0f
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

        viewModelScope.launch {
            _defaultAccount.value = db.accountDao().findByName("Cash Spends")
        }
    }

    fun loadOriginalSms(sourceSmsId: Long?) {
        if (sourceSmsId == null) {
            _originalSmsText.value = null
            return
        }
        viewModelScope.launch {
            val sms = getOriginalSmsMessage(sourceSmsId)
            _originalSmsText.value = sms?.body
            Log.d(TAG, "Loaded SMS for ID $sourceSmsId. Found: ${sms != null}")
        }
    }

    fun clearOriginalSms() {
        _originalSmsText.value = null
    }

    suspend fun getOriginalSmsMessage(smsId: Long): SmsMessage? {
        return withContext(Dispatchers.IO) {
            smsRepository.getSmsDetailsById(smsId)
        }
    }

    fun reparseTransactionFromSms(transactionId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val logTag = "ReparseLogic"
            Log.d(logTag, "--- Starting reparse for transactionId: $transactionId ---")

            val transaction = transactionRepository.getTransactionById(transactionId).first()
            if (transaction?.sourceSmsId == null) {
                Log.w(logTag, "FAILURE: Transaction or sourceSmsId is null.")
                return@launch
            }
            Log.d(logTag, "Found transaction: $transaction")

            val smsMessage = smsRepository.getSmsDetailsById(transaction.sourceSmsId)
            if (smsMessage == null) {
                Log.w(logTag, "FAILURE: Could not find original SMS for sourceSmsId: ${transaction.sourceSmsId}")
                return@launch
            }
            Log.d(logTag, "Found original SMS: ${smsMessage.body}")

            val potentialTxn = SmsParser.parse(smsMessage, emptyMap(), db.customSmsRuleDao(), db.merchantRenameRuleDao())
            Log.d(logTag, "SmsParser result: $potentialTxn")

            if (potentialTxn != null) {
                // UPDATE DESCRIPTION
                if (potentialTxn.merchantName != null && potentialTxn.merchantName != transaction.description) {
                    Log.d(logTag, "Updating description for txnId $transactionId from '${transaction.description}' to '${potentialTxn.merchantName}'")
                    transactionRepository.updateDescription(transactionId, potentialTxn.merchantName)
                }

                // UPDATE ACCOUNT
                potentialTxn.potentialAccount?.let { parsedAccount ->
                    Log.d(logTag, "Parsed account found: Name='${parsedAccount.formattedName}', Type='${parsedAccount.accountType}'")
                    val currentAccount = accountRepository.getAccountById(transaction.accountId).first()
                    Log.d(logTag, "Current account in DB: Name='${currentAccount?.name}'")

                    if (currentAccount?.name?.equals(parsedAccount.formattedName, ignoreCase = true) == false) {
                        Log.d(logTag, "Account names differ. Proceeding with find-or-create.")

                        var account = db.accountDao().findByName(parsedAccount.formattedName)
                        Log.d(logTag, "Attempting to find existing account by name '${parsedAccount.formattedName}'. Found: ${account != null}")

                        if (account == null) {
                            Log.d(logTag, "Account not found. Creating new one.")
                            val newAccount = Account(name = parsedAccount.formattedName, type = parsedAccount.accountType)
                            val newId = accountRepository.insert(newAccount)
                            Log.d(logTag, "Inserted new account, got ID: $newId")
                            account = db.accountDao().getAccountById(newId.toInt()).first()
                            Log.d(logTag, "Re-fetched new account from DB: $account")
                        }

                        if (account != null) {
                            Log.d(logTag, "SUCCESS: Updating transaction $transactionId to use accountId ${account.id} ('${account.name}')")
                            transactionRepository.updateAccountId(transactionId, account.id)
                        } else {
                            Log.e(logTag, "FAILURE: Failed to find or create the new account '${parsedAccount.formattedName}'.")
                        }
                    } else {
                        Log.d(logTag, "Account names are the same. No update needed.")
                    }
                } ?: Log.d(logTag, "No potential account was parsed from the SMS.")
            } else {
                Log.d(logTag, "SmsParser returned null. No updates to perform.")
            }
            Log.d(logTag, "--- Reparse finished for transactionId: $transactionId ---")
        }
    }


    fun updateFilterKeyword(keyword: String) {
        _filterState.update { it.copy(keyword = keyword) }
    }

    fun updateFilterAccount(account: Account?) {
        _filterState.update { it.copy(account = account) }
    }

    fun updateFilterCategory(category: Category?) {
        _filterState.update { it.copy(category = category) }
    }

    fun clearFilters() {
        _filterState.value = TransactionFilterState()
    }

    fun setSelectedMonth(calendar: Calendar) {
        _selectedMonth.value = calendar
    }

    fun createAccount(name: String, type: String, onAccountCreated: (Account) -> Unit) {
        if (name.isBlank() || type.isBlank()) return
        viewModelScope.launch {
            val newAccountId = accountRepository.insert(Account(name = name, type = type))
            accountRepository.getAccountById(newAccountId.toInt()).first()?.let { newAccount ->
                onAccountCreated(newAccount)
            }
        }
    }

    fun createCategory(name: String, iconKey: String, colorKey: String, onCategoryCreated: (Category) -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val usedColorKeys = allCategories.first().map { it.colorKey }
            val finalIconKey = if (iconKey == "category") "letter_default" else iconKey
            val finalColorKey = if (colorKey == "gray_light") CategoryIconHelper.getNextAvailableColor(usedColorKeys) else colorKey

            val newCategory = Category(name = name, iconKey = finalIconKey, colorKey = finalColorKey)
            val newCategoryId = categoryRepository.insert(newCategory)
            categoryRepository.getCategoryById(newCategoryId.toInt())?.let { createdCategory ->
                onCategoryCreated(createdCategory)
            }
        }
    }

    fun attachPhotoToTransaction(transactionId: Int, sourceUri: Uri) {
        viewModelScope.launch {
            val localPath = saveImageToInternalStorage(sourceUri)
            if (localPath != null) {
                transactionRepository.addImageToTransaction(transactionId, localPath)
            }
        }
    }

    fun deleteTransactionImage(image: TransactionImage) {
        viewModelScope.launch {
            transactionRepository.deleteImage(image)
            withContext(Dispatchers.IO) {
                try {
                    File(image.imageUri).delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete image file: ${image.imageUri}", e)
                }
            }
        }
    }

    fun loadImagesForTransaction(transactionId: Int) {
        viewModelScope.launch {
            transactionRepository.getImagesForTransaction(transactionId).collect {
                _transactionImages.value = it
            }
        }
    }

    private suspend fun saveImageToInternalStorage(sourceUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(sourceUri)
                val fileName = "txn_attach_${System.currentTimeMillis()}.jpg"
                val file = File(context.filesDir, fileName)
                val outputStream = FileOutputStream(file)
                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                file.absolutePath
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error saving image to internal storage", e)
                null
            }
        }
    }

    fun updateTransactionDescription(id: Int, description: String) = viewModelScope.launch {
        if (description.isNotBlank()) {
            transactionRepository.updateDescription(id, description)
        }
    }

    fun updateTransactionAmount(id: Int, amountStr: String) = viewModelScope.launch {
        amountStr.toDoubleOrNull()?.let {
            if (it > 0) {
                transactionRepository.updateAmount(id, it)
            }
        }
    }

    fun updateTransactionNotes(id: Int, notes: String) = viewModelScope.launch {
        transactionRepository.updateNotes(id, notes.takeIf { it.isNotBlank() })
    }

    fun updateTransactionCategory(id: Int, categoryId: Int?) = viewModelScope.launch {
        transactionRepository.updateCategoryId(id, categoryId)
    }

    fun updateTransactionAccount(id: Int, accountId: Int) = viewModelScope.launch {
        transactionRepository.updateAccountId(id, accountId)
    }

    fun updateTransactionDate(id: Int, date: Long) = viewModelScope.launch {
        transactionRepository.updateDate(id, date)
    }

    fun updateTransactionExclusion(id: Int, isExcluded: Boolean) = viewModelScope.launch {
        transactionRepository.updateExclusionStatus(id, isExcluded)
    }

    fun updateTagsForTransaction(transactionId: Int) = viewModelScope.launch {
        Log.d(TAG, "updateTagsForTransaction: Saving tags for txn ID $transactionId. Tags: ${_selectedTags.value.map { it.name }}")
        transactionRepository.updateTagsForTransaction(transactionId, _selectedTags.value)
    }

    fun onTagSelected(tag: Tag) {
        Log.d(TAG, "onTagSelected: Toggled tag '${tag.name}' (ID: ${tag.id})")
        _selectedTags.update { if (tag in it) it - tag else it + tag }
    }

    fun addTagOnTheGo(tagName: String) {
        if (tagName.isNotBlank()) {
            viewModelScope.launch {
                val newTag = Tag(name = tagName)
                val newId = tagRepository.insert(newTag)
                if (newId != -1L) {
                    _selectedTags.update { it + newTag.copy(id = newId.toInt()) }
                }
            }
        }
    }

    fun loadTagsForTransaction(transactionId: Int) {
        if (currentTxnIdForTags == transactionId && areTagsLoadedForCurrentTxn) {
            return
        }
        viewModelScope.launch {
            val initialTags = transactionRepository.getTagsForTransaction(transactionId).first()
            _selectedTags.value = initialTags.toSet()
            areTagsLoadedForCurrentTxn = true
            currentTxnIdForTags = transactionId
        }
    }

    fun clearSelectedTags() {
        _selectedTags.value = emptySet()
        areTagsLoadedForCurrentTxn = false
        currentTxnIdForTags = null
    }

    fun findTransactionDetailsById(id: Int): Flow<TransactionDetails?> {
        return transactionRepository.getTransactionDetailsById(id)
    }

    fun getTransactionById(id: Int): Flow<Transaction?> {
        return transactionRepository.getTransactionById(id)
    }

    suspend fun approveSmsTransaction(
        potentialTxn: PotentialTransaction,
        description: String,
        categoryId: Int?,
        notes: String?,
        tags: Set<Tag>
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val accountName = potentialTxn.potentialAccount?.formattedName ?: "Unknown Account"
                val accountType = potentialTxn.potentialAccount?.accountType ?: "General"

                var account = db.accountDao().findByName(accountName)
                if (account == null) {
                    val newAccount = Account(name = accountName, type = accountType)
                    accountRepository.insert(newAccount)
                    account = db.accountDao().findByName(accountName)
                }

                if (account == null) {
                    return@withContext false
                }

                val newTransaction = Transaction(
                    description = description,
                    categoryId = categoryId,
                    amount = potentialTxn.amount,
                    date = System.currentTimeMillis(),
                    accountId = account.id,
                    notes = notes,
                    transactionType = potentialTxn.transactionType,
                    sourceSmsId = potentialTxn.sourceSmsId,
                    sourceSmsHash = potentialTxn.sourceSmsHash,
                    source = "Reviewed Import"
                )
                transactionRepository.insertTransactionWithTags(newTransaction, tags)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    fun addTransaction(
        description: String,
        categoryId: Int?,
        amountStr: String,
        accountId: Int,
        notes: String?,
        date: Long,
        transactionType: String,
        sourceSmsId: Long?,
        sourceSmsHash: String?,
        imageUris: List<Uri>
    ): Boolean {
        _validationError.value = null

        if (description.isBlank()) {
            _validationError.value = "Description cannot be empty."
            return false
        }
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _validationError.value = "Please enter a valid, positive amount."
            return false
        }

        val newTransaction =
            Transaction(
                description = description,
                categoryId = categoryId,
                amount = amount,
                date = date,
                accountId = accountId,
                notes = notes,
                transactionType = transactionType,
                sourceSmsId = sourceSmsId,
                sourceSmsHash = sourceSmsHash,
                source = "Manual Entry"
            )
        viewModelScope.launch {
            val savedImagePaths = imageUris.mapNotNull { uri ->
                saveImageToInternalStorage(uri)
            }
            transactionRepository.insertTransactionWithTagsAndImages(
                newTransaction,
                _selectedTags.value,
                savedImagePaths
            )
        }
        return true
    }

    fun updateTransaction(transaction: Transaction): Boolean {
        _validationError.value = null

        if (transaction.description.isBlank()) {
            _validationError.value = "Description cannot be empty."
            return false
        }
        if (transaction.amount <= 0.0) {
            _validationError.value = "Amount must be a valid, positive number."
            return false
        }

        viewModelScope.launch {
            transactionRepository.updateTransactionWithTags(transaction, _selectedTags.value)
        }
        return true
    }

    fun deleteTransaction(transaction: Transaction) =
        viewModelScope.launch {
            transactionRepository.delete(transaction)
        }

    fun clearError() {
        _validationError.value = null
    }
}
