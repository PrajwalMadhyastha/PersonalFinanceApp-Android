// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/RetrospectiveUpdateViewModel.kt
// REASON: NEW FILE - This ViewModel supports the new Retrospective Update
// screen. It takes details about a pending change (new description or category),
// finds all similar transactions using the repository, manages the user's
// selection of which transactions to update, and performs the final batch
// update operation.
// =================================================================================
package io.pm.finlight

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RetrospectiveUpdateViewModelFactory(
    private val application: Application,
    private val originalDescription: String,
    private val transactionId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RetrospectiveUpdateViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RetrospectiveUpdateViewModel(application, originalDescription, transactionId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class RetrospectiveUpdateUiState(
    val similarTransactions: List<Transaction> = emptyList(),
    val selectedIds: Set<Int> = emptySet(),
    val isLoading: Boolean = true
)

class RetrospectiveUpdateViewModel(
    application: Application,
    private val originalDescription: String,
    private val transactionId: Int
) : ViewModel() {

    private val transactionRepository: TransactionRepository = TransactionRepository(AppDatabase.getInstance(application).transactionDao())

    private val _uiState = MutableStateFlow(RetrospectiveUpdateUiState())
    val uiState = _uiState.asStateFlow()

    init {
        findSimilarTransactions()
    }

    private fun findSimilarTransactions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val similar = transactionRepository.findSimilarTransactions(originalDescription, transactionId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    similarTransactions = similar,
                    selectedIds = similar.map { t -> t.id }.toSet() // Pre-select all
                )
            }
        }
    }

    fun toggleSelection(id: Int) {
        _uiState.update { currentState ->
            val newSelectedIds = currentState.selectedIds.toMutableSet()
            if (id in newSelectedIds) {
                newSelectedIds.remove(id)
            } else {
                newSelectedIds.add(id)
            }
            currentState.copy(selectedIds = newSelectedIds)
        }
    }

    fun toggleSelectAll() {
        _uiState.update { currentState ->
            if (currentState.selectedIds.size == currentState.similarTransactions.size) {
                // If all are selected, deselect all
                currentState.copy(selectedIds = emptySet())
            } else {
                // Otherwise, select all
                currentState.copy(selectedIds = currentState.similarTransactions.map { it.id }.toSet())
            }
        }
    }

    fun performBatchUpdate(newDescription: String?, newCategoryId: Int?) {
        viewModelScope.launch {
            val idsToUpdate = _uiState.value.selectedIds.toList()
            if (idsToUpdate.isEmpty()) return@launch

            if (newDescription != null) {
                transactionRepository.updateDescriptionForIds(idsToUpdate, newDescription)
            }
            if (newCategoryId != null) {
                transactionRepository.updateCategoryForIds(idsToUpdate, newCategoryId)
            }
        }
    }
}
