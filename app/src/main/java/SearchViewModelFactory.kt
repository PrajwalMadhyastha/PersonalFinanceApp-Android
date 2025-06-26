package com.example.personalfinanceapp

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SearchViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            val database = AppDatabase.getInstance(application)
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(
                transactionDao = database.transactionDao(),
                accountDao = database.accountDao(),
                categoryDao = database.categoryDao(),
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
