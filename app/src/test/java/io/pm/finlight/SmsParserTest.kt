package io.pm.finlight

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the SmsParser utility.
 */
class SmsParserTest {
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

    // --- NEW TESTS FOR THE PROVIDED EXAMPLES ---

    @Test
    fun `test parses SBI Credit Card message`() {
        val smsBody = "Rs.267.00 spent on your SBI Credit Card ending with 3201 at HALLI THOTA on 29-06-25 via UPI (Ref No. 1231230123). Trxn. Not done by you? Report at https://sbicards.com/Dispute)"
        val mockSms = SmsMessage(id = 4L, sender = "VM-SBICRD", body = smsBody, date = System.currentTimeMillis())
        val result = SmsParser.parse(mockSms, emptyMappings)

        assertNotNull(result)
        assertEquals(267.00, result?.amount)
        assertEquals("expense", result?.transactionType)
        assertEquals("HALLI THOTA", result?.merchantName)
        assertNotNull(result?.potentialAccount)
        assertEquals("SBI - xx3201", result?.potentialAccount?.formattedName)
        assertEquals("Credit Card", result?.potentialAccount?.accountType)
    }

    @Test
    fun `test parses ICICI debit message`() {
        val smsBody = "ICICI Bank Acct XX823 debited for Rs 240.00 on 21-Jun-25; DAKSHIN CAFE credited. UPI: 552200221100. Call 18002661 for dispute."
        val mockSms = SmsMessage(id = 5L, sender = "DM-ICIBNK", body = smsBody, date = System.currentTimeMillis())
        val result = SmsParser.parse(mockSms, emptyMappings)

        assertNotNull(result)
        assertEquals(240.00, result?.amount)
        assertEquals("expense", result?.transactionType)
        assertEquals("DAKSHIN CAFE", result?.merchantName)
        assertNotNull(result?.potentialAccount)
        assertEquals("ICICI Bank - xx823", result?.potentialAccount?.formattedName)
        assertEquals("Savings Account", result?.potentialAccount?.accountType)
    }

    @Test
    fun `test parses HDFC card message`() {
        val smsBody = "[JD-HDFCBK-S] Spent Rs.388.19 On HDFC Bank Card 9922 At ..MC DONALDS_ on2025-06-22:08:01:24.Not You> To Block+Reissue Call 1800232323232/SMS BLOCK CC 9922 to 123098123"
        val mockSms = SmsMessage(id = 6L, sender = "JD-HDFCBK", body = smsBody, date = System.currentTimeMillis())
        val result = SmsParser.parse(mockSms, emptyMappings)

        assertNotNull(result)
        assertEquals(388.19, result?.amount)
        assertEquals("expense", result?.transactionType)
        assertEquals("MC DONALDS", result?.merchantName)
        assertNotNull(result?.potentialAccount)
        assertEquals("HDFC Bank - xx9922", result?.potentialAccount?.formattedName)
        assertEquals("Card", result?.potentialAccount?.accountType)
    }

    @Test
    fun `test parses Pluxee Meal Card message`() {
        val smsBody = "[VD-PLUXEE-S] Rs.40.00 spent from Pluxee. Meal Card wallet, card no.xx7809 on 22-06-2025 14:46:11 at KITCHEN AFF . Avl bal Rs.3402.65. Not you call 213091823"
        val mockSms = SmsMessage(id = 7L, sender = "VD-PLUXEE", body = smsBody, date = System.currentTimeMillis())
        val result = SmsParser.parse(mockSms, emptyMappings)

        assertNotNull(result)
        assertEquals(40.00, result?.amount)
        assertEquals("expense", result?.transactionType)
        assertEquals("KITCHEN AFF", result?.merchantName)
        assertNotNull(result?.potentialAccount)
        assertEquals("Pluxee - xx7809", result?.potentialAccount?.formattedName)
        assertEquals("Meal Card wallet", result?.potentialAccount?.accountType)
    }

    @Test
    fun `test parses ICICI credit message with tricky format`() {
        val smsBody = "Dear Customer, Acct XX823 is credited with Rs 6000.00 on 26-Jun-25 from GANGA MANGA. UPI:5577822323232-ICICI Bank"
        val mockSms = SmsMessage(id = 8L, sender = "QP-ICIBNK", body = smsBody, date = System.currentTimeMillis())
        val result = SmsParser.parse(mockSms, emptyMappings)

        assertNotNull(result)
        assertEquals(6000.00, result?.amount)
        assertEquals("income", result?.transactionType)
        assertEquals("GANGA MANGA", result?.merchantName)
        assertNotNull(result?.potentialAccount)
        assertEquals("ICICI Bank - xx823", result?.potentialAccount?.formattedName)
        assertEquals("Savings Account", result?.potentialAccount?.accountType)
    }
}
