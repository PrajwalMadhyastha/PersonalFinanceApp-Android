// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/SearchViewModel.kt
// REASON: FEATURE - The ViewModel now accepts an `initialDateMillis` parameter.
// In its `init` block, it checks for this date and, if present, automatically
// sets the start and end date filters to span that single day. This allows the
// UI to display pre-filtered search results when navigated to from the calendar.
// =================================================================================
package io.pm.finlight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

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
    private val initialCategoryId: Int?,
    private val initialDateMillis: Long? // --- NEW: Accept initial date
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<TransactionDetails>>(emptyList())
    val searchResults: StateFlow<List<TransactionDetails>> = _searchResults.asStateFlow()

    init {
        // Load initial filter options
        viewModelScope.launch {
            accountDao.getAllAccounts().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
        viewModelScope.launch {
            categoryDao.getAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
                if (initialCategoryId != null) {
                    val initialCategory = categories.find { it.id == initialCategoryId }
                    if (initialCategory != null) {
                        _uiState.update { it.copy(selectedCategory = initialCategory) }
                    }
                }
            }
        }

        // --- NEW: Pre-select date range if an initial date was passed ---
        if (initialDateMillis != null && initialDateMillis != -1L) {
            val cal = Calendar.getInstance().apply { timeInMillis = initialDateMillis }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val start = cal.timeInMillis

            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            val end = cal.timeInMillis

            onDateChange(start, end)
        }


        // Reactive search logic
        viewModelScope.launch {
            uiState
                .debounce(300L)
                .collectLatest { state ->
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
    }
}
