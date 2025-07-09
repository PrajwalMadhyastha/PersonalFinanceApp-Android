// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/RecurringTransactionViewModel.kt
// REASON: FEATURE - The ViewModel now calls the new
// `ReminderManager.scheduleRecurringTransactionWorker` function after a new rule
// is added. This ensures that the automation process is kicked off as soon as the
// user creates their first recurring transaction rule.
// =================================================================================
package io.pm.finlight

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
        categoryId: Int?,
    ) = viewModelScope.launch {
        val newRule =
            RecurringTransaction(
                description = description,
                amount = amount,
                transactionType = transactionType,
                recurrenceInterval = recurrenceInterval,
                startDate = startDate,
                accountId = accountId,
                categoryId = categoryId,
            )
        repository.insert(newRule)
        // --- NEW: Schedule the worker after adding a rule ---
        ReminderManager.scheduleRecurringTransactionWorker(getApplication())
    }
}