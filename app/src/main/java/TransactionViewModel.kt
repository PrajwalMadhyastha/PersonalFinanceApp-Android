package com.example.personalfinanceapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    // 1. Declare properties for BOTH repositories.
    private val transactionRepository: TransactionRepository
    private val accountRepository: AccountRepository

    // 2. Declare the public data Flows that the UI will observe.
    val allTransactions: Flow<List<TransactionWithAccount>>
    val allAccounts: Flow<List<Account>> // This is needed for the dropdowns

    init {
        // 3. Get instances of BOTH DAOs from the database.
        val transactionDao = AppDatabase.getInstance(application).transactionDao()
        val accountDao = AppDatabase.getInstance(application).accountDao()

        // 4. Initialize BOTH repositories.
        transactionRepository = TransactionRepository(transactionDao)
        accountRepository = AccountRepository(accountDao)

        // 5. Initialize the public Flows using their respective repositories.
        allTransactions = transactionRepository.allTransactions
        allAccounts = accountRepository.allAccounts
    }

    /**
     * Gets a single transaction by its ID for the edit screen.
     */
    fun getTransactionById(id: Int): Flow<Transaction?> {
        return transactionRepository.getTransactionById(id)
    }

    /**
     * Adds a new transaction.
     */
    fun addTransaction(description: String, amountStr: String, accountId: Int) {
        // Basic validation
        val amount = amountStr.toDoubleOrNull() ?: return

        val newTransaction = Transaction(
            description = description,
            amount = amount,
            date = System.currentTimeMillis(),
            accountId = accountId
        )
        viewModelScope.launch {
            transactionRepository.insert(newTransaction)
        }
    }

    /**
     * Updates an existing transaction.
     */
    fun updateTransaction(transaction: Transaction) = viewModelScope.launch {
        transactionRepository.update(transaction)
    }

    /**
     * Deletes a transaction.
     */
    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch {
        transactionRepository.delete(transaction)
    }
}