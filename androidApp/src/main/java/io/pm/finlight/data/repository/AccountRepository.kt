package io.pm.finlight.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.pm.finlight.data.db.entity.Account
import io.pm.finlight.shared.db.AccountQueries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AccountRepository(
    private val accountQueries: AccountQueries,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    fun getAllAccounts(): Flow<List<Account>> =
        accountQueries.selectAll(::mapAccount).asFlow().mapToList(dispatcher)

    fun getAccountById(id: Long): Flow<Account?> =
        accountQueries.selectById(id, ::mapAccount).asFlow().mapToList(dispatcher).map { it.firstOrNull() }

    suspend fun findAccountByName(name: String): Account? = withContext(dispatcher) {
        accountQueries.selectByName(name, ::mapAccount).executeAsOneOrNull()
    }

    suspend fun insert(account: Account): Long = withContext(dispatcher) {
        accountQueries.insert(name = account.name, type = account.type)
        return@withContext accountQueries.lastInsertRowId().executeAsOne()
    }

    suspend fun update(account: Account) = withContext(dispatcher) {
        accountQueries.update(
            id = account.id.toLong(),
            name = account.name,
            type = account.type
        )
    }

    suspend fun delete(account: Account) = withContext(dispatcher) {
        accountQueries.deleteById(account.id.toLong())
    }

    private fun mapAccount(
        id: Long,
        name: String,
        type: String
    ): Account {
        return Account(
            id = id.toInt(),
            name = name,
            type = type
        )
    }
}