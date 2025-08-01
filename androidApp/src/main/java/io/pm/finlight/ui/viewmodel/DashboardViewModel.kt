package io.pm.finlight.ui.viewmodel

import androidx.lifecycle.ViewModel
import io.pm.finlight.data.repository.AccountRepository
import io.pm.finlight.data.repository.SettingsRepository
import io.pm.finlight.data.repository.TransactionRepository
import io.pm.finlight.shared.db.AppDatabase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow

// A simplified version for now to get the build to pass.
// We will restore full functionality step-by-step.
class DashboardViewModel(
    private val db: AppDatabase,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val userName: StateFlow<String> = MutableStateFlow("User")
    val profilePictureUri: StateFlow<String?> = MutableStateFlow(null)
}