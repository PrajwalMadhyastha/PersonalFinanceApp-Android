// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/SearchViewModel.kt
// REASON: Implemented a dynamic, debounced search mechanism. The ViewModel now
// automatically triggers a search query 300ms after any filter or keyword
// changes, removing the need for a manual "Apply" button.
// =================================================================================
package io.pm.finlight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SearchUiState(
    val keyword: String = "",
    val selectedAccount: Account? = null,
    val selectedCategory: Category? = null,
    val transactionType: String = "All", // "All", "Income", "Expense"
    val startDate: Long? = null,
    val endDate: Long? = null,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val hasSearched: Boolean = false,
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<TransactionDetails>>(emptyList())
    val searchResults: StateFlow<List<TransactionDetails>> = _searchResults.asStateFlow()

    init {
        // Load initial filter options (accounts and categories)
        viewModelScope.launch {
            accountDao.getAllAccounts().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
        viewModelScope.launch {
            categoryDao.getAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }

        // --- NEW: Reactive search logic ---
        // This flow automatically executes a search whenever the UI state changes,
        // with a 300ms debounce to prevent excessive queries while typing.
        viewModelScope.launch {
            uiState
                .debounce(300L)
                .collectLatest { state -> // Use collectLatest to cancel previous searches if a new state arrives
                    val filtersAreActive = state.selectedAccount != null ||
                            state.selectedCategory != null ||
                            state.transactionType != "All" ||
                            state.startDate != null ||
                            state.endDate != null

                    if (state.keyword.isNotBlank() || filtersAreActive) {
                        _uiState.update { it.copy(hasSearched = true) }
                        val results = transactionDao.searchTransactions(
                            keyword = state.keyword,
                            accountId = state.selectedAccount?.id,
                            categoryId = state.selectedCategory?.id,
                            transactionType = if (state.transactionType.equals("All", ignoreCase = true)) null else state.transactionType.lowercase(),
                            startDate = state.startDate,
                            endDate = state.endDate
                        )
                        _searchResults.value = results
                    } else {
                        // Clear results and search status if no filters/keyword are active
                        _searchResults.value = emptyList()
                        _uiState.update { it.copy(hasSearched = false) }
                    }
                }
        }
    }

    fun onKeywordChange(newKeyword: String) {
        _uiState.update { it.copy(keyword = newKeyword) }
    }

    fun onAccountChange(account: Account?) {
        _uiState.update { it.copy(selectedAccount = account) }
    }

    fun onCategoryChange(category: Category?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onTypeChange(type: String?) {
        _uiState.update { it.copy(transactionType = type ?: "All") }
    }

    fun onDateChange(
        start: Long? = _uiState.value.startDate,
        end: Long? = _uiState.value.endDate,
    ) {
        _uiState.update { it.copy(startDate = start, endDate = end) }
    }

    fun clearFilters() {
        _uiState.value =
            SearchUiState(
                accounts = _uiState.value.accounts,
                categories = _uiState.value.categories,
            )
        // Results will clear automatically due to the reactive flow
    }

    // The manual executeSearch function is no longer needed.
}
