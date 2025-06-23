package com.example.personalfinanceapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class RecurringTransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RecurringTransactionRepository
    val allRecurringTransactions: Flow<List<RecurringTransaction>>

    init {
        val recurringDao = AppDatabase.getInstance(application).recurringTransactionDao()
        repository = RecurringTransactionRepository(recurringDao)
        allRecurringTransactions = repository.getAll()
    }

    fun addRecurringTransaction(
        description: String,
        amount: Double,
        transactionType: String,
        recurrenceInterval: String,
        startDate: Long,
        accountId: Int,
        categoryId: Int?
    ) = viewModelScope.launch {
        val newRule = RecurringTransaction(
            description = description,
            amount = amount,
            transactionType = transactionType,
            recurrenceInterval = recurrenceInterval,
            startDate = startDate,
            accountId = accountId,
            categoryId = categoryId
        )
        repository.insert(newRule)
    }
}
