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
        val smsBody = "[JD-HDFCBK-S] Spent Rs.388.19 On HDFC Bank Card 9922 At ..MC DONALDS_ on2025-06-22:08:01:24.Not You> To Block+Reissue Call 18002323232/SMS BLOCK CC 9922 to 123098123"
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
        val smsBody = "Rs. 60.00 spent from Pluxee Meal Card wallet, card no.xx1345 on 30-06-2025 18:41:56 at KITCHEN AFF . Avl bal Rs.1824.65. Not you call 18002106919"
        val mockSms = SmsMessage(id = 7L, sender = "VD-PLUXEE", body = smsBody, date = System.currentTimeMillis())
        val result = SmsParser.parse(mockSms, emptyMappings)

        assertNotNull(result)
        assertEquals(60.00, result?.amount)
        assertEquals("expense", result?.transactionType)
        assertEquals("KITCHEN AFF", result?.merchantName)
        assertNotNull(result?.potentialAccount)
        assertEquals("Pluxee - xx1345", result?.potentialAccount?.formattedName)
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

    // --- NEW: Unit tests for the latest fixes ---

    @Test
    fun `test ignores invoice message`() {
        val smsBody = "An Invoice of Rs.330.8 for A4 Block-108 is raised. Pay at https://nbhd.co/NBHood/g/szBBpng. Ignore if paid - NoBrokerHood"
        val mockSms = SmsMessage(id = 9L, sender = "VM-NBHOOD", body = smsBody, date = System.currentTimeMillis())
        val result = SmsParser.parse(mockSms, emptyMappings)
        assertNull("Parser should ignore invoice messages", result)
    }

    @Test
    fun `test ignores successful payment confirmation`() {
        val smsBody = "Your payment of Rs.330.80 for A4-108 against Water Charges is successful. Regards NoBrokerHood"
        val mockSms = SmsMessage(id = 10L, sender = "VM-NBHOOD", body = smsBody, date = System.currentTimeMillis())
        val result = SmsParser.parse(mockSms, emptyMappings)
        assertNull("Parser should ignore successful payment confirmations", result)
    }

    @Test
    fun `test parses ICICI NEFT credit message`() {
        val smsBody = "ICICI Bank Account XX823 credited:Rs. 1,133.00 on 01-Jul-25. Info NEFT-HDFCN5202507024345356218-. Available Balance is Rs. 1,858.35."
        val mockSms = SmsMessage(id = 11L, sender = "VM-ICIBNK", body = smsBody, date = System.currentTimeMillis())
        val result = SmsParser.parse(mockSms, emptyMappings)

        assertNotNull(result)
        assertEquals(1133.00, result?.amount)
        assertEquals("income", result?.transactionType)
        assertEquals("NEFT-HDFCN5202507024345356218-", result?.merchantName)
        assertNotNull(result?.potentialAccount)
        assertEquals("ICICI Bank - xx823", result?.potentialAccount?.formattedName)
        assertEquals("Bank Account", result?.potentialAccount?.accountType)
    }

    @Test
    fun `test parses HDFC NEFT credit message`() {
        val smsBody = "HDFC Bank : NEFT money transfer Txn No HDFCN520253454560344 for Rs INR 1,500.00 has been credited to Manga Penga on 01-07-2025 at 08:05:30"
        val mockSms = SmsMessage(id = 12L, sender = "VM-HDFCBK", body = smsBody, date = System.currentTimeMillis())
        val result = SmsParser.parse(mockSms, emptyMappings)
        assertNull("Parser should ignore has been credited to messages", result)
    }
}
