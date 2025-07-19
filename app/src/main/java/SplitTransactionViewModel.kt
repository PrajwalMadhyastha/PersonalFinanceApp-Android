// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/SplitTransactionViewModel.kt
// REASON: NEW FILE - This dedicated ViewModel manages the state and business
// logic for the new transaction splitting screen. It holds the list of split
// items, tracks the remaining amount to be allocated, and enforces the "Golden
// Rule" where the sum of splits must equal the parent total.
// =================================================================================
package io.pm.finlight

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SplitItem(
    val id: Int, // Can be a temporary ID for new items, or the real ID for existing ones
    val amount: String,
    val category: Category?,
    val notes: String?
)

data class SplitTransactionUiState(
    val parentTransaction: Transaction? = null,
    val splitItems: List<SplitItem> = emptyList(),
    val remainingAmount: Double = 0.0,
    val isSaving: Boolean = false,
    val error: String? = null
)

class SplitTransactionViewModelFactory(
    private val application: Application,
    private val transactionId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SplitTransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SplitTransactionViewModel(application, transactionId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class SplitTransactionViewModel(
    application: Application,
    private val transactionId: Int
) : ViewModel() {

    private val db = AppDatabase.getInstance(application)
    private val transactionRepository = TransactionRepository(db.transactionDao())
    val categoryRepository = CategoryRepository(db.categoryDao())

    private val _uiState = MutableStateFlow(SplitTransactionUiState())
    val uiState = _uiState.asStateFlow()

    private var nextTempId = -1 // For unique keys for new items

    init {
        viewModelScope.launch {
            // For splitting, we only care about the parent transaction, not its relations.
            val parentTxn = transactionRepository.getTransactionById(transactionId).firstOrNull()
            if (parentTxn != null) {
                // Initialize with a single split item for the full amount, uncategorized.
                val initialSplit = SplitItem(
                    id = nextTempId--,
                    amount = parentTxn.amount.toString(),
                    category = null,
                    notes = parentTxn.notes
                )
                _uiState.value = SplitTransactionUiState(
                    parentTransaction = parentTxn,
                    splitItems = listOf(initialSplit),
                    remainingAmount = 0.0
                )
            } else {
                _uiState.value = SplitTransactionUiState(error = "Transaction not found.")
            }
        }
    }

    fun addSplitItem() {
        _uiState.update { currentState ->
            val newSplit = SplitItem(id = nextTempId--, amount = "0.0", category = null, notes = null)
            currentState.copy(splitItems = currentState.splitItems + newSplit)
        }
        recalculateRemainingAmount()
    }

    fun removeSplitItem(itemToRemove: SplitItem) {
        _uiState.update { currentState ->
            currentState.copy(splitItems = currentState.splitItems.filter { it.id != itemToRemove.id })
        }
        recalculateRemainingAmount()
    }

    fun updateSplitAmount(itemToUpdate: SplitItem, newAmount: String) {
        _uiState.update { currentState ->
            val updatedList = currentState.splitItems.map {
                if (it.id == itemToUpdate.id) it.copy(amount = newAmount) else it
            }
            currentState.copy(splitItems = updatedList)
        }
        recalculateRemainingAmount()
    }

    fun updateSplitCategory(itemToUpdate: SplitItem, newCategory: Category?) {
        _uiState.update { currentState ->
            val updatedList = currentState.splitItems.map {
                if (it.id == itemToUpdate.id) it.copy(category = newCategory) else it
            }
            currentState.copy(splitItems = updatedList)
        }
    }

    private fun recalculateRemainingAmount() {
        _uiState.update { currentState ->
            val parentAmount = currentState.parentTransaction?.amount ?: 0.0
            val totalSplitAmount = currentState.splitItems.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
            currentState.copy(remainingAmount = parentAmount - totalSplitAmount)
        }
    }
}
