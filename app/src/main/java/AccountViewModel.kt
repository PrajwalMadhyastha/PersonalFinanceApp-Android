package com.example.personalfinanceapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val accountRepository: AccountRepository
    private val transactionRepository: TransactionRepository

    // This new property will be consumed by the UI.
    // It combines accounts and transactions to produce a list of accounts with their calculated balances.
    val accountsWithBalance: Flow<List<AccountWithBalance>>

    init {
        val accountDao = AppDatabase.getInstance(application).accountDao()
        val transactionDao = AppDatabase.getInstance(application).transactionDao()

        accountRepository = AccountRepository(accountDao)
        transactionRepository = TransactionRepository(transactionDao)

        // The 'combine' operator takes two Flows (all accounts and all transactions).
        // Whenever either flow emits a new list, this block of code is executed.
        accountsWithBalance = accountRepository.allAccounts.combine(
            transactionRepository.getAllTransactionsSimple()
        ) { accounts, transactions ->
            // We now have the latest lists of accounts and transactions.
            // We map over the list of accounts...
            accounts.map { account ->
                // ...and for each account, we calculate its balance by filtering the
                // transactions list and summing the amounts.
                val calculatedBalance = transactions
                    .filter { it.accountId == account.id }
                    .sumOf { it.amount }
                // Finally, we create our new data object.
                AccountWithBalance(account = account, balance = calculatedBalance)
            }
        }
    }

    // --- The rest of the functions are unchanged ---

    fun getAccountBalance(accountId: Int): Flow<Double> {
        return transactionRepository.getTransactionsForAccount(accountId)
            .map { transactions ->
                transactions.sumOf { it.amount }
            }
    }

    fun getAccountById(id: Int): Flow<Account?> = accountRepository.getAccountById(id)

    fun addAccount(accountName: String, accountType: String) = viewModelScope.launch {
        val newAccount = Account(name = accountName, type = accountType, balance = 0.0)
        accountRepository.insert(newAccount)
    }

    fun updateAccount(account: Account) = viewModelScope.launch {
        accountRepository.update(account)
    }

    fun deleteAccount(account: Account) = viewModelScope.launch {
        accountRepository.delete(account)
    }

    fun getTransactionsForAccount(accountId: Int): Flow<List<Transaction>> {
        return transactionRepository.getTransactionsForAccount(accountId)
    }
}