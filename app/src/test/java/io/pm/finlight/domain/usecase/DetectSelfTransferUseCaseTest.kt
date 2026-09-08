package io.pm.finlight.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.pm.finlight.Account
import io.pm.finlight.BaseViewModelTest
import io.pm.finlight.ITransactionRepository
import io.pm.finlight.Transaction
import io.pm.finlight.TransactionType
import io.pm.finlight.data.db.AppDatabase
import io.pm.finlight.data.db.dao.AccountAliasDao
import io.pm.finlight.data.db.dao.AccountDao
import io.pm.finlight.data.db.dao.TransactionAnalyticsDao
import io.pm.finlight.data.db.dao.TransactionQueryDao
import io.pm.finlight.data.db.dao.TransactionReimbursementDao
import io.pm.finlight.data.db.dao.TransactionWriteDao
import io.pm.finlight.data.db.entity.AccountAlias
import io.pm.finlight.utils.TestDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetectSelfTransferUseCaseTest : BaseViewModelTest() {
    private val transactionRepository: ITransactionRepository = mockk(relaxed = true)
    private val accountDao: AccountDao = mockk(relaxed = true)
    private val accountAliasDao: AccountAliasDao = mockk(relaxed = true)
    private lateinit var testDispatcherProvider: TestDispatcherProvider
    private lateinit var useCase: DetectSelfTransferUseCase

    @Before
    override fun setup() {
        super.setup()
        testDispatcherProvider = TestDispatcherProvider(testDispatcher)
        useCase =
            DetectSelfTransferUseCase(
                transactionRepository = transactionRepository,
                accountDao = accountDao,
                accountAliasDao = accountAliasDao,
                dispatcherProvider = testDispatcherProvider,
            )
    }

    @Test
    fun `strict time match within 5 minutes links transactions atomically`() =
        runTest(testDispatcher) {
            val newTxn =
                Transaction(
                    id = 1,
                    description = "Withdrawal",
                    amount = 500.0,
                    date = 1000000L,
                    accountId = 1,
                    transactionType = TransactionType.EXPENSE,
                    sourceSmsId = 10,
                    categoryId = null,
                    notes = null,
                )
            val candidate =
                Transaction(
                    id = 2,
                    description = "Deposit",
                    amount = 500.0,
                    date = 1000000L + (4 * 60 * 1000L),
                    accountId = 2,
                    transactionType = TransactionType.INCOME,
                    sourceSmsId = 20,
                    categoryId = null,
                    notes = null,
                )

            coEvery {
                transactionRepository.findPotentialTransfers(
                    amount = 500.0,
                    accountId = 1,
                    transactionType = TransactionType.EXPENSE,
                    startTime = any(),
                    endTime = any(),
                )
            } returns listOf(candidate)

            useCase(newTxn)

            coVerify(exactly = 1) { transactionRepository.linkTransfer(1, 2) }
        }

    @Test
    fun `loose time match with alias digit match links transactions`() =
        runTest(testDispatcher) {
            val newTxn =
                Transaction(
                    id = 1,
                    description = "Transfer",
                    originalDescription = "Transfer to 1234",
                    amount = 1000.0,
                    date = 1000000L,
                    accountId = 1,
                    transactionType = TransactionType.EXPENSE,
                    sourceSmsId = 10,
                    categoryId = null,
                    notes = null,
                )
            val candidate =
                Transaction(
                    id = 2,
                    description = "Received",
                    originalDescription = "Received from a/c",
                    amount = 1000.0,
                    date = 1000000L + (2 * 3600 * 1000L),
                    accountId = 2,
                    transactionType = TransactionType.INCOME,
                    sourceSmsId = 20,
                    categoryId = null,
                    notes = null,
                )

            coEvery {
                transactionRepository.findPotentialTransfers(
                    amount = 1000.0,
                    accountId = 1,
                    transactionType = TransactionType.EXPENSE,
                    startTime = any(),
                    endTime = any(),
                )
            } returns listOf(candidate)

            val alias = AccountAlias(aliasName = "HDFC-1234", destinationAccountId = 2)
            coEvery { accountAliasDao.getAliasesForAccount(1) } returns emptyList()
            coEvery { accountAliasDao.getAliasesForAccount(2) } returns listOf(alias)
            coEvery { accountDao.getAccountByIdBlocking(1) } returns Account(id = 1, name = "Account1", type = "bank")
            coEvery { accountDao.getAccountByIdBlocking(2) } returns Account(id = 2, name = "Account2", type = "bank")

            useCase(newTxn)

            coVerify(exactly = 1) { transactionRepository.linkTransfer(1, 2) }
        }

    @Test
    fun `loose time match with account alias token overlap links transactions`() =
        runTest(testDispatcher) {
            val newTxn =
                Transaction(
                    id = 1,
                    description = "Transfer",
                    originalDescription = "Sent to my savings account",
                    amount = 1500.0,
                    date = 1000000L,
                    accountId = 1,
                    transactionType = TransactionType.EXPENSE,
                    sourceSmsId = 10,
                    categoryId = null,
                    notes = null,
                )
            val candidate =
                Transaction(
                    id = 2,
                    description = "Deposit",
                    originalDescription = "Received cash",
                    amount = 1500.0,
                    date = 1000000L + (2 * 3600 * 1000L),
                    accountId = 2,
                    transactionType = TransactionType.INCOME,
                    sourceSmsId = 20,
                    categoryId = null,
                    notes = null,
                )

            coEvery {
                transactionRepository.findPotentialTransfers(
                    amount = 1500.0,
                    accountId = 1,
                    transactionType = TransactionType.EXPENSE,
                    startTime = any(),
                    endTime = any(),
                )
            } returns listOf(candidate)

            val alias = AccountAlias(aliasName = "savings account", destinationAccountId = 2)
            coEvery { accountAliasDao.getAliasesForAccount(1) } returns emptyList()
            coEvery { accountAliasDao.getAliasesForAccount(2) } returns listOf(alias)
            coEvery { accountDao.getAccountByIdBlocking(1) } returns Account(id = 1, name = "Primary Checking", type = "bank")
            coEvery { accountDao.getAccountByIdBlocking(2) } returns Account(id = 2, name = "Secondary Savings", type = "bank")

            useCase(newTxn)

            coVerify(exactly = 1) { transactionRepository.linkTransfer(1, 2) }
        }

    @Test
    fun `loose time match with candidate bank name token overlap links transactions`() =
        runTest(testDispatcher) {
            val newTxn =
                Transaction(
                    id = 1,
                    description = "Transfer",
                    originalDescription = "Sent money to State Bank of India main branch",
                    amount = 2500.0,
                    date = 1000000L,
                    accountId = 1,
                    transactionType = TransactionType.EXPENSE,
                    sourceSmsId = 10,
                    categoryId = null,
                    notes = null,
                )
            val candidate =
                Transaction(
                    id = 2,
                    description = "Received",
                    originalDescription = "Received from other account",
                    amount = 2500.0,
                    date = 1000000L + (3 * 3600 * 1000L),
                    accountId = 2,
                    transactionType = TransactionType.INCOME,
                    sourceSmsId = 20,
                    categoryId = null,
                    notes = null,
                )

            coEvery {
                transactionRepository.findPotentialTransfers(
                    amount = 2500.0,
                    accountId = 1,
                    transactionType = TransactionType.EXPENSE,
                    startTime = any(),
                    endTime = any(),
                )
            } returns listOf(candidate)

            coEvery { accountAliasDao.getAliasesForAccount(1) } returns emptyList()
            coEvery { accountAliasDao.getAliasesForAccount(2) } returns emptyList()
            coEvery { accountDao.getAccountByIdBlocking(1) } returns Account(id = 1, name = "ICICI Bank", type = "bank")
            coEvery { accountDao.getAccountByIdBlocking(2) } returns Account(id = 2, name = "State Bank of India", type = "bank")

            useCase(newTxn)

            coVerify(exactly = 1) { transactionRepository.linkTransfer(1, 2) }
        }

    @Test
    fun `loose time match with new transaction bank name token overlap links transactions`() =
        runTest(testDispatcher) {
            val newTxn =
                Transaction(
                    id = 1,
                    description = "Transfer",
                    originalDescription = "Sent money",
                    amount = 2500.0,
                    date = 1000000L,
                    accountId = 1,
                    transactionType = TransactionType.EXPENSE,
                    sourceSmsId = 10,
                    categoryId = null,
                    notes = null,
                )
            val candidate =
                Transaction(
                    id = 2,
                    description = "Received",
                    originalDescription = "Received from ICICI Bank salary account",
                    amount = 2500.0,
                    date = 1000000L + (3 * 3600 * 1000L),
                    accountId = 2,
                    transactionType = TransactionType.INCOME,
                    sourceSmsId = 20,
                    categoryId = null,
                    notes = null,
                )

            coEvery {
                transactionRepository.findPotentialTransfers(
                    amount = 2500.0,
                    accountId = 1,
                    transactionType = TransactionType.EXPENSE,
                    startTime = any(),
                    endTime = any(),
                )
            } returns listOf(candidate)

            coEvery { accountAliasDao.getAliasesForAccount(1) } returns emptyList()
            coEvery { accountAliasDao.getAliasesForAccount(2) } returns emptyList()
            coEvery { accountDao.getAccountByIdBlocking(1) } returns Account(id = 1, name = "ICICI Bank", type = "bank")
            coEvery { accountDao.getAccountByIdBlocking(2) } returns Account(id = 2, name = "Axis Bank", type = "bank")

            useCase(newTxn)

            coVerify(exactly = 1) { transactionRepository.linkTransfer(1, 2) }
        }

    @Test
    fun `loose time match with NEFT transfer keywords links transactions`() =
        runTest(testDispatcher) {
            val newTxn =
                Transaction(
                    id = 1,
                    description = "NEFT transfer sent",
                    originalDescription = "neft transfer sent ref 9988",
                    amount = 1200.0,
                    date = 1000000L,
                    accountId = 1,
                    transactionType = TransactionType.EXPENSE,
                    sourceSmsId = 10,
                    categoryId = null,
                    notes = null,
                )
            val candidate =
                Transaction(
                    id = 2,
                    description = "NEFT transfer recd",
                    originalDescription = "neft transfer received ref 9988",
                    amount = 1200.0,
                    date = 1000000L + (1 * 3600 * 1000L),
                    accountId = 2,
                    transactionType = TransactionType.INCOME,
                    sourceSmsId = 20,
                    categoryId = null,
                    notes = null,
                )

            coEvery {
                transactionRepository.findPotentialTransfers(
                    amount = 1200.0,
                    accountId = 1,
                    transactionType = TransactionType.EXPENSE,
                    startTime = any(),
                    endTime = any(),
                )
            } returns listOf(candidate)

            coEvery { accountAliasDao.getAliasesForAccount(1) } returns emptyList()
            coEvery { accountAliasDao.getAliasesForAccount(2) } returns emptyList()
            coEvery { accountDao.getAccountByIdBlocking(1) } returns Account(id = 1, name = "Acc1", type = "bank")
            coEvery { accountDao.getAccountByIdBlocking(2) } returns Account(id = 2, name = "Acc2", type = "bank")

            useCase(newTxn)

            coVerify(exactly = 1) { transactionRepository.linkTransfer(1, 2) }
        }

    @Test
    fun `loose time match without keyword or alias match does not link`() =
        runTest(testDispatcher) {
            val newTxn =
                Transaction(
                    id = 1,
                    description = "Expense",
                    originalDescription = "grocery store purchase",
                    amount = 100.0,
                    date = 1000000L,
                    accountId = 1,
                    transactionType = TransactionType.EXPENSE,
                    sourceSmsId = 10,
                    categoryId = null,
                    notes = null,
                )
            val candidate =
                Transaction(
                    id = 2,
                    description = "Income",
                    originalDescription = "freelance payment",
                    amount = 100.0,
                    date = 1000000L + (2 * 3600 * 1000L),
                    accountId = 2,
                    transactionType = TransactionType.INCOME,
                    sourceSmsId = 20,
                    categoryId = null,
                    notes = null,
                )

            coEvery {
                transactionRepository.findPotentialTransfers(
                    amount = 100.0,
                    accountId = 1,
                    transactionType = TransactionType.EXPENSE,
                    startTime = any(),
                    endTime = any(),
                )
            } returns listOf(candidate)

            coEvery { accountAliasDao.getAliasesForAccount(1) } returns emptyList()
            coEvery { accountAliasDao.getAliasesForAccount(2) } returns emptyList()
            coEvery { accountDao.getAccountByIdBlocking(1) } returns Account(id = 1, name = "Acc1", type = "bank")
            coEvery { accountDao.getAccountByIdBlocking(2) } returns Account(id = 2, name = "Acc2", type = "bank")

            useCase(newTxn)

            coVerify(exactly = 0) { transactionRepository.linkTransfer(any(), any()) }
        }

    @Test
    fun `skips execution when transaction is invalid for transfer linking`() =
        runTest(testDispatcher) {
            val noSms =
                Transaction(
                    id = 1,
                    description = "A",
                    amount = 100.0,
                    date = 1000L,
                    accountId = 1,
                    transactionType = TransactionType.EXPENSE,
                    sourceSmsId = null,
                    categoryId = null,
                    notes = null,
                )
            val alreadyLinked =
                Transaction(
                    id = 2,
                    description = "B",
                    amount = 100.0,
                    date = 1000L,
                    accountId = 1,
                    transactionType = TransactionType.EXPENSE,
                    sourceSmsId = 10,
                    linkedTransferId = 99,
                    categoryId = null,
                    notes = null,
                )
            val excluded =
                Transaction(
                    id = 3,
                    description = "C",
                    amount = 100.0,
                    date = 1000L,
                    accountId = 1,
                    transactionType = TransactionType.EXPENSE,
                    sourceSmsId = 10,
                    isExcluded = true,
                    categoryId = null,
                    notes = null,
                )
            val split =
                Transaction(
                    id = 4,
                    description = "D",
                    amount = 100.0,
                    date = 1000L,
                    accountId = 1,
                    transactionType = TransactionType.EXPENSE,
                    sourceSmsId = 10,
                    isSplit = true,
                    categoryId = null,
                    notes = null,
                )

            useCase(noSms)
            useCase(alreadyLinked)
            useCase(excluded)
            useCase(split)

            coVerify(exactly = 0) {
                transactionRepository.findPotentialTransfers(any(), any(), any(), any(), any())
            }
        }

    @Test
    fun `multiple candidates links only the first match`() =
        runTest(testDispatcher) {
            val newTxn =
                Transaction(
                    id = 1,
                    description = "Transfer",
                    amount = 100.0,
                    date = 1000000L,
                    accountId = 1,
                    transactionType = TransactionType.EXPENSE,
                    sourceSmsId = 10,
                    categoryId = null,
                    notes = null,
                )
            val candidate1 =
                Transaction(
                    id = 2,
                    description = "Match1",
                    amount = 100.0,
                    date = 1000000L + 60000L,
                    accountId = 2,
                    transactionType = TransactionType.INCOME,
                    sourceSmsId = 20,
                    categoryId = null,
                    notes = null,
                )
            val candidate2 =
                Transaction(
                    id = 3,
                    description = "Match2",
                    amount = 100.0,
                    date = 1000000L + 120000L,
                    accountId = 3,
                    transactionType = TransactionType.INCOME,
                    sourceSmsId = 30,
                    categoryId = null,
                    notes = null,
                )

            coEvery {
                transactionRepository.findPotentialTransfers(
                    amount = 100.0,
                    accountId = 1,
                    transactionType = TransactionType.EXPENSE,
                    startTime = any(),
                    endTime = any(),
                )
            } returns listOf(candidate1, candidate2)

            useCase(newTxn)

            coVerify(exactly = 1) { transactionRepository.linkTransfer(1, 2) }
            coVerify(exactly = 0) { transactionRepository.linkTransfer(1, 3) }
            coVerify(exactly = 0) { transactionRepository.linkTransfer(3, 1) }
        }

    @Test
    fun `detectAndLinkSelfTransfer delegation method delegates to invoke`() =
        runTest(testDispatcher) {
            val newTxn =
                Transaction(
                    id = 1,
                    description = "Withdrawal",
                    amount = 500.0,
                    date = 1000000L,
                    accountId = 1,
                    transactionType = TransactionType.EXPENSE,
                    sourceSmsId = 10,
                    categoryId = null,
                    notes = null,
                )
            val candidate =
                Transaction(
                    id = 2,
                    description = "Deposit",
                    amount = 500.0,
                    date = 1000000L + (2 * 60 * 1000L),
                    accountId = 2,
                    transactionType = TransactionType.INCOME,
                    sourceSmsId = 20,
                    categoryId = null,
                    notes = null,
                )

            coEvery {
                transactionRepository.findPotentialTransfers(any(), any(), any(), any(), any())
            } returns listOf(candidate)

            useCase.detectAndLinkSelfTransfer(newTxn)

            coVerify(exactly = 1) { transactionRepository.linkTransfer(1, 2) }
        }

    @Test
    fun `secondary constructor initializes and works with AppDatabase`() =
        runTest(testDispatcher) {
            val db: AppDatabase = mockk(relaxed = true)
            val writeDao: TransactionWriteDao = mockk(relaxed = true)
            val queryDao: TransactionQueryDao = mockk(relaxed = true)
            val analyticsDao: TransactionAnalyticsDao = mockk(relaxed = true)
            val reimbursementDao: TransactionReimbursementDao = mockk(relaxed = true)
            val accDao: AccountDao = mockk(relaxed = true)
            val aliasDao: AccountAliasDao = mockk(relaxed = true)

            every { db.transactionWriteDao() } returns writeDao
            every { db.transactionQueryDao() } returns queryDao
            every { db.transactionAnalyticsDao() } returns analyticsDao
            every { db.transactionReimbursementDao() } returns reimbursementDao
            every { db.accountDao() } returns accDao
            every { db.accountAliasDao() } returns aliasDao

            val secondaryUseCase = DetectSelfTransferUseCase(db, testDispatcherProvider)
            val newTxn =
                Transaction(
                    id = 1,
                    description = "No SMS",
                    amount = 10.0,
                    date = 1000L,
                    accountId = 1,
                    transactionType = TransactionType.EXPENSE,
                    sourceSmsId = null,
                    categoryId = null,
                    notes = null,
                )

            secondaryUseCase(newTxn)
            coVerify(exactly = 0) { queryDao.findPotentialTransfers(any(), any(), any(), any(), any()) }
        }
}
