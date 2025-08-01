package io.pm.finlight.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.pm.finlight.data.db.DatabaseProvider
import io.pm.finlight.data.repository.AccountRepository
import io.pm.finlight.data.repository.SettingsRepository
import io.pm.finlight.data.repository.TransactionRepository

class DashboardViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            val db = DatabaseProvider.getInstance(application)
            val transactionRepository = TransactionRepository(db.transactionQueries, db.accountQueries, db.categoryQueries, db.tagQueries, db.transaction_tag_cross_refQueries)
            val accountRepository = AccountRepository(db.accountQueries)
            val settingsRepository = SettingsRepository(application)

            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(
                db = db,
                transactionRepository = transactionRepository,
                accountRepository = accountRepository,
                settingsRepository = settingsRepository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}