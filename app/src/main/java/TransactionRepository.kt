// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/TransactionRepository.kt
// REASON: FEATURE - The repository now exposes the new DAO functions for the
// Retrospective Update feature. `findSimilarTransactions`,
// `updateCategoryForIds`, and `updateDescriptionForIds` are now available to
// be called by the ViewModel layer, providing a clean abstraction over the
// database operations.
// =================================================================================
package io.pm.finlight

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

class TransactionRepository(private val transactionDao: TransactionDao) {
    val allTransactions: Flow<List<TransactionDetails>> =
        transactionDao.getAllTransactions()
            .onEach { transactions ->
                Log.d(
                    "TransactionFlowDebug",
                    "Repository Flow Emitted. Count: ${transactions.size}. Newest: ${transactions.firstOrNull()?.transaction?.description}",
                )
            }

    fun getIncomeTransactionsForRange(startDate: Long, endDate: Long, keyword: String?, accountId: Int?, categoryId: Int?): Flow<List<TransactionDetails>> {
        return transactionDao.getIncomeTransactionsForRange(startDate, endDate, keyword, accountId, categoryId)
    }

    fun getIncomeByCategoryForMonth(startDate: Long, endDate: Long, keyword: String?, accountId: Int?, categoryId: Int?): Flow<List<CategorySpending>> {
        return transactionDao.getIncomeByCategoryForMonth(startDate, endDate, keyword, accountId, categoryId)
    }

    fun getSpendingByMerchantForMonth(startDate: Long, endDate: Long, keyword: String?, accountId: Int?, categoryId: Int?): Flow<List<MerchantSpendingSummary>> {
        return transactionDao.getSpendingByMerchantForMonth(startDate, endDate, keyword, accountId, categoryId)
    }

    suspend fun addImageToTransaction(transactionId: Int, imageUri: String) {
        val transactionImage = TransactionImage(transactionId = transactionId, imageUri = imageUri)
        transactionDao.insertImage(transactionImage)
    }

    suspend fun deleteImage(transactionImage: TransactionImage) {
        transactionDao.deleteImage(transactionImage)
    }

    fun getImagesForTransaction(transactionId: Int): Flow<List<TransactionImage>> {
        return transactionDao.getImagesForTransaction(transactionId)
    }

    suspend fun updateDescription(id: Int, description: String) = transactionDao.updateDescription(id, description)
    suspend fun updateAmount(id: Int, amount: Double) = transactionDao.updateAmount(id, amount)
    suspend fun updateNotes(id: Int, notes: String?) = transactionDao.updateNotes(id, notes)
    suspend fun updateCategoryId(id: Int, categoryId: Int?) = transactionDao.updateCategoryId(id, categoryId)
    suspend fun updateAccountId(id: Int, accountId: Int) = transactionDao.updateAccountId(id, accountId)
    suspend fun updateDate(id: Int, date: Long) = transactionDao.updateDate(id, date)
    suspend fun updateExclusionStatus(id: Int, isExcluded: Boolean) = transactionDao.updateExclusionStatus(id, isExcluded)

    fun getTransactionDetailsById(id: Int): Flow<TransactionDetails?> {
        return transactionDao.getTransactionDetailsById(id)
    }

    val recentTransactions: Flow<List<TransactionDetails>> = transactionDao.getRecentTransactionDetails()

    fun getAllSmsHashes(): Flow<List<String>> {
        return transactionDao.getAllSmsHashes()
    }

    fun getTransactionsForAccountDetails(accountId: Int): Flow<List<TransactionDetails>> {
        return transactionDao.getTransactionsForAccountDetails(accountId)
    }

    fun getTransactionDetailsForRange(
        startDate: Long,
        endDate: Long,
        keyword: String?,
        accountId: Int?,
        categoryId: Int?
    ): Flow<List<TransactionDetails>> {
        return transactionDao.getTransactionDetailsForRange(startDate, endDate, keyword, accountId, categoryId)
    }

    fun getAllTransactionsSimple(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactionsSimple()
    }

    fun getAllTransactionsForRange(
        startDate: Long,
        endDate: Long,
    ): Flow<List<Transaction>> {
        return transactionDao.getAllTransactionsForRange(startDate, endDate)
    }

    fun getTransactionById(id: Int): Flow<Transaction?> {
        return transactionDao.getTransactionById(id)
    }

    fun getTransactionsForAccount(accountId: Int): Flow<List<Transaction>> {
        return transactionDao.getTransactionsForAccount(accountId)
    }

    fun getSpendingForCategory(
        categoryName: String,
        startDate: Long,
        endDate: Long,
    ): Flow<Double?> {
        return transactionDao.getSpendingForCategory(categoryName, startDate, endDate)
    }

