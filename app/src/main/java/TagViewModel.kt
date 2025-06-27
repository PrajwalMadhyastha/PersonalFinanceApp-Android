// FILE: app/src/main/java/io/pm/finlight/TagViewModel.kt

package io.pm.finlight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TagViewModel(application: Application) : AndroidViewModel(application) {
    private val tagRepository: TagRepository

    // --- NEW: Channel for sending one-time events (like Toasts/Snackbars) to the UI ---
    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    val allTags: StateFlow<List<Tag>>

    init {
        val database = AppDatabase.getInstance(application)
        val tagDao = database.tagDao()
        val transactionDao = database.transactionDao()
        // --- UPDATED: Pass both DAOs to the repository ---
        tagRepository = TagRepository(tagDao, transactionDao)

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

    // --- NEW: Function to handle updating a tag ---
    fun updateTag(tag: Tag) {
        if (tag.name.isNotBlank()) {
            viewModelScope.launch {
                tagRepository.update(tag)
            }
        }
    }

    // --- NEW: Function to handle deleting a tag with validation ---
    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            // Check if the tag is in use before deleting.
            if (tagRepository.isTagInUse(tag.id)) {
                // If it's in use, send an error message to the UI.
                _uiEvent.send("Cannot delete '${tag.name}'. It is attached to one or more transactions.")
            } else {
                // If it's not in use, delete it.
                tagRepository.delete(tag)
                _uiEvent.send("Tag '${tag.name}' deleted.")
            }
        }
    }
}
