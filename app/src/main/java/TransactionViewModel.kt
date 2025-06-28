// =================================================================================
// FILE: /app/src/main/java/com/pm/finlight/TransactionViewModel.kt
// PURPOSE: Handles business logic for transactions.
// NOTE: Now contains the complete logic for approving SMS transactions.
// =================================================================================
package io.pm.finlight

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionRepository: TransactionRepository
    private val accountRepository: AccountRepository
    private val categoryRepository: CategoryRepository
    private val tagRepository: TagRepository

    // --- FIX: Made the database instance a class property to be accessible in other functions ---
    private val db = AppDatabase.getInstance(application)

    val allTransactions: StateFlow<List<TransactionDetails>>
    val allAccounts: StateFlow<List<Account>>
    val allCategories: Flow<List<Category>>
    val allTags: StateFlow<List<Tag>>

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError = _validationError.asStateFlow()

    private val _selectedTags = MutableStateFlow<Set<Tag>>(emptySet())
    val selectedTags = _selectedTags.asStateFlow()


    init {
        transactionRepository = TransactionRepository(db.transactionDao())
        accountRepository = AccountRepository(db.accountDao())
        categoryRepository = CategoryRepository(db.categoryDao())
        tagRepository = TagRepository(db.tagDao(), db.transactionDao())

        allTransactions =
            transactionRepository.allTransactions
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )

        allAccounts = accountRepository.allAccounts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allCategories = categoryRepository.allCategories

        allTags = tagRepository.allTags.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun onTagSelected(tag: Tag) {
        _selectedTags.update { currentTags ->
            if (tag in currentTags) {
                currentTags - tag
            } else {
                currentTags + tag
            }
        }
    }

    fun addTagOnTheGo(tagName: String) {
        if (tagName.isNotBlank()) {
            viewModelScope.launch {
                tagRepository.insert(Tag(name = tagName))
            }
        }
    }

    fun loadTagsForTransaction(transactionId: Int) {
        viewModelScope.launch {
            transactionRepository.getTagsForTransaction(transactionId).collect { tags ->
                _selectedTags.value = tags.toSet()
            }
        }
    }

    fun clearSelectedTags() {
        _selectedTags.value = emptySet()
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
                // Step 1: Find or create the account.
                val accountName = potentialTxn.potentialAccount?.formattedName ?: "Unknown Account"
                val accountType = potentialTxn.potentialAccount?.accountType ?: "General"

                var account = db.accountDao().findByName(accountName)
                if (account == null) {
                    Log.d("ViewModel_Approve", "Account '$accountName' not found. Creating new one.")
                    val newAccount = Account(name = accountName, type = accountType)
                    accountRepository.insert(newAccount)
                    // Re-fetch to be certain
                    account = db.accountDao().findByName(accountName)
                }

                if (account == null) {
                    Log.e("ViewModel_Approve", "Failed to find or create account.")
                    return@withContext false
                }

                // Step 2: Create the transaction object.
                val newTransaction = Transaction(
                    description = description,
                    categoryId = categoryId,
                    amount = potentialTxn.amount,
                    date = System.currentTimeMillis(),
                    accountId = account.id, // This is now safe
                    notes = notes,
                    transactionType = potentialTxn.transactionType,
                    sourceSmsId = potentialTxn.sourceSmsId
                )

                // Step 3: Insert transaction and tags.
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
