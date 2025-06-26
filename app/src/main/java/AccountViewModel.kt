package com.example.personalfinanceapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AccountViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AccountRepository
    private val transactionRepository: TransactionRepository

    // This is the data your UI will observe. It now comes directly from the repository.
    val accountsWithBalance: Flow<List<AccountWithBalance>>

    init {
        val db = AppDatabase.getInstance(application)
        repository = AccountRepository(db.accountDao())
        transactionRepository = TransactionRepository(db.transactionDao())

        // --- CORRECTED: Directly use the powerful query from the DAO via the repository ---
        // This is much more efficient than using 'combine' and calculating in the ViewModel.
        accountsWithBalance = repository.accountsWithBalance
    }

    fun getAccountById(accountId: Int): Flow<Account?> = repository.getAccountById(accountId)

    // --- CORRECTED: The balance calculation now correctly uses the transactionType ---
    // Helper function to calculate a single account's balance for the detail view
    fun getAccountBalance(accountId: Int): Flow<Double> {
        // This leverages the existing getTransactionsForAccount and then sums the amounts correctly.
        return transactionRepository.getTransactionsForAccount(accountId).map { transactions ->
            transactions.sumOf { if (it.transactionType == "income") it.amount else -it.amount }
        }
    }

    // Pass through for the detail screen to get full transaction details
    fun getTransactionsForAccount(accountId: Int): Flow<List<TransactionDetails>> {
        // NOTE: You'll need to add getTransactionsForAccountDetails to your TransactionRepository
        // that calls the corresponding DAO method.
        return transactionRepository.getTransactionsForAccountDetails(accountId)
    }

    // --- CORRECTED: The Account object no longer has a 'balance' parameter in its constructor ---
    fun addAccount(
        name: String,
        type: String,
    ) = viewModelScope.launch {
        if (name.isNotBlank() && type.isNotBlank()) {
            // Create the Account object without a balance.
            repository.insert(Account(name = name, type = type))
        }
    }

    fun updateAccount(account: Account) =
        viewModelScope.launch {
            repository.update(account)
        }

    fun deleteAccount(account: Account) =
        viewModelScope.launch {
            repository.delete(account)
        }
}
