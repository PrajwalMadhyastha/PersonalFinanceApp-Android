package com.example.personalfinanceapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionRepository: TransactionRepository
    private val accountRepository: AccountRepository
    private val categoryRepository: CategoryRepository

    val allTransactions: Flow<List<TransactionDetails>>
    val allAccounts: Flow<List<Account>>
    val allCategories: Flow<List<Category>>

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError = _validationError.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        transactionRepository = TransactionRepository(db.transactionDao())
        accountRepository = AccountRepository(db.accountDao())
        categoryRepository = CategoryRepository(db.categoryDao())

        allTransactions = transactionRepository.allTransactions
        allAccounts = accountRepository.allAccounts
        allCategories = categoryRepository.allCategories
    }

    fun getTransactionById(id: Int): Flow<Transaction?> {
        return transactionRepository.getTransactionById(id)
    }

    // --- UPDATED: Now accepts transactionType ---
    fun addTransaction(
        description: String,
        categoryId: Int?,
        amountStr: String,
        accountId: Int,
        notes: String?,
        date: Long,
        transactionType: String // New parameter
    ): Boolean {
        if (description.isBlank()) {
            _validationError.value = "Description cannot be empty."
            return false
        }
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0.0) { // Amount must be positive
            _validationError.value = "Please enter a valid, positive amount."
            return false
        }

        val newTransaction = Transaction(
            description = description,
            categoryId = categoryId,
            amount = amount, // Amount is now always positive
            date = date,
            accountId = accountId,
            notes = notes,
            transactionType = transactionType // Set the type
        )
        viewModelScope.launch {
            transactionRepository.insert(newTransaction)
        }
        _validationError.value = null // Clear error on success
        return true
    }

    // --- UPDATED: Validation logic simplified ---
    fun updateTransaction(transaction: Transaction): Boolean {
        if (transaction.description.isBlank()) {
            _validationError.value = "Description cannot be empty."
            return false
        }
        if (transaction.amount <= 0.0) { // Amount must be positive
            _validationError.value = "Amount must be a valid, positive number."
            return false
        }

        viewModelScope.launch {
            transactionRepository.update(transaction)
        }
        _validationError.value = null // Clear error on success
        return true
    }

    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch {
        transactionRepository.delete(transaction)
    }

    fun clearError() {
        _validationError.value = null
    }
}
