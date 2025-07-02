// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/RuleCreationViewModel.kt
// REASON: UX IMPROVEMENT - The `saveRule` function is updated to populate the new
// `merchantNameExample` and `amountExample` fields in the `CustomSmsRule` entity.
// It now saves the literal text the user selected, which will be displayed on
// the rule management screen for better readability.
// =================================================================================
package io.pm.finlight

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.regex.Pattern

/**
 * Data class to hold the state of a user's selection for a custom rule.
 *
 * @param selectedText The actual text the user has highlighted.
 * @param startIndex The starting index of the selection within the original SMS text.
 * @param endIndex The ending index of the selection within the original SMS text.
 */
data class RuleSelection(
    val selectedText: String = "",
    val startIndex: Int = -1,
    val endIndex: Int = -1
)

/**
 * UI state for the RuleCreationScreen.
 */
data class RuleCreationUiState(
    val triggerSelection: RuleSelection = RuleSelection(),
    val merchantSelection: RuleSelection = RuleSelection(),
    val amountSelection: RuleSelection = RuleSelection()
)

/**
 * ViewModel for the RuleCreationScreen.
 */
class RuleCreationViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RuleCreationUiState())
    val uiState = _uiState.asStateFlow()

    private val customSmsRuleDao = AppDatabase.getInstance(application).customSmsRuleDao()

    fun onMarkAsTrigger(selection: RuleSelection) {
        _uiState.update { it.copy(triggerSelection = selection) }
    }

    fun onMarkAsMerchant(selection: RuleSelection) {
        _uiState.update { it.copy(merchantSelection = selection) }
    }

    fun onMarkAsAmount(selection: RuleSelection) {
        _uiState.update { it.copy(amountSelection = selection) }
    }

    /**
     * Generates regex patterns and saves a single consolidated rule to the database.
     */
    fun saveRule(fullSmsText: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState.triggerSelection.selectedText.isBlank()) {
                Log.e("RuleCreation", "Cannot save rule without a trigger phrase.")
                onComplete()
                return@launch
            }

            val merchantRegex = if (currentState.merchantSelection.selectedText.isNotBlank()) {
                generateRegex(fullSmsText, currentState.merchantSelection)
            } else null

            val amountRegex = if (currentState.amountSelection.selectedText.isNotBlank()) {
                generateRegex(fullSmsText, currentState.amountSelection)
            } else null

            val newRule = CustomSmsRule(
                triggerPhrase = currentState.triggerSelection.selectedText,
                merchantRegex = merchantRegex,
                amountRegex = amountRegex,
                // --- NEW: Save the example text for user-friendly display ---
                merchantNameExample = currentState.merchantSelection.selectedText.takeIf { it.isNotBlank() },
                amountExample = currentState.amountSelection.selectedText.takeIf { it.isNotBlank() },
                priority = 10 // Default priority
            )

            Log.d("RuleCreation", "Saving new trigger-based rule: $newRule")
            customSmsRuleDao.insert(newRule)
            onComplete()
        }
    }

    /**
     * Generates a robust regular expression based on the words surrounding a user's selection.
     */
    private fun generateRegex(fullText: String, selection: RuleSelection): String? {
        if (selection.startIndex == -1) return null

        val textBefore = fullText.substring(0, selection.startIndex)
        val textAfter = fullText.substring(selection.endIndex)

        val prefixWords = textBefore.trim().split(Regex("\\s+")).takeLast(2)
        val prefix = prefixWords.joinToString(separator = " ")

        val suffixWords = textAfter.trim().split(Regex("\\s+")).take(2)
        val suffix = suffixWords.joinToString(separator = " ")

        val escapedPrefix = if (prefix.isNotBlank()) Pattern.quote(prefix) else ""
        val escapedSuffix = if (suffix.isNotBlank()) Pattern.quote(suffix) else ""

        return when {
            escapedPrefix.isNotBlank() && escapedSuffix.isNotBlank() -> "$escapedPrefix\\s+(.*?)\\s+$escapedSuffix"
            escapedPrefix.isNotBlank() -> "$escapedPrefix\\s+(.*)"
            escapedSuffix.isNotBlank() -> "(.*?)\\s+$escapedSuffix"
            else -> Pattern.quote(selection.selectedText)
        }
    }
}
