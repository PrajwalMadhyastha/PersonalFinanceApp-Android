// =================================================================================
// FILE: ./app/src/test/java/io/pm/finlight/SmsParserTest.kt
// REASON: FEATURE - Added the `test_ignores_bharat_bill_pay_confirmation` unit
// test. This new test case specifically verifies that the parser correctly
// ignores credit card payment confirmation messages sent via the "Bharat Bill
// Payment System", ensuring the new negative keyword regex is effective.
// =================================================================================
package io.pm.finlight

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SmsParserTest {

    @Mock
    private lateinit var mockCustomSmsRuleDao: CustomSmsRuleDao

    @Mock
    private lateinit var mockMerchantRenameRuleDao: MerchantRenameRuleDao

    private val emptyMappings = emptyMap<String, String>()

    @Before
    fun setUp() {
        // No need to mock AppDatabase anymore
    }

    private suspend fun setupTest(customRules: List<CustomSmsRule> = emptyList(), renameRules: List<MerchantRenameRule> = emptyList()) {
        `when`(mockCustomSmsRuleDao.getAllRules()).thenReturn(flowOf(customRules))
        `when`(mockMerchantRenameRuleDao.getAllRules()).thenReturn(flowOf(renameRules))
    }

    @Test
    fun `test parses debit message successfully`() = runBlocking {
        setupTest()
        val smsBody = "Your account with HDFC Bank has been debited for Rs. 750.50 at Amazon on 22-Jun-2025."
        val mockSms = SmsMessage(id = 1L, sender = "AM-HDFCBK", body = smsBody, date = System.currentTimeMillis())

        val result = SmsParser.parse(mockSms, emptyMappings, mockCustomSmsRuleDao, mockMerchantRenameRuleDao)

        assertNotNull("Parser should return a result for a debit message", result)
        assertEquals(750.50, result?.amount)
        assertEquals("expense", result?.transactionType)
        assertEquals("Amazon", result?.merchantName)
    }

    @Test
    fun `test parses credit message successfully`() = runBlocking {
        setupTest()
        val smsBody = "You have received a credit of INR 5,000.00 from Freelance Client."
        val mockSms = SmsMessage(id = 2L, sender = "DM-SOMEBK", body = smsBody, date = System.currentTimeMillis())

        val result = SmsParser.parse(mockSms, emptyMappings, mockCustomSmsRuleDao, mockMerchantRenameRuleDao)

        assertNotNull("Parser should return a result for a credit message", result)
        assertEquals(5000.00, result?.amount)
        assertEquals("income", result?.transactionType)
        assertEquals("Freelance Client", result?.merchantName)
    }

    @Test
    fun `test returns null for non-financial message`() = runBlocking {
        setupTest()
        val smsBody = "Hello, just checking in. Are we still on for dinner tomorrow evening?"
        val mockSms = SmsMessage(id = 3L, sender = "+1234567890", body = smsBody, date = System.currentTimeMillis())

        val result = SmsParser.parse(mockSms, emptyMappings, mockCustomSmsRuleDao, mockMerchantRenameRuleDao)

        assertNull("Parser should return null for a non-financial message", result)
    }

    @Test
    fun `test parses SBI Credit Card message`() = runBlocking {
        setupTest()
        val smsBody = "Rs.267.00 spent on your SBI Credit Card ending with 3201 at HALLI THOTA on 29-06-25 via UPI (Ref No. 1231230123). Trxn. Not done by you? Report at https://sbicards.com/Dispute)"
        val mockSms = SmsMessage(id = 4L, sender = "VM-SBICRD", body = smsBody, date = System.currentTimeMillis())
        val result = SmsParser.parse(mockSms, emptyMappings, mockCustomSmsRuleDao, mockMerchantRenameRuleDao)

        assertNotNull(result)
        assertEquals(267.00, result?.amount)
        assertEquals("expense", result?.transactionType)
        assertEquals("HALLI THOTA", result?.merchantName)
        assertNotNull(result?.potentialAccount)
        assertEquals("SBI - xx3201", result?.potentialAccount?.formattedName)
        assertEquals("Credit Card", result?.potentialAccount?.accountType)
    }

    @Test
    fun `test parses ICICI debit message`() = runBlocking {
        setupTest()
        val smsBody = "ICICI Bank Acct XX823 debited for Rs 240.00 on 21-Jun-25; DAKSHIN CAFE credited. UPI: 552200221100. Call 18002661 for dispute."
        val mockSms = SmsMessage(id = 5L, sender = "DM-ICIBNK", body = smsBody, date = System.currentTimeMillis())
        val result = SmsParser.parse(mockSms, emptyMappings, mockCustomSmsRuleDao, mockMerchantRenameRuleDao)

        assertNotNull(result)
        assertEquals(240.00, result?.amount)
        assertEquals("expense", result?.transactionType)
        assertEquals("DAKSHIN CAFE", result?.merchantName)
        assertNotNull(result?.potentialAccount)
        assertEquals("ICICI Bank - xx823", result?.potentialAccount?.formattedName)
        assertEquals("Savings Account", result?.potentialAccount?.accountType)
    }

    @Test
    fun `test parses HDFC card message`() = runBlocking {
        setupTest()
        val smsBody = "[JD-HDFCBK-S] Spent Rs.388.19 On HDFC Bank Card 9922 At ..MC DONALDS_ on2025-06-22:08:01:24.Not You> To Block+Reissue Call 18002323232/SMS BLOCK CC 9922 to 123098123"
        val mockSms = SmsMessage(id = 6L, sender = "JD-HDFCBK", body = smsBody, date = System.currentTimeMillis())
        val result = SmsParser.parse(mockSms, emptyMappings, mockCustomSmsRuleDao, mockMerchantRenameRuleDao)

        assertNotNull(result)
        assertEquals(388.19, result?.amount)
        assertEquals("expense", result?.transactionType)
        assertEquals("MC DONALDS", result?.merchantName)
        assertNotNull(result?.potentialAccount)
        assertEquals("HDFC Bank - xx9922", result?.potentialAccount?.formattedName)
        assertEquals("Card", result?.potentialAccount?.accountType)
    }

    @Test
    fun `test parses Pluxee Meal Card message`() = runBlocking {
        setupTest()
        val smsBody = "Rs. 60.00 spent from Pluxee Meal Card wallet, card no.xx1345 on 30-06-2025 18:41:56 at KITCHEN AFF . Avl bal Rs.1824.65. Not you call 18002106919"
        val mockSms = SmsMessage(id = 7L, sender = "VD-PLUXEE", body = smsBody, date = System.currentTimeMillis())
        val result = SmsParser.parse(mockSms, emptyMappings, mockCustomSmsRuleDao, mockMerchantRenameRuleDao)

        assertNotNull(result)
        assertEquals(60.00, result?.amount)
        assertEquals("expense", result?.transactionType)
        assertEquals("KITCHEN AFF", result?.merchantName)
        assertNotNull(result?.potentialAccount)
        assertEquals("Pluxee - xx1345", result?.potentialAccount?.formattedName)
        assertEquals("Meal Card wallet", result?.potentialAccount?.accountType)
    }

    @Test
    fun `test parses ICICI credit message with tricky format`() = runBlocking {
        setupTest()
        val smsBody = "Dear Customer, Acct XX823 is credited with Rs 6000.00 on 26-Jun-25 from GANGA MANGA. UPI:5577822323232-ICICI Bank"
        val mockSms = SmsMessage(id = 8L, sender = "QP-ICIBNK", body = smsBody, date = System.currentTimeMillis())
        val result = SmsParser.parse(mockSms, emptyMappings, mockCustomSmsRuleDao, mockMerchantRenameRuleDao)

        assertNotNull(result)
        assertEquals(6000.00, result?.amount)
        assertEquals("income", result?.transactionType)
        assertEquals("GANGA MANGA", result?.merchantName)
        assertNotNull(result?.potentialAccount)
        assertEquals("ICICI Bank - xx823", result?.potentialAccount?.formattedName)
        assertEquals("Savings Account", result?.potentialAccount?.accountType)
    }

    @Test
    fun `test ignores invoice message`() = runBlocking {
        setupTest()
        val smsBody = "An Invoice of Rs.330.8 for A4 Block-108 is raised. Pay at https://nbhd.co/NBHood/g/szBBpng. Ignore if paid - NoBrokerHood"
        val mockSms = SmsMessage(id = 9L, sender = "VM-NBHOOD", body = smsBody, date = System.currentTimeMillis())
        val result = SmsParser.parse(mockSms, emptyMappings, mockCustomSmsRuleDao, mockMerchantRenameRuleDao)
        assertNull("Parser should ignore invoice messages", result)
    }

    @Test
    fun `test ignores successful payment confirmation`() = runBlocking {
        setupTest()
        val smsBody = "Your payment of Rs.330.80 for A4-108 against Water Charges is successful. Regards NoBrokerHood"
        val mockSms = SmsMessage(id = 10L, sender = "VM-NBHOOD", body = smsBody, date = System.currentTimeMillis())
        val result = SmsParser.parse(mockSms, emptyMappings, mockCustomSmsRuleDao, mockMerchantRenameRuleDao)
        assertNull("Parser should ignore successful payment confirmations", result)
    }

    @Test
    fun `test parses ICICI NEFT credit message`() = runBlocking {
        setupTest()
        val smsBody = "ICICI Bank Account XX823 credited:Rs. 1,133.00 on 01-Jul-25. Info NEFT-HDFCN5202507024345356218-. Available Balance is Rs. 1,858.35."
        val mockSms = SmsMessage(id = 11L, sender = "VM-ICIBNK", body = smsBody, date = System.currentTimeMillis())
        val result = SmsParser.parse(mockSms, emptyMappings, mockCustomSmsRuleDao, mockMerchantRenameRuleDao)

        assertNotNull(result)
        assertEquals(1133.00, result?.amount)
        assertEquals("income", result?.transactionType)
        assertEquals("NEFT-HDFCN5202507024345356218-", result?.merchantName)
        assertNotNull(result?.potentialAccount)
        assertEquals("ICICI Bank - xx823", result?.potentialAccount?.formattedName)
        assertEquals("Bank Account", result?.potentialAccount?.accountType)
    }

    @Test
    fun `test ignores HDFC NEFT credit message`() = runBlocking {
        setupTest()
        val smsBody = "HDFC Bank : NEFT money transfer Txn No HDFCN520253454560344 for Rs INR 1,500.00 has been credited to Manga Penga on 01-07-2025 at 08:05:30"
        val mockSms = SmsMessage(id = 12L, sender = "VM-HDFCBK", body = smsBody, date = System.currentTimeMillis())
        val result = SmsParser.parse(mockSms, emptyMappings, mockCustomSmsRuleDao, mockMerchantRenameRuleDao)
        assertNull("Parser should ignore has been credited to messages", result)
    }

    @Test
    fun `test ignores credit card payment confirmation`() = runBlocking {
        setupTest()
        val smsBody = "Payment of INR 1180.01 has been received towards your SBI card XX1121"
        val mockSms = SmsMessage(id = 13L, sender = "DM-SBICRD", body = smsBody, date = System.currentTimeMillis())
        val result = SmsParser.parse(mockSms, emptyMappings, mockCustomSmsRuleDao, mockMerchantRenameRuleDao)
        assertNull("Parser should ignore credit card payment confirmations", result)
    }

    @Test
    fun `test ignores bharat bill pay confirmation`() = runBlocking {
        setupTest()
        val smsBody = "Payment of Rs 356.33 has been received on your ICICI Bank Credit Card XX2529 through Bharat Bill Payment System on 03-JUL-25."
        val mockSms = SmsMessage(id = 14L, sender = "DM-ICIBNK", body = smsBody, date = System.currentTimeMillis())
        val result = SmsParser.parse(mockSms, emptyMappings, mockCustomSmsRuleDao, mockMerchantRenameRuleDao)
        assertNull("Parser should ignore Bharat Bill Pay confirmations", result)
    }
}
