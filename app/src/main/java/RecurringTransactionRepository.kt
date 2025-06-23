package com.example.personalfinanceapp

import kotlinx.coroutines.flow.Flow

class RecurringTransactionRepository(private val recurringTransactionDao: RecurringTransactionDao) {

    fun getAll(): Flow<List<RecurringTransaction>> {
        return recurringTransactionDao.getAll()
    }

    suspend fun insert(recurringTransaction: RecurringTransaction) {
        recurringTransactionDao.insert(recurringTransaction)
    }
}
