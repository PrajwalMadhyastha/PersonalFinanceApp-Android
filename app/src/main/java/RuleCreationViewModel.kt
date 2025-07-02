// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/RuleCreationViewModel.kt
// REASON: BUG FIX - The `generateRegex` function was too strict with its
// whitespace requirements, causing rules to fail if the selected text was
// adjacent to punctuation. The regex construction is now more flexible, using
// `\s*` (zero or more whitespace) instead of `\s+` (one or more), making the
// generated rules much more robust and reliable.
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
    val amountSelection: RuleSelection = RuleSelection(),
    val accountSelection: RuleSelection = RuleSelection(),
    val transactionType: String? = null
)

/**
 * ViewModel for the RuleCreationScreen.
 */
class RuleCreationViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RuleCreationUiState())
    val uiState = _uiState.asStateFlow()

    private val customSmsRuleDao = AppDatabase.getInstance(application).customSmsRuleDao()

    fun initializeState(potentialTxn: PotentialTransaction) {
        val amountStr = String.format("%.2f", potentialTxn.amount)
        val amountIndex = potentialTxn.originalMessage.indexOf(amountStr)

        val amountSelection = if (amountIndex != -1) {
            RuleSelection(
                selectedText = amountStr,
                startIndex = amountIndex,
                endIndex = amountIndex + amountStr.length
            )
        } else {
            RuleSelection()
        }

        _uiState.value = RuleCreationUiState(
            amountSelection = amountSelection,
            transactionType = potentialTxn.transactionType
        )
    }

    fun onMarkAsTrigger(selection: RuleSelection) {
        _uiState.update { it.copy(triggerSelection = selection) }
    }

    fun onMarkAsMerchant(selection: RuleSelection) {
        _uiState.update { it.copy(merchantSelection = selection) }
    }

    fun onMarkAsAmount(selection: RuleSelection) {
        _uiState.update { it.copy(amountSelection = selection) }
    }

    fun onMarkAsAccount(selection: RuleSelection) {
        _uiState.update { it.copy(accountSelection = selection) }
    }

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

            val accountRegex = if (currentState.accountSelection.selectedText.isNotBlank()) {
                generateRegex(fullSmsText, currentState.accountSelection)
            } else null

            val newRule = CustomSmsRule(
                triggerPhrase = currentState.triggerSelection.selectedText,
                merchantRegex = merchantRegex,
                amountRegex = amountRegex,
                accountRegex = accountRegex,
                merchantNameExample = currentState.merchantSelection.selectedText.takeIf { it.isNotBlank() },
                amountExample = currentState.amountSelection.selectedText.takeIf { it.isNotBlank() },
                accountNameExample = currentState.accountSelection.selectedText.takeIf { it.isNotBlank() },
                priority = 10
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

        // --- FIX: Use `\s*` for flexible whitespace matching instead of `\s+` ---
        // This handles cases where the selection is immediately next to punctuation.
        return when {
            escapedPrefix.isNotBlank() && escapedSuffix.isNotBlank() -> "$escapedPrefix\\s*(.*?)\\s*$escapedSuffix"
            escapedPrefix.isNotBlank() -> "$escapedPrefix\\s*(.*)"
            escapedSuffix.isNotBlank() -> "(.*?)\\s*$escapedSuffix"
            else -> Pattern.quote(selection.selectedText)
        }
    }
}
