package com.example.personalfinanceapp

import kotlinx.coroutines.flow.Flow

class AccountRepository(private val accountDao: AccountDao) {

    // Expose the new query result to the ViewModel
    val accountsWithBalance: Flow<List<AccountWithBalance>> = accountDao.getAccountsWithBalance()

    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()

    fun getAccountById(accountId: Int): Flow<Account?> {
        return accountDao.getAccountById(accountId)
    }

    suspend fun insert(account: Account) {
        accountDao.insert(account)
    }

    suspend fun update(account: Account) {
        accountDao.update(account)
    }

    suspend fun delete(account: Account) {
        accountDao.delete(account)
    }
}