    fun getSpendingByCategoryForMonth(
        startDate: Long,
        endDate: Long,
        keyword: String?,
        accountId: Int?,
        categoryId: Int?
    ): Flow<List<CategorySpending>> {
        return transactionDao.getSpendingByCategoryForMonth(startDate, endDate, keyword, accountId, categoryId)
    }

    fun getMonthlyTrends(startDate: Long): Flow<List<MonthlyTrend>> {
        return transactionDao.getMonthlyTrends(startDate)
    }

    suspend fun countTransactionsForCategory(categoryId: Int): Int {
        return transactionDao.countTransactionsForCategory(categoryId)
    }

    fun getTagsForTransaction(transactionId: Int): Flow<List<Tag>> {
        return transactionDao.getTagsForTransaction(transactionId)
    }

    suspend fun updateTagsForTransaction(transactionId: Int, tags: Set<Tag>) {
        transactionDao.clearTagsForTransaction(transactionId)
        if (tags.isNotEmpty()) {
            val crossRefs = tags.map { tag ->
                TransactionTagCrossRef(transactionId = transactionId, tagId = tag.id)
            }
            transactionDao.addTagsToTransaction(crossRefs)
        }
    }

    suspend fun insertTransactionWithTags(transaction: Transaction, tags: Set<Tag>) {
        val transactionId = transactionDao.insert(transaction).toInt()
        if (tags.isNotEmpty()) {
            val crossRefs = tags.map { tag ->
                TransactionTagCrossRef(transactionId = transactionId, tagId = tag.id)
            }
            transactionDao.addTagsToTransaction(crossRefs)
        }
    }

    suspend fun updateTransactionWithTags(transaction: Transaction, tags: Set<Tag>) {
        transactionDao.update(transaction)
        transactionDao.clearTagsForTransaction(transaction.id)
        if (tags.isNotEmpty()) {
            val crossRefs = tags.map { tag ->
                TransactionTagCrossRef(transactionId = transaction.id, tagId = tag.id)
            }
            transactionDao.addTagsToTransaction(crossRefs)
        }
    }

    suspend fun insertTransactionWithTagsAndImages(
        transaction: Transaction,
        tags: Set<Tag>,
        imagePaths: List<String>
    ): Long {
        val newTransactionId = transactionDao.insert(transaction)
        if (tags.isNotEmpty()) {
            val crossRefs = tags.map { tag ->
                TransactionTagCrossRef(transactionId = newTransactionId.toInt(), tagId = tag.id)
            }
            transactionDao.addTagsToTransaction(crossRefs)
        }
        imagePaths.forEach { path ->
            val imageEntity = TransactionImage(
                transactionId = newTransactionId.toInt(),
                imageUri = path
            )
            transactionDao.insertImage(imageEntity)
        }
        return newTransactionId
    }

    suspend fun insert(transaction: Transaction) {
        transactionDao.insert(transaction)
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction)
    }

    suspend fun setSmsHash(transactionId: Int, smsHash: String) {
        transactionDao.setSmsHash(transactionId, smsHash)
    }

    suspend fun findLinkableTransactions(
        smsDate: Long,
        smsAmount: Double,
        transactionType: String
    ): List<Transaction> {
        val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000
        val startDate = smsDate - sevenDaysInMillis
        val endDate = smsDate + sevenDaysInMillis

        val amountRange = smsAmount * 0.10
        val minAmount = smsAmount - amountRange
        val maxAmount = smsAmount + amountRange

        return transactionDao.findLinkableTransactions(
            startDate = startDate,
            endDate = endDate,
            minAmount = minAmount,
            maxAmount = maxAmount,
            smsDate = smsDate,
            transactionType = transactionType
        )
    }

    fun getTransactionCountForMerchant(description: String): Flow<Int> {
        return transactionDao.getTransactionCountForMerchant(description)
    }

    // --- NEW: Expose DAO function for finding similar transactions ---
    suspend fun findSimilarTransactions(description: String, excludeId: Int): List<Transaction> {
        return transactionDao.findSimilarTransactions(description, excludeId)
    }

    // --- NEW: Expose DAO function for batch updating category ---
    suspend fun updateCategoryForIds(ids: List<Int>, categoryId: Int) {
        transactionDao.updateCategoryForIds(ids, categoryId)
    }

    // --- NEW: Expose DAO function for batch updating description ---
    suspend fun updateDescriptionForIds(ids: List<Int>, newDescription: String) {
        transactionDao.updateDescriptionForIds(ids, newDescription)
    }
}
