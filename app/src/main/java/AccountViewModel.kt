// Open the file: AccountViewModel.kt

package com.example.personalfinanceapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AccountRepository

    val allAccounts: Flow<List<Account>>

    init {
        val accountDao = AppDatabase.getInstance(application).accountDao()
        repository = AccountRepository(accountDao)
        allAccounts = repository.allAccounts
    }

    fun addAccount(accountName: String, accountType: String) = viewModelScope.launch {
        val newAccount = Account(name = accountName, type = accountType, balance = 0.0)
        repository.insert(newAccount)
    }
    fun getAccountById(id: Int): Flow<Account?> = repository.getAccountById(id) // <-- ADD THIS

    fun updateAccount(account: Account) = viewModelScope.launch { // <-- ADD THIS
        repository.update(account)
    }

    fun deleteAccount(account: Account) = viewModelScope.launch { // <-- ADD THIS
        repository.delete(account)
    }

}