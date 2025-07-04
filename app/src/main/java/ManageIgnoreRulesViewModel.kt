// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ManageIgnoreRulesViewModel.kt
// REASON: FEATURE - The ViewModel is updated to handle the new distinction
// between default and user-added rules. It now includes a method to update a
// rule's `isEnabled` status, allowing users to toggle the default rules.
// BUG FIX - Renamed `updateRule` to `updateIgnoreRule` to provide a more
// specific function name and resolve a persistent compilation error.
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
                // User-added rules are not default rules
                ignoreRuleDao.insert(IgnoreRule(phrase = phrase.trim(), isDefault = false))
            }
        }
    }

    /**
     * Updates an existing ignore rule, typically to toggle its enabled status.
     * @param rule The rule to be updated.
     */
    fun updateIgnoreRule(rule: IgnoreRule) {
        viewModelScope.launch {
            ignoreRuleDao.update(rule)
        }
    }

    /**
     * Deletes a given ignore rule from the database. This should only be called
     * for non-default rules.
     * @param rule The rule to be deleted.
     */
    fun deleteIgnoreRule(rule: IgnoreRule) {
        // Safety check to prevent deleting default rules
        if (!rule.isDefault) {
            viewModelScope.launch {
                ignoreRuleDao.delete(rule)
            }
        }
    }
}
