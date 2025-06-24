package com.example.personalfinanceapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val hasSearched: Boolean = false
)

class SearchViewModel(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<TransactionDetails>>(emptyList())
    val searchResults: StateFlow<List<TransactionDetails>> = _searchResults.asStateFlow()

    init {
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

    // CORRECTED: The 'type' parameter is now nullable to handle when the user clears the filter.
    fun onTypeChange(type: String?) {
        // If the type is null (cleared), default back to "All".
        _uiState.update { it.copy(transactionType = type ?: "All") }
    }

    fun onDateChange(start: Long? = _uiState.value.startDate, end: Long? = _uiState.value.endDate) {
        _uiState.update { it.copy(startDate = start, endDate = end) }
    }

    fun clearFilters() {
        _uiState.value = SearchUiState(
            accounts = _uiState.value.accounts,
            categories = _uiState.value.categories
        )
        _searchResults.value = emptyList()
    }

    fun executeSearch() {
        viewModelScope.launch {
            val state = _uiState.value
            _searchResults.value = transactionDao.searchTransactions(
                keyword = state.keyword,
                accountId = state.selectedAccount?.id,
                categoryId = state.selectedCategory?.id,
                transactionType = if (state.transactionType.equals("All", ignoreCase = true)) null else state.transactionType.lowercase(),
                startDate = state.startDate,
                endDate = state.endDate
            )
            _uiState.update { it.copy(hasSearched = true) }
        }
    }
}
