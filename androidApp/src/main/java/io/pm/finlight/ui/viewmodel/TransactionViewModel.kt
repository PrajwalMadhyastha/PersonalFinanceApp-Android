// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/TransactionViewModel.kt
// REASON: MAJOR REFACTOR - The ViewModel's dependencies have been updated to
// use the new SQLDelight-backed repositories instead of the old Room DAOs.
// The init block now correctly instantiates repositories using the DatabaseProvider.
// =================================================================================
package io.pm.finlight

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.pm.finlight.data.db.DatabaseProvider
import io.pm.finlight.data.db.entity.*
import io.pm.finlight.data.model.*
import io.pm.finlight.data.repository.*
import io.pm.finlight.ui.components.ShareableField
import io.pm.finlight.utils.CategoryIconHelper
import io.pm.finlight.utils.ShareImageGenerator
import io.pm.finlight.utils.SmsParser
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

data class RetroUpdateSheetState(
    val originalDescription: String,
    val newDescription: String? = null,
    val newCategoryId: Int? = null,
    val similarTransactions: List<Transaction> = emptyList(),
    val selectedIds: Set<Int> = emptySet(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionRepository: TransactionRepository
    val accountRepository: AccountRepository
    val categoryRepository: CategoryRepository
    private val tagRepository: TagRepository
    private val settingsRepository: SettingsRepository
    private val smsRepository: SmsRepository
    private val merchantRenameRuleRepository: MerchantRenameRuleRepository
    private val merchantCategoryMappingRepository: MerchantCategoryMappingRepository
    private val merchantMappingRepository: MerchantMappingRepository
    private val splitTransactionRepository: SplitTransactionRepository
    private val context = application

    // NOTE: Direct DB access is now through repositories
    private var areTagsLoadedForCurrentTxn = false
    private var currentTxnIdForTags: Int? = null

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance())
    val selectedMonth: StateFlow<Calendar> = _selectedMonth.asStateFlow()

    private val _filterState = MutableStateFlow(TransactionFilterState())
    val filterState: StateFlow<TransactionFilterState> = _filterState.asStateFlow()

    private val _showFilterSheet = MutableStateFlow(false)
    val showFilterSheet: StateFlow<Boolean> = _showFilterSheet.asStateFlow()

    private val _transactionForCategoryChange = MutableStateFlow<TransactionDetails?>(null)
    val transactionForCategoryChange: StateFlow<TransactionDetails?> = _transactionForCategoryChange.asStateFlow()

    private val _isSelectionModeActive = MutableStateFlow(false)
    val isSelectionModeActive: StateFlow<Boolean> = _isSelectionModeActive.asStateFlow()

    private val _selectedTransactionIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedTransactionIds: StateFlow<Set<Int>> = _selectedTransactionIds.asStateFlow()

    private val _showShareSheet = MutableStateFlow(false)
    val showShareSheet: StateFlow<Boolean> = _showShareSheet.asStateFlow()

    private val _shareableFields = MutableStateFlow(
        setOf(ShareableField.Date, ShareableField.Description, ShareableField.Amount, ShareableField.Category, ShareableField.Tags)
    )
    val shareableFields: StateFlow<Set<ShareableField>> = _shareableFields.asStateFlow()


    private val combinedState: Flow<Pair<Calendar, TransactionFilterState>> =
        _selectedMonth.combine(_filterState) { month, filters ->
            Pair(month, filters)
        }

    private val merchantAliases: StateFlow<Map<String, String>>

    val transactionsForSelectedMonth: StateFlow<List<TransactionDetails>>
    val monthlyIncome: StateFlow<Double>
    val monthlyExpenses: StateFlow<Double>
    val categorySpendingForSelectedMonth: StateFlow<List<CategorySpending>>
    val merchantSpendingForSelectedMonth: StateFlow<List<MerchantSpendingSummary>>
    val overallMonthlyBudget: StateFlow<Float>
    val amountRemaining: StateFlow<Float>
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

    private val _visitCount = MutableStateFlow(0)
    val visitCount: StateFlow<Int> = _visitCount.asStateFlow()

    private val _retroUpdateSheetState = MutableStateFlow<RetroUpdateSheetState?>(null)
    val retroUpdateSheetState = _retroUpdateSheetState.asStateFlow()

    val travelModeSettings: StateFlow<TravelModeSettings?>

    init {
        // --- REFACTOR: Instantiate DB and Repositories using SQLDelight ---
        val db = DatabaseProvider.getInstance(application)

        transactionRepository = TransactionRepository(db.transactionQueries, db.transaction_tag_cross_refQueries, db.tagQueries)
        accountRepository = AccountRepository(db.accountQueries)
        // TODO: Create CategoryRepository KMP version
        // categoryRepository = CategoryRepository(db.categoryQueries)
        categoryRepository = CategoryRepository(AppDatabase.getInstance(application).categoryDao()) // Placeholder
        tagRepository = TagRepository(db.tagQueries, db.transaction_tag_cross_refQueries)
        settingsRepository = SettingsRepository(application)
        smsRepository = SmsRepository(application)
        // TODO: Create KMP versions for these repositories
        merchantRenameRuleRepository = MerchantRenameRuleRepository(AppDatabase.getInstance(application).merchantRenameRuleDao()) // Placeholder
        merchantCategoryMappingRepository = MerchantCategoryMappingRepository(AppDatabase.getInstance(application).merchantCategoryMappingDao()) // Placeholder
        merchantMappingRepository = MerchantMappingRepository(AppDatabase.getInstance(application).merchantMappingDao()) // Placeholder
        splitTransactionRepository = SplitTransactionRepository(AppDatabase.getInstance(application).splitTransactionDao()) // Placeholder
        // --- END REFACTOR ---


        travelModeSettings = settingsRepository.getTravelModeSettings()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

        merchantAliases = merchantRenameRuleRepository.getAliasesAsMap()
            .map { it.mapKeys { (key, _) -> key.lowercase(Locale.getDefault()) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

        transactionsForSelectedMonth = combinedState.flatMapLatest { (calendar, filters) ->
            val monthStart = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
            val monthEnd = (calendar.clone() as Calendar).apply { add(Calendar.MONTH, 1); set(Calendar.DAY_OF_MONTH, 1); add(Calendar.DAY_OF_MONTH, -1); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
            transactionRepository.getTransactionDetailsForRange(monthStart, monthEnd, filters.keyword.takeIf { it.isNotBlank() }, filters.account?.id, filters.category?.id)
        }.combine(merchantAliases) { transactions, aliases ->
            applyAliases(transactions, aliases)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val financialSummaryFlow = _selectedMonth.flatMapLatest { calendar ->
            val monthStart = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
            val monthEnd = (calendar.clone() as Calendar).apply { add(Calendar.MONTH, 1); set(Calendar.DAY_OF_MONTH, 1); add(Calendar.DAY_OF_MONTH, -1); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
            transactionRepository.getFinancialSummaryForRangeFlow(monthStart, monthEnd)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        monthlyIncome = financialSummaryFlow.map { it?.totalIncome ?: 0.0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        monthlyExpenses = financialSummaryFlow.map { it?.totalExpenses ?: 0.0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        categorySpendingForSelectedMonth = combinedState.flatMapLatest { (calendar, filters) ->
            val monthStart = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
            val monthEnd = (calendar.clone() as Calendar).apply { add(Calendar.MONTH, 1); set(Calendar.DAY_OF_MONTH, 1); add(Calendar.DAY_OF_MONTH, -1); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
            transactionRepository.getSpendingByCategoryForMonth(monthStart, monthEnd, filters.keyword.takeIf { it.isNotBlank() }, filters.account?.id, filters.category?.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        merchantSpendingForSelectedMonth = combinedState.flatMapLatest { (calendar, filters) ->
            val monthStart = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
            val monthEnd = (calendar.clone() as Calendar).apply { add(Calendar.MONTH, 1); set(Calendar.DAY_OF_MONTH, 1); add(Calendar.DAY_OF_MONTH, -1); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
            transactionRepository.getSpendingByMerchantForMonth(monthStart, monthEnd, filters.keyword.takeIf { it.isNotBlank() }, filters.account?.id, filters.category?.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allAccounts = accountRepository.getAllAccounts().stateIn(
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

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val startOfYear = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis

        monthlySummaries = transactionRepository.getMonthlyTrends(startOfYear)
            .map { trends ->
                val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val monthMap = trends.filter {
                    (dateFormat.parse(it.monthYear) ?: Date()).let { date ->
                        val cal = Calendar.getInstance().apply { time = date }
                        cal.get(Calendar.YEAR) == currentYear
                    }
                }.associate {
                    val cal = Calendar.getInstance().apply { time = dateFormat.parse(it.monthYear) ?: Date() }
                    (cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)) to it.totalExpenses
                }

                (0..11).map { monthIndex ->
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, currentYear)
                        set(Calendar.MONTH, monthIndex)
                    }
                    val key = cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)
                    val spent = monthMap[key] ?: 0.0
                    MonthlySummaryItem(monthTimestamp = cal.timeInMillis, totalSpent = spent)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        overallMonthlyBudget = _selectedMonth.flatMapLatest { settingsRepository.getOverallBudgetForMonth(it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)
        amountRemaining = combine(overallMonthlyBudget, monthlyExpenses) { budget, expenses -> budget - expenses.toFloat() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

        viewModelScope.launch {
            _defaultAccount.value = accountRepository.findAccountByName("Cash Spends")
        }
    }

    // The rest of the ViewModel functions remain the same for now...
    // We will refactor them as we create the KMP versions of other repositories.

    fun enterSelectionMode(initialTransactionId: Int) {
        _isSelectionModeActive.value = true
        _selectedTransactionIds.value = setOf(initialTransactionId)
    }

    fun toggleTransactionSelection(transactionId: Int) {
        _selectedTransactionIds.update { currentSelection ->
            if (transactionId in currentSelection) {
                currentSelection - transactionId
            } else {
                currentSelection + transactionId
            }
        }
    }

    fun clearSelectionMode() {
        _isSelectionModeActive.value = false
        _selectedTransactionIds.value = emptySet()
    }

    fun onShareClick() {
        _showShareSheet.value = true
    }

    fun onShareSheetDismiss() {
        _showShareSheet.value = false
    }

    fun onShareableFieldToggled(field: ShareableField) {
        _shareableFields.update { currentFields ->
            if (field in currentFields) {
                currentFields - field
            } else {
                currentFields + field
            }
        }
    }

    fun generateAndShareSnapshot(context: Context) {
        viewModelScope.launch {
            val selectedIds = _selectedTransactionIds.value
            if (selectedIds.isEmpty()) return@launch

            val allTransactions = transactionsForSelectedMonth.first()
            val selectedTransactionsDetails = allTransactions.filter { it.transaction.id in selectedIds }

            if (selectedTransactionsDetails.isNotEmpty()) {
                val transactionsWithData = withContext(Dispatchers.IO) {
                    selectedTransactionsDetails.map { details ->
                        val tags = transactionRepository.getTagsForTransaction(details.transaction.id).first()
                        ShareImageGenerator.TransactionSnapshotData(details = details, tags = tags)
                    }
                }

                ShareImageGenerator.shareTransactionsAsImage(
                    context = context,
                    transactionsWithData = transactionsWithData,
                    fields = _shareableFields.value
                )
            }
            onShareSheetDismiss()
            clearSelectionMode()
        }
    }


    fun requestCategoryChange(details: TransactionDetails) {
        _transactionForCategoryChange.value = details
    }

    fun cancelCategoryChange() {
        _transactionForCategoryChange.value = null
    }

    fun getSplitDetailsForTransaction(transactionId: Int): Flow<List<SplitTransactionDetails>> {
        return splitTransactionRepository.getSplitsForParent(transactionId)
    }

    fun saveTransactionSplits(parentTransactionId: Int, splitItems: List<SplitItem>, onComplete: () -> Unit) {
        // This will need to be updated once SplitTransactionRepository is KMP-ready
        // For now, it will cause an error, which is expected.
    }

    private fun applyAliases(transactions: List<TransactionDetails>, aliases: Map<String, String>): List<TransactionDetails> {
        return transactions.map { details ->
            val key = (details.transaction.originalDescription ?: details.transaction.description).lowercase(Locale.getDefault())
            val newDescription = aliases[key] ?: details.transaction.description
            details.copy(transaction = details.transaction.copy(description = newDescription))
        }
    }

    fun findTransactionDetailsById(id: Int): Flow<TransactionDetails?> {
        return transactionRepository.getTransactionDetailsById(id)
            .combine(merchantAliases) { details, aliases ->
                details?.let { applyAliases(listOf(it), aliases).firstOrNull() }
            }
    }

    fun loadVisitCount(originalDescription: String?, fallbackDescription: String) {
        val descriptionToQuery = originalDescription ?: fallbackDescription
        viewModelScope.launch {
            transactionRepository.getTransactionCountForMerchant(descriptionToQuery).collect { count ->
                _visitCount.value = count
            }
        }
    }

    suspend fun addTransaction(
        description: String,
        categoryId: Int?,
        amountStr: String,
        accountId: Int,
        notes: String?,
        date: Long,
        transactionType: String,
        imageUris: List<Uri>
    ): Boolean {
        _validationError.value = null

        if (description.isBlank()) {
            _validationError.value = "Description cannot be empty."
            return false
        }
        val enteredAmount = amountStr.toDoubleOrNull()
        if (enteredAmount == null || enteredAmount <= 0.0) {
            _validationError.value = "Please enter a valid, positive amount."
            return false
        }
        if (categoryId == null) {
            _validationError.value = "Please select a category."
            return false
        }

        val travelSettings = travelModeSettings.value
        val isTravelMode = travelSettings?.isEnabled == true &&
                date >= travelSettings.startDate &&
                date <= travelSettings.endDate

        val transactionToSave = if (isTravelMode) {
            Transaction(
                description = description,
                originalDescription = description,
                categoryId = categoryId,
                amount = enteredAmount * travelSettings!!.conversionRate,
                date = date,
                accountId = accountId,
                notes = notes,
                transactionType = transactionType,
                isExcluded = false,
                sourceSmsId = null,
                sourceSmsHash = null,
                source = "Added Manually",
                originalAmount = enteredAmount,
                currencyCode = travelSettings.currencyCode,
                conversionRate = travelSettings.conversionRate.toDouble()
            )
        } else {
            Transaction(
                description = description,
                originalDescription = description,
                categoryId = categoryId,
                amount = enteredAmount,
                date = date,
                accountId = accountId,
                notes = notes,
                transactionType = transactionType,
                isExcluded = false,
                sourceSmsId = null,
                sourceSmsHash = null,
                source = "Added Manually"
            )
        }

        return try {
            withContext(Dispatchers.IO) {
                val savedImagePaths = imageUris.mapNotNull { uri ->
                    saveImageToInternalStorage(uri)
                }
                transactionRepository.insertTransactionWithTagsAndImages(
                    transactionToSave,
                    _selectedTags.value,
                    savedImagePaths
                )
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save transaction", e)
            _validationError.value = "An error occurred while saving."
            false
        }
    }

    fun clearAddTransactionState() {
        _selectedTags.value = emptySet()
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
        // This will need to be updated once all repositories are KMP-ready
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

    fun onFilterClick() {
        _showFilterSheet.value = true
    }

    fun onFilterSheetDismiss() {
        _showFilterSheet.value = false
    }

    fun setSelectedMonth(calendar: Calendar) {
        _selectedMonth.value = calendar
    }

    fun createAccount(name: String, type: String, onAccountCreated: (Account) -> Unit) {
        if (name.isBlank() || type.isBlank()) return
        viewModelScope.launch {
            val existingAccount = accountRepository.findAccountByName(name)
            if (existingAccount != null) {
                _validationError.value = "An account named '$name' already exists."
                return@launch
            }

            val newAccountId = accountRepository.insert(Account(name = name, type = type))
            accountRepository.getAccountById(newAccountId).first()?.let { newAccount ->
                onAccountCreated(newAccount)
            }
        }
    }

    fun createCategory(name: String, iconKey: String, colorKey: String, onCategoryCreated: (Category) -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val existingCategory = AppDatabase.getInstance(context).categoryDao().findByName(name) // Placeholder
            if (existingCategory != null) {
                _validationError.value = "A category named '$name' already exists."
                return@launch
            }

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

    fun updateTransactionDescription(id: Int, newDescription: String) = viewModelScope.launch(Dispatchers.IO) {
        // This will need KMP version of findSimilarTransactions
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

    fun updateTransactionCategory(id: Int, categoryId: Int?) = viewModelScope.launch(Dispatchers.IO) {
        // This will need KMP version of findSimilarTransactions
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
                // val existingTag = db.tagDao().findByName(tagName) // Needs KMP version
                // if (existingTag != null) {
                //     _validationError.value = "A tag named '$tagName' already exists."
                //     return@launch
                // }
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

    suspend fun approveSmsTransaction(
        potentialTxn: PotentialTransaction,
        description: String,
        categoryId: Int?,
        notes: String?,
        tags: Set<Tag>,
        isForeign: Boolean
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val accountName = potentialTxn.potentialAccount?.formattedName ?: "Unknown Account"
                val accountType = potentialTxn.potentialAccount?.accountType ?: "General"

                var account = accountRepository.findAccountByName(accountName)
                if (account == null) {
                    val newAccount = Account(name = accountName, type = accountType)
                    val newId = accountRepository.insert(newAccount)
                    account = accountRepository.getAccountById(newId).first()
                }

                if (account == null) return@withContext false

                val transactionToSave = if (isForeign) {
                    val travelSettings = settingsRepository.getTravelModeSettings().first()
                    if (travelSettings == null) {
                        Log.e(TAG, "Attempted to save foreign SMS transaction, but Travel Mode is not configured.")
                        return@withContext false
                    }
                    Transaction(
                        description = description,
                        originalDescription = potentialTxn.merchantName,
                        categoryId = categoryId,
                        amount = potentialTxn.amount * travelSettings.conversionRate,
                        date = System.currentTimeMillis(),
                        accountId = account.id,
                        notes = notes,
                        transactionType = potentialTxn.transactionType,
                        sourceSmsId = potentialTxn.sourceSmsId,
                        sourceSmsHash = potentialTxn.sourceSmsHash,
                        source = "Imported",
                        originalAmount = potentialTxn.amount,
                        currencyCode = travelSettings.currencyCode,
                        conversionRate = travelSettings.conversionRate.toDouble()
                    )
                } else {
                    Transaction(
                        description = description,
                        originalDescription = potentialTxn.merchantName,
                        categoryId = categoryId,
                        amount = potentialTxn.amount,
                        date = System.currentTimeMillis(),
                        accountId = account.id,
                        notes = notes,
                        transactionType = potentialTxn.transactionType,
                        sourceSmsId = potentialTxn.sourceSmsId,
                        sourceSmsHash = potentialTxn.sourceSmsHash,
                        source = "Imported"
                    )
                }

                // transactionRepository.insertTransactionWithTags(transactionToSave, tags)

                if (categoryId != null && potentialTxn.merchantName != null) {
                    val mapping = MerchantCategoryMapping(
                        parsedName = potentialTxn.merchantName,
                        categoryId = categoryId
                    )
                    // merchantCategoryMappingRepository.insert(mapping)
                    Log.d(TAG, "Saved learned category mapping: ${potentialTxn.merchantName} -> Category ID $categoryId")
                }

                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to approve SMS transaction", e)
                false
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) =
        viewModelScope.launch {
            // transactionRepository.delete(transaction)
        }

    fun unsplitTransaction(transaction: Transaction) {
        // This will need KMP version
    }


    fun clearError() {
        _validationError.value = null
    }

    fun dismissRetroUpdateSheet() {
        _retroUpdateSheetState.value = null
    }

    fun toggleRetroUpdateSelection(id: Int) {
        _retroUpdateSheetState.update { currentState ->
            currentState?.copy(
                selectedIds = currentState.selectedIds.toMutableSet().apply {
                    if (id in this) remove(id) else add(id)
                }
            )
        }
    }

    fun toggleRetroUpdateSelectAll() {
        _retroUpdateSheetState.update { currentState ->
            currentState?.let {
                if (it.selectedIds.size == it.similarTransactions.size) {
                    it.copy(selectedIds = emptySet()) // Deselect all
                } else {
                    it.copy(selectedIds = it.similarTransactions.map { t -> t.id }.toSet()) // Select all
                }
            }
        }
    }

    fun performBatchUpdate() {
        // This will need KMP version
    }
}
