// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/TagViewModel.kt
// REASON: UX REFINEMENT - The `addTag` function now checks if a tag with the
// same name already exists (case-insensitively). If a duplicate is found, it
// sends a feedback message to the UI via the `uiEvent` channel, improving the
// user experience by preventing silent failures.
// =================================================================================
package io.pm.finlight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.pm.finlight.data.db.AppDatabase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TagViewModel(application: Application) : AndroidViewModel(application) {
    private val tagRepository: TagRepository
    private val tagDao: TagDao // Expose DAO for direct checks
    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    val allTags: StateFlow<List<Tag>>

    init {
        val database = AppDatabase.getInstance(application)
        tagDao = database.tagDao() // Initialize DAO
        val transactionDao = database.transactionDao()
        tagRepository = TagRepository(tagDao, transactionDao)

        allTags = tagRepository.allTags.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    /**
     * Called from the 'Manage Tags' screen. Inserts a new tag into the database.
     */
    fun addTag(tagName: String) {
        if (tagName.isNotBlank()) {
            viewModelScope.launch {
                // Check if a tag with this name already exists
                val existingTag = tagDao.findByName(tagName)
                if (existingTag != null) {
                    _uiEvent.send("A tag named '$tagName' already exists.")
                } else {
                    tagRepository.insert(Tag(name = tagName))
                    _uiEvent.send("Tag '$tagName' created.")
                }
            }
        }
    }

    fun updateTag(tag: Tag) {
        if (tag.name.isNotBlank()) {
            viewModelScope.launch {
                tagRepository.update(tag)
            }
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            if (tagRepository.isTagInUse(tag.id)) {
                _uiEvent.send("Cannot delete '${tag.name}'. It is attached to one or more transactions.")
            } else {
                tagRepository.delete(tag)
                _uiEvent.send("Tag '${tag.name}' deleted.")
            }
        }
    }
}