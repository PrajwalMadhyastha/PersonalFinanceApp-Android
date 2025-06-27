// FILE: app/src/main/java/io/pm/finlight/TagRepository.kt

package io.pm.finlight

import kotlinx.coroutines.flow.Flow

/**
 * Repository that abstracts access to the tag data source.
 * Now includes update and delete logic.
 */
class TagRepository(
    private val tagDao: TagDao,
    // --- NEW: Add dependency for validation check ---
    private val transactionDao: TransactionDao
) {

    val allTags: Flow<List<Tag>> = tagDao.getAllTags()

    suspend fun insert(tag: Tag) {
        tagDao.insert(tag)
    }

    // --- NEW: To update a tag ---
    suspend fun update(tag: Tag) {
        tagDao.update(tag)
    }

    // --- NEW: To delete a tag ---
    suspend fun delete(tag: Tag) {
        tagDao.delete(tag)
    }

    // --- NEW: To check if a tag is associated with any transactions ---
    suspend fun isTagInUse(tagId: Int): Boolean {
        return transactionDao.countTransactionsForTag(tagId) > 0
    }
}
