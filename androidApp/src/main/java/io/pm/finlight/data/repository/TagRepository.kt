package io.pm.finlight.data.repository

import io.pm.finlight.data.db.entity.Tag
import io.pm.finlight.shared.db.TagQueries
import io.pm.finlight.shared.db.Transaction_tag_cross_refQueries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne

/**
 * Repository that abstracts access to the tag data source.
 * NOTE: This class has been refactored to use SQLDelight queries instead of Room DAOs.
 */
class TagRepository(
    private val tagQueries: TagQueries,
    private val transactionTagCrossRefQueries: Transaction_tag_cross_refQueries,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    val allTags: Flow<List<Tag>> = tagQueries.selectAll(::mapTag).asFlow().mapToList(dispatcher)

    suspend fun insert(tag: Tag): Long = withContext(dispatcher) {
        tagQueries.insert(tag.name)
        return@withContext tagQueries.lastInsertRowId().executeAsOne()
    }

    suspend fun update(tag: Tag) = withContext(dispatcher) {
        tagQueries.update(tag.id.toLong(), tag.name)
    }

    suspend fun delete(tag: Tag) = withContext(dispatcher) {
        tagQueries.deleteById(tag.id.toLong())
    }

    suspend fun isTagInUse(tagId: Int): Boolean = withContext(dispatcher) {
        return@withContext transactionTagCrossRefQueries.countTransactionsForTag(tagId.toLong())
            .executeAsOne() > 0
    }

    private fun mapTag(id: Long, name: String): Tag {
        return Tag(id.toInt(), name)
    }
}
