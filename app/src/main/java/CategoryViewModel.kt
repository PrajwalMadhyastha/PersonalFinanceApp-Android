package com.example.personalfinanceapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * ViewModel to handle the business logic for Categories.
 */
class CategoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CategoryRepository
    val allCategories: Flow<List<Category>>

    init {
        val categoryDao = AppDatabase.getInstance(application).categoryDao()
        repository = CategoryRepository(categoryDao)
        allCategories = repository.allCategories
    }

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun addCategory(name: String) = viewModelScope.launch {
        repository.insert(Category(name = name))
    }

    /**
     * Launching a new coroutine to update the data in a non-blocking way
     */
    fun updateCategory(category: Category) = viewModelScope.launch {
        repository.update(category)
    }

    /**
     * Launching a new coroutine to delete the data in a non-blocking way
     */
    fun deleteCategory(category: Category) = viewModelScope.launch {
        repository.delete(category)
    }
}
