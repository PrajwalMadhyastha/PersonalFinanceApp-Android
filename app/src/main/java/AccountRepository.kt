// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/AccountRepository.kt
// REASON: Updated the insert function to return the new account's ID (Long).
// =================================================================================
package io.pm.finlight

import kotlinx.coroutines.flow.Flow

class AccountRepository(private val accountDao: AccountDao) {
    val accountsWithBalance: Flow<List<AccountWithBalance>> = accountDao.getAccountsWithBalance()

    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()

    fun getAccountById(accountId: Int): Flow<Account?> {
        return accountDao.getAccountById(accountId)
    }

    // --- UPDATED: Returns the new row ID from the DAO ---
    suspend fun insert(account: Account): Long {
        return accountDao.insert(account)
    }

    suspend fun update(account: Account) {
        accountDao.update(account)
    }

    suspend fun delete(account: Account) {
        accountDao.delete(account)
    }
}
