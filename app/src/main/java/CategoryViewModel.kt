package io.pm.finlight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val categoryRepository: CategoryRepository
    private val transactionRepository: TransactionRepository
    val allCategories: Flow<List<Category>>
    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        val db = AppDatabase.getInstance(application)
        categoryRepository = CategoryRepository(db.categoryDao())
        transactionRepository = TransactionRepository(db.transactionDao())
        allCategories = categoryRepository.allCategories
    }

    suspend fun getCategoryById(id: Int): Category? {
        return allCategories.firstOrNull()?.find { it.id == id }
    }

    fun addCategory(name: String, iconKey: String, colorKey: String) =
        viewModelScope.launch {
            categoryRepository.insert(Category(name = name, iconKey = iconKey, colorKey = colorKey))
        }

    fun updateCategory(category: Category) =
        viewModelScope.launch {
            categoryRepository.update(category)
        }

    fun deleteCategory(category: Category) =
        viewModelScope.launch {
            val transactionCount = transactionRepository.countTransactionsForCategory(category.id)
            if (transactionCount == 0) {
                categoryRepository.delete(category)
                _uiEvent.send("Category '${category.name}' deleted.")
            } else {
                _uiEvent.send("Cannot delete '${category.name}'. It's used by $transactionCount transaction(s).")
            }
        }
}
