package io.pm.finlight

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the SmsParser utility.
 * These tests run on the local JVM and do not require an Android device or emulator.
 */
class SmsParserTest {
    // A mock map of user-defined mappings, empty for these tests.
    private val emptyMappings = emptyMap<String, String>()

    @Test
    fun `test parses debit message successfully`() {
        val smsBody = "Your account with HDFC Bank has been debited for Rs. 750.50 at Amazon on 22-Jun-2025."
        val mockSms = SmsMessage(id = 1L, sender = "AM-HDFCBK", body = smsBody, date = System.currentTimeMillis())

        val result = SmsParser.parse(mockSms, emptyMappings)

        assertNotNull("Parser should return a result for a debit message", result)
        assertEquals(750.50, result?.amount)
        assertEquals("expense", result?.transactionType)
        assertEquals("Amazon", result?.merchantName)
    }

    @Test
    fun `test parses credit message successfully`() {
        val smsBody = "You have received a credit of INR 5,000.00 from Freelance Client."
        val mockSms = SmsMessage(id = 2L, sender = "DM-SOMEBK", body = smsBody, date = System.currentTimeMillis())

        val result = SmsParser.parse(mockSms, emptyMappings)

        assertNotNull("Parser should return a result for a credit message", result)
        assertEquals(5000.00, result?.amount)
        assertEquals("income", result?.transactionType)
        assertEquals("Freelance Client", result?.merchantName)
    }

    @Test
    fun `test returns null for non-financial message`() {
        val smsBody = "Hello, just checking in. Are we still on for dinner tomorrow evening?"
        val mockSms = SmsMessage(id = 3L, sender = "+1234567890", body = smsBody, date = System.currentTimeMillis())

        val result = SmsParser.parse(mockSms, emptyMappings)

        assertNull("Parser should return null for a non-financial message", result)
    }

    @Test
    fun `test parses HDFC debit message with complex merchant name`() {
        val smsBody = "Spent Rs.388.19 On HDFC Bank Card 9922 At ..MC DONALDS_ on 2025-06-22."
        val mockSms = SmsMessage(id = 4L, sender = "JD-HDFCBK", body = smsBody, date = System.currentTimeMillis())

        val result = SmsParser.parse(mockSms, emptyMappings)

        assertNotNull(result)
        assertEquals(388.19, result?.amount)
        assertEquals("expense", result?.transactionType)
        assertEquals("MC DONALDS", result?.merchantName) // Note: underscore is cleaned up
    }
}
