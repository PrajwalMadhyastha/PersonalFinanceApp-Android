package io.pm.finlight

import android.app.Application
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
    val merchantSelection: RuleSelection = RuleSelection(),
    val amountSelection: RuleSelection = RuleSelection(),
    val transactionType: String? = null
)

/**
 * ViewModel for the RuleCreationScreen.
 *
 * This ViewModel is responsible for holding the state of the user's selections
 * as they define a new custom parsing rule from an SMS message.
 */
class RuleCreationViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RuleCreationUiState())
    val uiState = _uiState.asStateFlow()

    private val customSmsRuleDao = AppDatabase.getInstance(application).customSmsRuleDao()

    /**
     * Updates the state to reflect that the user has marked the provided selection as the merchant.
     *
     * @param selection The user's text selection.
     */
    fun onMarkAsMerchant(selection: RuleSelection) {
        _uiState.update { it.copy(merchantSelection = selection) }
    }

    /**
     * Updates the state to reflect that the user has marked the provided selection as the amount.
     *
     * @param selection The user's text selection.
     */
    fun onMarkAsAmount(selection: RuleSelection) {
        _uiState.update { it.copy(amountSelection = selection) }
    }

    /**
     * Generates regex patterns from the user's selections and saves them to the database.
     *
     * @param smsSender The sender of the SMS (e.g., "AM-HDFCBK").
     * @param fullSmsText The complete text of the SMS message.
     * @param onComplete A callback to be invoked when the save operation is finished.
     */
    fun saveRules(smsSender: String, fullSmsText: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val currentState = _uiState.value

            // Generate and save the merchant rule if a selection was made
            if (currentState.merchantSelection.selectedText.isNotBlank()) {
                val merchantRegex = generateRegex(fullSmsText, currentState.merchantSelection)
                if (merchantRegex != null) {
                    val rule = CustomSmsRule(
                        smsSender = smsSender,
                        ruleType = "MERCHANT",
                        regexPattern = merchantRegex,
                        priority = 10 // Default priority
                    )
                    customSmsRuleDao.insert(rule)
                }
            }

            // Generate and save the amount rule if a selection was made
            if (currentState.amountSelection.selectedText.isNotBlank()) {
                val amountRegex = generateRegex(fullSmsText, currentState.amountSelection)
                if (amountRegex != null) {
                    val rule = CustomSmsRule(
                        smsSender = smsSender,
                        ruleType = "AMOUNT",
                        regexPattern = amountRegex,
                        priority = 10 // Default priority
                    )
                    customSmsRuleDao.insert(rule)
                }
            }

            onComplete()
        }
    }

    /**
     * Generates a regular expression based on the text surrounding a user's selection.
     *
     * @param fullText The entire SMS message body.
     * @param selection The user's selection details (text, start index, end index).
     * @return A regex string, or null if generation is not possible.
     */
    private fun generateRegex(fullText: String, selection: RuleSelection): String? {
        if (selection.startIndex == -1) return null

        // Define how many characters to look for before and after the selection
        val contextLength = 10

        // Get the text immediately before the selection
        val prefixStart = (selection.startIndex - contextLength).coerceAtLeast(0)
        val prefix = fullText.substring(prefixStart, selection.startIndex)

        // Get the text immediately after the selection
        val suffixEnd = (selection.endIndex + contextLength).coerceAtMost(fullText.length)
        val suffix = fullText.substring(selection.endIndex, suffixEnd)

        // Escape any special regex characters in the prefix and suffix to treat them as literals
        val escapedPrefix = Pattern.quote(prefix)
        val escapedSuffix = Pattern.quote(suffix)

        // Construct the final regex with a non-greedy capture group for the selected part
        return "$escapedPrefix(.*?)$escapedSuffix"
    }
}
