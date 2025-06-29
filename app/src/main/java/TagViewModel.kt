// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/TagViewModel.kt
// REASON: Corrected the addTag function. This ViewModel is for the "Manage Tags"
// screen, so it only needs to insert the tag. The UI will be updated
// automatically by the Flow from the database.
// =================================================================================
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
    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    val allTags: StateFlow<List<Tag>>

    init {
        val database = AppDatabase.getInstance(application)
        val tagDao = database.tagDao()
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
                tagRepository.insert(Tag(name = tagName))
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
