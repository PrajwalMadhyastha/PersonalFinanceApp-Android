package io.pm.finlight

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
class RuleCreationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RuleCreationUiState())
    val uiState = _uiState.asStateFlow()

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
}
