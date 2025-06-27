package io.pm.finlight

import kotlinx.coroutines.flow.Flow
// REMOVED: import javax.inject.Inject

/**
 * Repository that abstracts access to the tag data source.
 */
// REMOVED: @Inject annotation from constructor
class TagRepository(private val tagDao: TagDao) {

    val allTags: Flow<List<Tag>> = tagDao.getAllTags()

    suspend fun insert(tag: Tag) {
        tagDao.insert(tag)
    }
}