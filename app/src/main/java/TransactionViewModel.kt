package com.example.personalfinanceapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionRepository: TransactionRepository
    private val accountRepository: AccountRepository
    // --- NEW: Add a repository for categories ---
    private val categoryRepository: CategoryRepository

    val allTransactions: Flow<List<TransactionDetails>>
    val allAccounts: Flow<List<Account>>
    // --- NEW: Expose the list of all categories to the UI ---
    val allCategories: Flow<List<Category>>

    init {
        val db = AppDatabase.getInstance(application)
        transactionRepository = TransactionRepository(db.transactionDao())
        accountRepository = AccountRepository(db.accountDao())
        // --- NEW: Initialize the category repository ---
        categoryRepository = CategoryRepository(db.categoryDao())

        allTransactions = transactionRepository.allTransactions
        allAccounts = accountRepository.allAccounts
        // --- NEW: Get all categories from the new repository ---
        allCategories = categoryRepository.allCategories
    }

    fun getTransactionById(id: Int): Flow<Transaction?> {
        return transactionRepository.getTransactionById(id)
    }

    // Updated to accept categoryId
    fun addTransaction(description: String, categoryId: Int?, amountStr: String, accountId: Int) {
        val amount = amountStr.toDoubleOrNull() ?: return

        val newTransaction = Transaction(
            description = description,
            categoryId = categoryId,
            amount = amount,
            date = System.currentTimeMillis(),
            accountId = accountId
        )
        viewModelScope.launch {
            transactionRepository.insert(newTransaction)
        }
    }

    fun updateTransaction(transaction: Transaction) = viewModelScope.launch {
        transactionRepository.update(transaction)
    }

    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch {
        transactionRepository.delete(transaction)
    }
}
