package io.pm.finlight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
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

        accountsWithBalance = repository.accountsWithBalance
    }

    fun getAccountById(accountId: Int): Flow<Account?> = repository.getAccountById(accountId)

    // --- FIX: Corrected the balance calculation logic ---
    // The `map` operator receives a `List<Transaction>`, so `it` inside `sumOf` is a
    // Transaction object. The properties should be accessed directly on `it`.
    fun getAccountBalance(accountId: Int): Flow<Double> {
        return transactionRepository.getTransactionsForAccount(accountId).map { transactions ->
            transactions.sumOf { if (it.transactionType == "income") it.amount else -it.amount }
        }
    }

    // Pass through for the detail screen to get full transaction details
    fun getTransactionsForAccount(accountId: Int): Flow<List<TransactionDetails>> {
        return transactionRepository.getTransactionsForAccountDetails(accountId)
    }

    fun addAccount(
        name: String,
        type: String,
    ) = viewModelScope.launch {
        if (name.isNotBlank() && type.isNotBlank()) {
            repository.insert(Account(name = name, type = type))
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
