package com.example.personalfinanceapp

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory for creating a DashboardViewModel with a constructor that takes dependencies.
 */
class DashboardViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            val db = AppDatabase.getInstance(application)
            val transactionRepository = TransactionRepository(db.transactionDao())
            val accountRepository = AccountRepository(db.accountDao())
            val settingsRepository = SettingsRepository(application)

            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(
                transactionRepository = transactionRepository,
                accountRepository = accountRepository,
                budgetDao = db.budgetDao(),
                settingsRepository = settingsRepository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
