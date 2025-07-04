// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/IgnoreRuleDao.kt
// REASON: NEW FILE - This DAO provides the necessary methods for the application
// to interact with the new `ignore_rules` table, including fetching all rules,
// inserting a new one, and deleting an existing one.
// =================================================================================
package io.pm.finlight

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the IgnoreRule entity.
 */
@Dao
interface IgnoreRuleDao {

    /**
     * Retrieves all ignore rules from the database, ordered alphabetically.
     * @return A Flow emitting a list of all IgnoreRule objects.
     */
    @Query("SELECT * FROM ignore_rules ORDER BY phrase ASC")
    fun getAll(): Flow<List<IgnoreRule>>

    /**
     * Inserts a new ignore rule. If a rule with the same phrase already exists,
     * it will be ignored.
     * @param rule The IgnoreRule object to insert.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rule: IgnoreRule)

    /**
     * Deletes a specific ignore rule from the database.
     * @param rule The IgnoreRule object to delete.
     */
    @Delete
    suspend fun delete(rule: IgnoreRule)
}
