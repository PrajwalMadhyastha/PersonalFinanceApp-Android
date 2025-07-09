// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/RecurringTransactionRepository.kt
// REASON: FEATURE - The repository has been updated to expose `getById`,
// `update`, and `delete` functions, providing a complete data access layer
// for managing recurring rules. This resolves the "Unresolved reference" errors
// in the ViewModel.
// =================================================================================
package io.pm.finlight

import kotlinx.coroutines.flow.Flow

class RecurringTransactionRepository(private val recurringTransactionDao: RecurringTransactionDao) {
    fun getAll(): Flow<List<RecurringTransaction>> {
        return recurringTransactionDao.getAllRulesFlow()
    }

    fun getById(id: Int): Flow<RecurringTransaction?> {
        return recurringTransactionDao.getById(id)
    }

    suspend fun insert(recurringTransaction: RecurringTransaction) {
        recurringTransactionDao.insert(recurringTransaction)
    }

    suspend fun update(recurringTransaction: RecurringTransaction) {
        recurringTransactionDao.update(recurringTransaction)
    }

    suspend fun delete(recurringTransaction: RecurringTransaction) {
        recurringTransactionDao.delete(recurringTransaction)
    }
}
