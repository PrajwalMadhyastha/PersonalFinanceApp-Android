// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/TagDao.kt
// REASON: REFACTOR - Added a new `findByName` function that performs a
// case-insensitive search. This is essential for the ViewModel to check if a
// tag already exists before attempting to insert a new one, allowing for
// proper user feedback on duplicates.
// =================================================================================
package io.pm.finlight

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: Tag): Long

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<Tag>>

    // --- NEW: Function to find a tag by name, case-insensitively ---
    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): Tag?

    @Update
    suspend fun update(tag: Tag)

    @Delete
    suspend fun delete(tag: Tag)
}