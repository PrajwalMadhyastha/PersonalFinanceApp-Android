package com.example.personalfinanceapp

import kotlinx.coroutines.flow.Flow

class AccountRepository(private val accountDao: AccountDao) {

    fun getAccountById(id: Int): Flow<Account?> = accountDao.getAccountById(id)

    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()

    suspend fun insert(account: Account) {
        accountDao.insert(account)
    }
    suspend fun update(account: Account) { // <-- ADD THIS
        accountDao.update(account)
    }
    suspend fun delete(account: Account) { // <-- ADD THIS
        accountDao.delete(account)
    }
}