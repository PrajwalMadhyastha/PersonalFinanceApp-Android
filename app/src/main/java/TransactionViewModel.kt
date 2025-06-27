// =================================================================================
// FILE: /app/src/main/java/com/pm/finlight/TransactionViewModel.kt
// PURPOSE: Handles business logic for transactions, now including tag management.
// =================================================================================
package io.pm.finlight

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionRepository: TransactionRepository
    private val accountRepository: AccountRepository
    private val categoryRepository: CategoryRepository
    // --- NEW: Add TagRepository dependency ---
    private val tagRepository: TagRepository

    val allTransactions: StateFlow<List<TransactionDetails>>

    val allAccounts: Flow<List<Account>>
    val allCategories: Flow<List<Category>>
    // --- NEW: Expose all available tags to the UI ---
    val allTags: StateFlow<List<Tag>>

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError = _validationError.asStateFlow()

    // --- NEW: State to hold the currently selected tags for a transaction ---
    private val _selectedTags = MutableStateFlow<Set<Tag>>(emptySet())
    val selectedTags = _selectedTags.asStateFlow()


    init {
        val db = AppDatabase.getInstance(application)
        transactionRepository = TransactionRepository(db.transactionDao())
        accountRepository = AccountRepository(db.accountDao())
        categoryRepository = CategoryRepository(db.categoryDao())
        // --- NEW: Initialize TagRepository ---
        tagRepository = TagRepository(db.tagDao(), db.transactionDao())

        allTransactions =
            transactionRepository.allTransactions
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )

        allAccounts = accountRepository.allAccounts
        allCategories = categoryRepository.allCategories

        // --- NEW: Fetch all tags and expose as StateFlow ---
        allTags = tagRepository.allTags.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // --- NEW: Methods to manage tag selection ---
    fun onTagSelected(tag: Tag) {
        _selectedTags.update { currentTags ->
            if (tag in currentTags) {
                currentTags - tag
            } else {
                currentTags + tag
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

    // --- UPDATED: Now saves the transaction with its selected tags ---
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

    // --- UPDATED: Now updates the transaction with its selected tags ---
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
