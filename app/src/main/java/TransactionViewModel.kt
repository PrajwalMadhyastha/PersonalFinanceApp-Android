// =================================================================================
// FILE: /app/src/main/java/com/pm/finlight/TransactionViewModel.kt
// PURPOSE: Handles business logic for the transaction list and add/edit screens.
// NOTE: Added extensive logging to debug data flow.
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

    val allTransactions: StateFlow<List<TransactionDetails>>

    val allAccounts: Flow<List<Account>>
    val allCategories: Flow<List<Category>>

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError = _validationError.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        transactionRepository = TransactionRepository(db.transactionDao())
        accountRepository = AccountRepository(db.accountDao())
        categoryRepository = CategoryRepository(db.categoryDao())

        allTransactions =
            transactionRepository.allTransactions
                .onEach { transactions ->
                    // DEBUG LOG: See what the ViewModel is receiving from the repository
                    Log.d("TransactionFlowDebug", "ViewModel Received Update. Count: ${transactions.size}. Newest: ${transactions.firstOrNull()?.transaction?.description}")
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )

        allAccounts = accountRepository.allAccounts
        allCategories = categoryRepository.allCategories
    }

    fun getTransactionById(id: Int): Flow<Transaction?> {
        return transactionRepository.getTransactionById(id)
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
            // DEBUG LOG: See when a transaction is being added
            Log.d("TransactionFlowDebug", "ViewModel: Attempting to add transaction '${newTransaction.description}'")
            transactionRepository.insert(newTransaction)
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
            transactionRepository.update(transaction)
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
