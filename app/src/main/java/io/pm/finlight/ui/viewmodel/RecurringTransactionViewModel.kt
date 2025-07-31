// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/RecurringTransactionViewModel.kt
// REASON: FEATURE - The ViewModel is updated to support full CRUD operations.
// It now includes `getRuleById`, `deleteRule`, and a comprehensive `saveRule`
// function that handles both creating new rules and updating existing ones.
// =================================================================================
package io.pm.finlight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.pm.finlight.data.db.AppDatabase
import io.pm.finlight.utils.ReminderManager
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

    fun getRuleById(id: Int): Flow<RecurringTransaction?> = repository.getById(id)

    fun saveRule(
        ruleId: Int?, // Null for new rules
        description: String,
        amount: Double,
        transactionType: String,
        recurrenceInterval: String,
        startDate: Long,
        accountId: Int,
        categoryId: Int?,
        lastRunDate: Long? // Preserve last run date on edit
    ) = viewModelScope.launch {
        val rule = RecurringTransaction(
            id = ruleId ?: 0,
            description = description,
            amount = amount,
            transactionType = transactionType,
            recurrenceInterval = recurrenceInterval,
            startDate = startDate,
            accountId = accountId,
            categoryId = categoryId,
            lastRunDate = lastRunDate
        )

        if (ruleId != null) {
            repository.update(rule)
        } else {
            repository.insert(rule)
            // Only schedule the worker when a new rule is added for the first time
            ReminderManager.scheduleRecurringTransactionWorker(getApplication())
        }
    }

    fun deleteRule(rule: RecurringTransaction) = viewModelScope.launch {
        repository.delete(rule)
    }
}
