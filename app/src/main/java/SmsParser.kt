package com.example.personalfinanceapp

import android.util.Log

object SmsParser {

    private const val TAG = "SmsParser"

    private val AMOUNT_REGEX = "(?:rs|inr|rs\\.?)\\s*([\\d,]+\\.?\\d*)".toRegex(RegexOption.IGNORE_CASE)
    private val EXPENSE_KEYWORDS_REGEX = "\\b(spent|debited|paid|charged|payment of|purchase of)\\b".toRegex(RegexOption.IGNORE_CASE)
    private val INCOME_KEYWORDS_REGEX = "\\b(credited|received|deposited|refund of)\\b".toRegex(RegexOption.IGNORE_CASE)

    private val MERCHANT_REGEX_PATTERNS = listOf(
        // High-priority: Look for a clear merchant name after a semicolon, before "credited"
        ";\\s*([A-Za-z0-9\\s.&'-]+?)\\s*credited".toRegex(RegexOption.IGNORE_CASE),
        // High-priority: Look for "at ..MERCHANT.. on" pattern from HDFC
        "at\\s*\\.\\.\\s*([A-Za-z0-9_\\s]+?)\\s*on".toRegex(RegexOption.IGNORE_CASE),
        // UPI transactions
        "UPI.*?(?:to|\\bat\\b)\\s+([A-Za-z0-9\\s.&'()]+?)(?:\\s+on|\\s+Ref|$)".toRegex(RegexOption.IGNORE_CASE),
        // Virtual Payment Address (VPA)
        "to\\s+([a-zA-Z0-9.\\-_]+@[a-zA-Z0-9]+)".toRegex(RegexOption.IGNORE_CASE),
        // Generic "at" or "to" a merchant
        "(?:\\bat\\b|to\\s+)([A-Za-z0-9\\s.&'-]+?)(?:\\s+on\\s+|\\s+for\\s+|\\.|$|\\s+was\\s+)".toRegex(RegexOption.IGNORE_CASE),
        // Merchant name after "Info:"
        "Info:\\s*([A-Za-z0-9\\s.&'-]+)".toRegex(RegexOption.IGNORE_CASE)
    )

    fun parse(sms: SmsMessage, mappings: Map<String, String>): PotentialTransaction? {
        val messageBody = sms.body
        Log.d(TAG, "--- Parsing SMS from ${sms.sender} ---")
        Log.d(TAG, "Body: $messageBody")

        val amountMatch = AMOUNT_REGEX.find(messageBody)
        val amount = amountMatch?.groups?.get(1)?.value?.replace(",", "")?.toDoubleOrNull()
        if (amount == null) {
            Log.d(TAG, "Result: FAILED (No amount found)")
            return null
        }
        Log.d(TAG, "Amount found: $amount")

        val transactionType = when {
            EXPENSE_KEYWORDS_REGEX.containsMatchIn(messageBody) -> "expense"
            INCOME_KEYWORDS_REGEX.containsMatchIn(messageBody) -> "income"
            else -> {
                Log.d(TAG, "Result: FAILED (No keywords found)")
                return null
            }
        }
        Log.d(TAG, "Type detected: $transactionType")

        var merchantName = mappings[sms.sender]
        if (merchantName != null) {
            Log.d(TAG, "Merchant found from user mapping: $merchantName")
        } else {
            Log.d(TAG, "No mapping found. Trying Regex patterns...")
            for ((index, pattern) in MERCHANT_REGEX_PATTERNS.withIndex()) {
                val match = pattern.find(messageBody)
                if (match != null) {
                    val potentialName = match.groups[1]?.value?.trim()?.replace("_", " ")?.replace(Regex("\\s+"), " ")
                    // Basic filter to avoid matching things that are clearly not merchant names
                    if (!potentialName.isNullOrBlank() && !potentialName.contains("call", ignoreCase = true) && !potentialName.matches(Regex(".*\\d{6,}.*"))) {
                        merchantName = potentialName
                        Log.d(TAG, "Pattern #$index matched! Merchant guess: $merchantName")
                        break
                    }
                }
            }
        }

        if(merchantName == null) {
            Log.w(TAG, "Could not determine merchant name from any pattern.")
        }

        val result = PotentialTransaction(
            sourceSmsId = sms.id, // Using the unique ID passed in
            smsSender = sms.sender,
            amount = amount,
            transactionType = transactionType,
            merchantName = merchantName,
            originalMessage = messageBody
        )
        Log.d(TAG, "Result: SUCCESS. Parsed as: $result")
        return result
    }
}
