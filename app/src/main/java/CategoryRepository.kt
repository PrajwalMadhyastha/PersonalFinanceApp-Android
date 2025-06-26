package io.pm.finlight

import kotlinx.coroutines.flow.Flow

/**
 * Repository that abstracts access to the category data source.
 */
class CategoryRepository(private val categoryDao: CategoryDao) {
    /**
     * Retrieves all categories from the category table, ordered by name.
     */
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    /**
     * Inserts a category in a non-blocking way.
     */
    suspend fun insert(category: Category) {
        categoryDao.insert(category)
    }

    /**
     * Updates a category in a non-blocking way.
     */
    suspend fun update(category: Category) {
        categoryDao.update(category)
    }

    /**
     * Deletes a category in a non-blocking way.
     */
    suspend fun delete(category: Category) {
        categoryDao.delete(category)
    }
}
