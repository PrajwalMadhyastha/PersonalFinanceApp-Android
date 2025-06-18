package com.example.personalfinanceapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Date

/**
 * The ViewModel for our main screen. It holds the UI state and handles business logic.
 *
 * We inherit from AndroidViewModel because we need the Application context
 * to initialize our database singleton.
 */
class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    // The private repository instance.
    private val repository: TransactionRepository

    // The Flow of transactions that the UI will observe.
    val allTransactions: Flow<List<Transaction>>

    init {
        // This is the setup logic. It gets the DAO from our database singleton,
        // then creates the repository instance using that DAO.
        val transactionDao = AppDatabase.getInstance(application).transactionDao()
        repository = TransactionRepository(transactionDao)
        allTransactions = repository.allTransactions
    }

    /**
     * A function that can be called from the UI to add a new transaction.
     * It creates the Transaction object and calls the repository to insert it.
     */
    fun addTransaction(description: String, amountStr: String) {
        // Don't add if the inputs are blank
        if (description.isBlank() || amountStr.isBlank()) {
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            // In a real app, you might show an error message to the user
            return
        }

        // The viewModelScope.launch ensures this database operation happens
        // on a background thread, not on the main UI thread.
        viewModelScope.launch {
            val newTransaction = Transaction(
                description = description,
                amount = amount,
                date = Date().time // Get the current time as a Long
            )
            repository.insert(newTransaction)
        }
    }
}