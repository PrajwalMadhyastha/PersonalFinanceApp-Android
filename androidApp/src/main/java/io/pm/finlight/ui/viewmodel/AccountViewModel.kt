// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/AccountViewModel.kt
// REASON: UX REFINEMENT - The `addAccount` function now checks if an account
// with the same name already exists (case-insensitively) before attempting to
// insert. If a duplicate is found, it sends a message to the UI via the new
// `uiEvent` channel, providing clear feedback to the user instead of failing
// silently.
// =================================================================================
package io.pm.finlight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.pm.finlight.data.db.dao.AccountDao
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class AccountViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AccountRepository
    private val transactionRepository: TransactionRepository
    private val accountDao: AccountDao // Expose DAO for direct checks

    // --- NEW: Channel for sending one-time UI events like snackbars ---
    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    val accountsWithBalance: Flow<List<AccountWithBalance>>

    init {
        val db = AppDatabase.getInstance(application)
        accountDao = db.accountDao() // Initialize DAO
        repository = AccountRepository(accountDao)
        transactionRepository = TransactionRepository(db.transactionDao())

        accountsWithBalance = repository.accountsWithBalance
    }

    fun getAccountById(accountId: Int): Flow<Account?> = repository.getAccountById(accountId)

    fun getAccountBalance(accountId: Int): Flow<Double> {
        return transactionRepository.getTransactionsForAccount(accountId).map { transactions ->
            transactions.sumOf { if (it.transactionType == "income") it.amount else -it.amount }
        }
    }

    fun getTransactionsForAccount(accountId: Int): Flow<List<TransactionDetails>> {
        return transactionRepository.getTransactionsForAccountDetails(accountId)
    }

    // --- UPDATED: Add pre-check and user feedback for duplicates ---
    fun addAccount(
        name: String,
        type: String,
    ) = viewModelScope.launch {
        if (name.isNotBlank() && type.isNotBlank()) {
            // Check if an account with this name already exists
            val existingAccount = accountDao.findByName(name)
            if (existingAccount != null) {
                _uiEvent.send("An account named '$name' already exists.")
            } else {
                repository.insert(Account(name = name, type = type))
                _uiEvent.send("Account '$name' created.")
            }
        }
    }

    fun updateAccount(account: Account) =
        viewModelScope.launch {
            repository.update(account)
        }

    fun renameAccount(accountId: Int, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val accountToUpdate = repository.getAccountById(accountId).firstOrNull()
            accountToUpdate?.let {
                updateAccount(it.copy(name = newName))
            }
        }
    }


    fun deleteAccount(account: Account) =
        viewModelScope.launch {
            repository.delete(account)
        }
}