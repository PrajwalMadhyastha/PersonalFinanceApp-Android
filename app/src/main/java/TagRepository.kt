package io.pm.finlight

import kotlinx.coroutines.flow.Flow

/**
 * Repository that abstracts access to the tag data source.
 * Now includes update and delete logic.
 */
class TagRepository(
    private val tagDao: TagDao,
    private val transactionDao: TransactionDao
) {

    val allTags: Flow<List<Tag>> = tagDao.getAllTags()

    // --- FIX: Modified to return the new row's ID from the DAO ---
    suspend fun insert(tag: Tag): Long {
        return tagDao.insert(tag)
    }

    suspend fun update(tag: Tag) {
        tagDao.update(tag)
    }

    suspend fun delete(tag: Tag) {
        tagDao.delete(tag)
    }

    suspend fun isTagInUse(tagId: Int): Boolean {
        return transactionDao.countTransactionsForTag(tagId) > 0
    }
}
