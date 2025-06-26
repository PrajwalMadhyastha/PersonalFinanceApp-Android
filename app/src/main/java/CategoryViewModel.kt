package io.pm.finlight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * ViewModel to handle the business logic for Categories.
 */
class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val categoryRepository: CategoryRepository

    // --- NEW: Add dependency on TransactionRepository for validation ---
    private val transactionRepository: TransactionRepository

    val allCategories: Flow<List<Category>>

    // --- NEW: Channel for sending one-time events (like Toasts/Snackbars) to the UI ---
    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        val db = AppDatabase.getInstance(application)
        categoryRepository = CategoryRepository(db.categoryDao())
        // Initialize the new repository
        transactionRepository = TransactionRepository(db.transactionDao())
        allCategories = categoryRepository.allCategories
    }

    // A helper to get a single category by ID, not as a flow.
    suspend fun getCategoryById(id: Int): Category? {
        // We take the first emission from the flow, which is the current state.
        return allCategories.firstOrNull()?.find { it.id == id }
    }

    fun addCategory(name: String) =
        viewModelScope.launch {
            categoryRepository.insert(Category(name = name))
        }

    fun updateCategory(category: Category) =
        viewModelScope.launch {
            categoryRepository.update(category)
        }

    // --- UPDATED: Delete function now includes validation logic ---
    fun deleteCategory(category: Category) =
        viewModelScope.launch {
            // Check if the category is in use before deleting.
            val transactionCount = transactionRepository.countTransactionsForCategory(category.id)
            if (transactionCount == 0) {
                categoryRepository.delete(category)
                _uiEvent.send("Category '${category.name}' deleted.")
            } else {
                // Send an event to the UI explaining why deletion failed.
                _uiEvent.send("Cannot delete '${category.name}'. It's used by $transactionCount transaction(s).")
            }
        }
}
