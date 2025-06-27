package io.pm.finlight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TagViewModel(application: Application) : AndroidViewModel(application) {
    private val tagRepository: TagRepository

    val allTags: StateFlow<List<Tag>>

    init {
        val tagDao = AppDatabase.getInstance(application).tagDao()
        tagRepository = TagRepository(tagDao)
        allTags = tagRepository.allTags.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addTag(tagName: String) {
        if (tagName.isNotBlank()) {
            viewModelScope.launch {
                tagRepository.insert(Tag(name = tagName))
            }
        }
    }
}