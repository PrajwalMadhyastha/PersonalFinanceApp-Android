package io.pm.finlight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the Profile screen.
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    // Expose the user's name as a StateFlow so the UI can observe it
    val userName: StateFlow<String> = settingsRepository.getUserName()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "User" // Provide an initial default value
        )
}
