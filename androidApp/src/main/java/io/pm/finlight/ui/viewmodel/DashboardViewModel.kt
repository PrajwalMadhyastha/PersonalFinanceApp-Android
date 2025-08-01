package io.pm.finlight.ui.viewmodel

// NOTE: This file will have many errors until all repositories and DAOs are migrated.
// This is a placeholder for the final version.
import androidx.lifecycle.ViewModel
import io.pm.finlight.data.repository.AccountRepository
import io.pm.finlight.data.repository.SettingsRepository
import io.pm.finlight.data.repository.TransactionRepository
import io.pm.finlight.shared.db.AppDatabase

class DashboardViewModel(
    private val db: AppDatabase,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    // This ViewModel will be fully implemented after repositories are refactored.
}