package com.example.personalfinanceapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filterNotNull
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
    val allTransactions: Flow<List<Transaction>>

    init {
        val transactionDao = AppDatabase.getInstance(application).transactionDao()
        repository = TransactionRepository(transactionDao)
        allTransactions = repository.allTransactions
    }

    // --- ADD THESE NEW FUNCTIONS ---

    /**
     * Retrieves a single transaction by ID.
     * We use filterNotNull() to ensure the UI only tries to process non-null values.
     */
    fun getTransactionById(id: Int): Flow<Transaction> {
        return repository.getTransactionById(id).filterNotNull()
    }

    /**
     * Launches a coroutine to update a transaction.
     */
    fun updateTransaction(transaction: Transaction) = viewModelScope.launch {
        repository.update(transaction)
    }

    /**
     * Launches a coroutine to delete a transaction.
     */
    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch {
        repository.delete(transaction)
    }

    // --- END OF NEW FUNCTIONS ---

    fun addTransaction(description: String, amountStr: String) {
        // 1. Don't add if the inputs are blank
        if (description.isBlank() || amountStr.isBlank()) {
            return
        }

        // --- THIS IS THE FIX ---
        // 2. Safely convert the amount string to a Double.
        // If the conversion fails (e.g., user types "abc"), do nothing.
        val amount = amountStr.toDoubleOrNull() ?: return

        // 3. Add a check to ensure the amount is a positive number.
        if (amount <= 0) {
            return
        }
        // --- END OF FIX ---

        // 4. Launch the coroutine to insert the new transaction
        viewModelScope.launch {
            val newTransaction = Transaction(
                description = description,
                amount = amount, // Use the correctly parsed Double
                date = Date().time
            )
            repository.insert(newTransaction)
        }
    }
}