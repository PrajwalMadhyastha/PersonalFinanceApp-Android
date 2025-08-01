package io.pm.finlight.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneNotNull
import app.cash.sqldelight.coroutines.mapToOneOrNull
import io.pm.finlight.data.db.entity.Tag
import io.pm.finlight.data.db.entity.Transaction
import io.pm.finlight.data.db.entity.TransactionImage
import io.pm.finlight.data.model.*
import io.pm.finlight.shared.db.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository for handling all transaction-related data operations.
 * This class is the single source of truth for transaction data and abstracts
 * the data source (SQLDelight database) from the rest of the application.
 *
 * NOTE: This class has been refactored to use SQLDelight queries instead of Room DAOs.
 */
class TransactionRepository(
    private val transactionQueries: TransactionQueries,
    private val transactionTagCrossRefQueries: Transaction_tag_cross_refQueries,
    private val tagQueries: TagQueries,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    val recentTransactions: Flow<List<TransactionDetails>> =
        transactionQueries.selectRecentTransactionDetails(limit = 10, mapper = ::mapTransactionDetails)
            .asFlow()
            .mapToList(dispatcher)

    fun getTransactionDetailsForRange(
        start: Long,
        end: Long,
        keyword: String?,
        accountId: Int?,
        categoryId: Int?
    ): Flow<List<TransactionDetails>> {
        val finalKeyword = if (keyword.isNullOrBlank()) null else "%$keyword%"
        return transactionQueries.getTransactionDetailsForRange(
            startDate = start,
            endDate = end,
            accountId = accountId?.toLong(),
            categoryId = categoryId?.toLong(),
            keyword = finalKeyword,
            mapper = ::mapTransactionDetails
        ).asFlow().mapToList(dispatcher)
    }

    fun getFinancialSummaryForRangeFlow(start: Long, end: Long): Flow<FinancialSummary?> {
        return transactionQueries.getFinancialSummaryForRange(start, end)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map {
                it?.let { summary ->
                    FinancialSummary(
                        totalIncome = summary.totalIncome ?: 0.0,
                        totalExpenses = summary.totalExpenses ?: 0.0
                    )
                }
            }
    }

    fun getSpendingByCategoryForMonth(
        startDate: Long,
        endDate: Long,
        keyword: String?,
        accountId: Int?,
        categoryId: Int?
    ): Flow<List<CategorySpending>> {
        val finalKeyword = if (keyword.isNullOrBlank()) null else "%$keyword%"
        return transactionQueries.getSpendingByCategoryForMonth(
            startDate = startDate,
            endDate = endDate,
            accountId = accountId?.toLong(),
            categoryId = categoryId?.toLong(),
            keyword = finalKeyword,
            mapper = ::mapCategorySpending
        ).asFlow().mapToList(dispatcher)
    }

    fun getSpendingByMerchantForMonth(
        startDate: Long,
        endDate: Long,
        keyword: String?,
        accountId: Int?,
        categoryId: Int?
    ): Flow<List<MerchantSpendingSummary>> {
        val finalKeyword = if (keyword.isNullOrBlank()) null else "%$keyword%"
        return transactionQueries.getSpendingByMerchantForMonth(
            startDate = startDate,
            endDate = endDate,
            accountId = accountId?.toLong(),
            categoryId = categoryId?.toLong(),
            keyword = finalKeyword,
            mapper = ::mapMerchantSpendingSummary
        ).asFlow().mapToList(dispatcher)
    }

    fun getMonthlyTrends(startDate: Long): Flow<List<MonthlyTrend>> {
        return transactionQueries.getMonthlyTrends(startDate, mapper = ::mapMonthlyTrend)
            .asFlow()
            .mapToList(dispatcher)
    }

    suspend fun insertTransactionWithTagsAndImages(
        transaction: Transaction,
        tags: Set<Tag>,
        imagePaths: List<String>
    ): Long = withContext(dispatcher) {
        var newTxnId: Long = -1
        transactionQueries.transaction {
            transactionQueries.insert(
                description = transaction.description,
                categoryId = transaction.categoryId?.toLong(),
                amount = transaction.amount,
                date = transaction.date,
                accountId = transaction.accountId.toLong(),
                notes = transaction.notes,
                transactionType = transaction.transactionType,
                sourceSmsId = transaction.sourceSmsId,
                sourceSmsHash = transaction.sourceSmsHash,
                source = transaction.source,
                originalDescription = transaction.originalDescription,
                isExcluded = if (transaction.isExcluded) 1L else 0L,
                smsSignature = transaction.smsSignature,
                originalAmount = transaction.originalAmount,
                currencyCode = transaction.currencyCode,
                conversionRate = transaction.conversionRate,
                isSplit = if (transaction.isSplit) 1L else 0L
            )
            newTxnId = transactionQueries.lastInsertRowId().executeAsOne()

            tags.forEach { tag ->
                transactionTagCrossRefQueries.insert(newTxnId, tag.id.toLong())
            }

            imagePaths.forEach { path ->
                transactionQueries.insertImage(transactionId = newTxnId, imageUri = path)
            }
        }
        return@withContext newTxnId
    }

    fun getTransactionById(id: Int): Flow<Transaction?> {
        return transactionQueries.selectById(id.toLong(), ::mapTransaction)
            .asFlow()
            .mapToOneOrNull(dispatcher)
    }

    fun getTransactionDetailsById(id: Int): Flow<TransactionDetails?> {
        return transactionQueries.selectTransactionDetailsById(id.toLong(), ::mapTransactionDetails)
            .asFlow()
            .mapToOneOrNull(dispatcher)
    }

    fun getTransactionCountForMerchant(description: String): Flow<Int> {
        return transactionQueries.getTransactionCountForMerchant(description)
            .asFlow()
            .mapToOneNotNull(dispatcher)
            .map { it.toInt() }
    }

    suspend fun updateDescription(id: Int, newDescription: String) = withContext(dispatcher) {
        transactionQueries.updateDescription(newDescription, id.toLong())
    }

    suspend fun updateAmount(id: Int, newAmount: Double) = withContext(dispatcher) {
        transactionQueries.updateAmount(newAmount, id.toLong())
    }

    suspend fun updateNotes(id: Int, newNotes: String?) = withContext(dispatcher) {
        transactionQueries.updateNotes(newNotes, id.toLong())
    }

    suspend fun updateCategoryId(id: Int, newCategoryId: Int?) = withContext(dispatcher) {
        transactionQueries.updateCategoryId(newCategoryId?.toLong(), id.toLong())
    }

    suspend fun updateAccountId(id: Int, newAccountId: Int) = withContext(dispatcher) {
        transactionQueries.updateAccountId(newAccountId.toLong(), id.toLong())
    }

    suspend fun updateDate(id: Int, newDate: Long) = withContext(dispatcher) {
        transactionQueries.updateDate(newDate, id.toLong())
    }

    suspend fun updateExclusionStatus(id: Int, isExcluded: Boolean) = withContext(dispatcher) {
        transactionQueries.updateExclusionStatus(if (isExcluded) 1L else 0L, id.toLong())
    }

    suspend fun updateTagsForTransaction(transactionId: Int, tags: Set<Tag>) = withContext(dispatcher) {
        transactionTagCrossRefQueries.transaction {
            transactionTagCrossRefQueries.deleteByTransactionId(transactionId.toLong())
            tags.forEach { tag ->
                transactionTagCrossRefQueries.insert(transactionId.toLong(), tag.id.toLong())
            }
        }
    }

    fun getTagsForTransaction(transactionId: Int): Flow<List<Tag>> {
        return tagQueries.selectTagsForTransaction(transactionId.toLong(), ::mapTag)
            .asFlow()
            .mapToList(dispatcher)
    }

    fun getImagesForTransaction(transactionId: Int): Flow<List<TransactionImage>> {
        return transactionQueries.selectImagesForTransaction(transactionId.toLong(), ::mapTransactionImage)
            .asFlow()
            .mapToList(dispatcher)
    }

    suspend fun addImageToTransaction(transactionId: Int, imagePath: String) = withContext(dispatcher) {
        transactionQueries.insertImage(transactionId.toLong(), imagePath)
    }

    suspend fun deleteImage(image: TransactionImage) = withContext(dispatcher) {
        transactionQueries.deleteImageById(image.id.toLong())
    }

    // --- Private Mapper Functions ---

    private fun mapTransaction(
        id: Long, description: String, categoryId: Long?, amount: Double, date: Long,
        accountId: Long, notes: String?, transactionType: String, sourceSmsId: Long?,
        sourceSmsHash: String?, source: String, originalDescription: String?,
        isExcluded: Long, smsSignature: String?, originalAmount: Double?,
        currencyCode: String?, conversionRate: Double?, isSplit: Long
    ): Transaction {
        return Transaction(
            id = id.toInt(), description = description, categoryId = categoryId?.toInt(),
            amount = amount, date = date, accountId = accountId.toInt(), notes = notes,
            transactionType = transactionType, sourceSmsId = sourceSmsId,
            sourceSmsHash = sourceSmsHash, source = source,
            originalDescription = originalDescription, isExcluded = isExcluded == 1L,
            smsSignature = smsSignature, originalAmount = originalAmount,
            currencyCode = currencyCode, conversionRate = conversionRate, isSplit = isSplit == 1L
        )
    }

    private fun mapTransactionDetails(
        id: Long, description: String, categoryId: Long?, amount: Double, date: Long,
        accountId: Long, notes: String?, transactionType: String, sourceSmsId: Long?,
        sourceSmsHash: String?, source: String, originalDescription: String?,
        isExcluded: Long, smsSignature: String?, originalAmount: Double?,
        currencyCode: String?, conversionRate: Double?, isSplit: Long,
        accountName: String?, categoryName: String?, categoryIconKey: String?, categoryColorKey: String?
    ): TransactionDetails {
        val transaction = mapTransaction(
            id, description, categoryId, amount, date, accountId, notes,
            transactionType, sourceSmsId, sourceSmsHash, source, originalDescription,
            isExcluded, smsSignature, originalAmount, currencyCode, conversionRate, isSplit
        )
        // Note: The 'images' list is loaded separately when needed.
        return TransactionDetails(
            transaction = transaction, images = emptyList(), accountName = accountName,
            categoryName = categoryName, categoryIconKey = categoryIconKey, categoryColorKey = categoryColorKey
        )
    }

    private fun mapCategorySpending(
        categoryName: String, totalAmount: Double, colorKey: String?, iconKey: String?
    ): CategorySpending {
        return CategorySpending(categoryName, totalAmount, colorKey, iconKey)
    }

    private fun mapMerchantSpendingSummary(
        merchantName: String, totalAmount: Double, transactionCount: Long
    ): MerchantSpendingSummary {
        return MerchantSpendingSummary(merchantName, totalAmount, transactionCount.toInt())
    }

    private fun mapMonthlyTrend(
        monthYear: String, totalIncome: Double?, totalExpenses: Double?
    ): MonthlyTrend {
        return MonthlyTrend(monthYear, totalIncome ?: 0.0, totalExpenses ?: 0.0)
    }

    private fun mapTag(id: Long, name: String): Tag {
        return Tag(id.toInt(), name)
    }

    private fun mapTransactionImage(id: Long, transactionId: Long, imageUri: String): TransactionImage {
        return TransactionImage(id.toInt(), transactionId.toInt(), imageUri)
    }
}
