// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ManageParseRulesViewModel.kt
// REASON: NEW FILE - This ViewModel provides the logic for the new rule management
// screen. It fetches all custom SMS parsing rules from the database and exposes
// them as a StateFlow for the UI. It also includes a function to handle the
// deletion of a specific rule.
// =================================================================================
package io.pm.finlight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ManageParseRulesViewModel(application: Application) : AndroidViewModel(application) {

    private val customSmsRuleDao = AppDatabase.getInstance(application).customSmsRuleDao()

    /**
     * A flow of all custom SMS parsing rules, collected as StateFlow for the UI.
     */
    val allRules: StateFlow<List<CustomSmsRule>> = customSmsRuleDao.getAllRules()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Deletes a given custom rule from the database.
     *
     * @param rule The rule to be deleted.
     */
    fun deleteRule(rule: CustomSmsRule) {
        viewModelScope.launch {
            customSmsRuleDao.delete(rule)
        }
    }
}
