// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ManageIgnoreRulesViewModel.kt
// REASON: NEW FILE - This ViewModel provides the business logic for the new
// "Manage Ignore Rules" screen. It fetches all ignore rules from the database,
// exposes them to the UI, and provides methods for adding and deleting rules.
// =================================================================================
package io.pm.finlight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ManageIgnoreRulesViewModel(application: Application) : AndroidViewModel(application) {

    private val ignoreRuleDao = AppDatabase.getInstance(application).ignoreRuleDao()

    /**
     * A flow of all ignore rules, collected as a StateFlow for the UI.
     */
    val allRules: StateFlow<List<IgnoreRule>> = ignoreRuleDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Adds a new ignore phrase to the database.
     * @param phrase The phrase to be added.
     */
    fun addIgnoreRule(phrase: String) {
        if (phrase.isNotBlank()) {
            viewModelScope.launch {
                ignoreRuleDao.insert(IgnoreRule(phrase = phrase.trim()))
            }
        }
    }

    /**
     * Deletes a given ignore rule from the database.
     * @param rule The rule to be deleted.
     */
    fun deleteIgnoreRule(rule: IgnoreRule) {
        viewModelScope.launch {
            ignoreRuleDao.delete(rule)
        }
    }
}
