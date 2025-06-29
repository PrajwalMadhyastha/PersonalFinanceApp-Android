package io.pm.finlight

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val TAG = "TransactionViewModel"

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionRepository: TransactionRepository
    val accountRepository: AccountRepository
    val categoryRepository: CategoryRepository
    private val tagRepository: TagRepository
    private val context = application

    private val db = AppDatabase.getInstance(application)
    private var areTagsLoadedForCurrentTxn = false
    private var currentTxnIdForTags: Int? = null

    private val _transactionTypeFilter = MutableStateFlow<String?>(null)

    val allTransactions: StateFlow<List<TransactionDetails>> =
        _transactionTypeFilter.flatMapLatest { filterType ->
            transactionRepository.allTransactions.map { list ->
                if (filterType == null) {
                    list
                } else {
                    list.filter { it.transaction.transactionType == filterType }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allAccounts: StateFlow<List<Account>>
    val allCategories: Flow<List<Category>>
    val allTags: StateFlow<List<Tag>>

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError = _validationError.asStateFlow()

    private val _selectedTags = MutableStateFlow<Set<Tag>>(emptySet())
    val selectedTags = _selectedTags.asStateFlow()

    private val _transactionImages = MutableStateFlow<List<TransactionImage>>(emptyList())
    val transactionImages = _transactionImages.asStateFlow()

    init {
        transactionRepository = TransactionRepository(db.transactionDao())
        accountRepository = AccountRepository(db.accountDao())
        categoryRepository = CategoryRepository(db.categoryDao())
        tagRepository = TagRepository(db.tagDao(), db.transactionDao())

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

    fun updateTransactionCategory(id: Int, categoryId: Int) = viewModelScope.launch {
        transactionRepository.updateCategoryId(id, categoryId)
    }

    fun updateTransactionAccount(id: Int, accountId: Int) = viewModelScope.launch {
        transactionRepository.updateAccountId(id, accountId)
    }

    fun updateTransactionDate(id: Int, date: Long) = viewModelScope.launch {
        transactionRepository.updateDate(id, date)
    }

    fun updateTagsForTransaction(transactionId: Int) = viewModelScope.launch {
        Log.d(TAG, "updateTagsForTransaction: Saving tags for txn ID $transactionId. Tags: ${_selectedTags.value.map { it.name }}")
        transactionRepository.updateTagsForTransaction(transactionId, _selectedTags.value)
    }


    fun setTransactionTypeFilter(type: String?) {
        _transactionTypeFilter.value = type
    }

    fun onTagSelected(tag: Tag) {
        Log.d(TAG, "onTagSelected: Toggled tag '${tag.name}' (ID: ${tag.id})")
        _selectedTags.update { currentTags ->
            val newSet = if (tag in currentTags) {
                currentTags - tag
            } else {
                currentTags + tag
            }
            Log.d(TAG, "onTagSelected: New selected tags: ${newSet.map { it.name }}")
            newSet
        }
    }

    fun addTagOnTheGo(tagName: String) {
        if (tagName.isNotBlank()) {
            viewModelScope.launch {
                Log.d(TAG, "addTagOnTheGo: Attempting to add tag '$tagName'")
                val newTag = Tag(name = tagName)
                Log.d(TAG, "addTagOnTheGo: Current selected tags before insert: ${_selectedTags.value.map { it.name }}")

                val newId = tagRepository.insert(newTag)
                Log.d(TAG, "addTagOnTheGo: Inserted tag '$tagName', got new ID: $newId")

                if (newId != -1L) {
                    _selectedTags.update { currentTags ->
                        val updatedTags = currentTags + newTag.copy(id = newId.toInt())
                        Log.d(TAG, "addTagOnTheGo: Updating selected tags. New set: ${updatedTags.map { it.name }}")
                        updatedTags
                    }
                } else {
                    Log.w(TAG, "addTagOnTheGo: Failed to insert tag '$tagName', it might already exist.")
                }
            }
        }
    }

    fun loadTagsForTransaction(transactionId: Int) {
        if (currentTxnIdForTags == transactionId && areTagsLoadedForCurrentTxn) {
            Log.d(TAG, "loadTagsForTransaction: Skipped DB fetch for txn ID $transactionId, tags already loaded.")
            return
        }

        viewModelScope.launch {
            Log.d(TAG, "loadTagsForTransaction: Loading initial tags for txn ID $transactionId")
            val initialTags = transactionRepository.getTagsForTransaction(transactionId).first()
            _selectedTags.value = initialTags.toSet()
            areTagsLoadedForCurrentTxn = true
            currentTxnIdForTags = transactionId
            Log.d(TAG, "loadTagsForTransaction: Loaded initial tags: ${initialTags.map { it.name }}. Flag set to true.")
        }
    }

    fun clearSelectedTags() {
        _selectedTags.value = emptySet()
        areTagsLoadedForCurrentTxn = false
        currentTxnIdForTags = null
        Log.d(TAG, "clearSelectedTags: Cleared selected tags and reset loading flag.")
    }

    fun getTransactionDetailsById(id: Int): Flow<TransactionDetails?> {
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
                    Log.d("ViewModel_Approve", "Account '$accountName' not found. Creating new one.")
                    val newAccount = Account(name = accountName, type = accountType)
                    accountRepository.insert(newAccount)
                    account = db.accountDao().findByName(accountName)
                }

                if (account == null) {
                    Log.e("ViewModel_Approve", "Failed to find or create account.")
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
                Log.d("ViewModel_Approve", "Successfully approved and saved transaction.")
                true
            } catch (e: Exception) {
                Log.e("ViewModel_Approve", "Error approving SMS transaction", e)
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
        sourceSmsHash: String?
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
            transactionRepository.insertTransactionWithTags(newTransaction, _selectedTags.value)
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
