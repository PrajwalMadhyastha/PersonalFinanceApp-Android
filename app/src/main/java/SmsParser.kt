package com.example.personalfinanceapp

import java.util.regex.Pattern

/**
 * A utility object for parsing financial information from SMS message bodies.
 * It uses a series of regular expressions to identify and extract transaction details.
 */
object SmsParser {

    // Regex to find amounts, handling formats like Rs.1,234.56, INR 50.00, Rs 500
    private val AMOUNT_REGEX = "(?:rs|inr|rs\\.?)\\s*([\\d,]+\\.?\\d*)".toRegex(RegexOption.IGNORE_CASE)

    // Regex to find keywords indicating an expense
    private val EXPENSE_KEYWORDS_REGEX = "\\b(spent|debited|paid|charged|payment of|purchase of)\\b".toRegex(RegexOption.IGNORE_CASE)

    // Regex to find keywords indicating income
    private val INCOME_KEYWORDS_REGEX = "\\b(credited|received|deposited)\\b".toRegex(RegexOption.IGNORE_CASE)

    // Regex to find potential merchant names, often following keywords like 'at' or 'to'
    private val MERCHANT_REGEX = "(?:at|to|on|from)\\s+([A-Za-z0-9\\s&.'-]+?)(?:\\s+on|\\s+for|\\s+with|\\s*$)".toRegex(RegexOption.IGNORE_CASE)


    /**
     * Parses the body of an SMS message to find transaction details.
     *
     * @param messageBody The full text of the SMS message.
     * @return A [PotentialTransaction] object if details are found, otherwise null.
     */
    fun parse(messageBody: String): PotentialTransaction? {
        // 1. Find the transaction amount
        val amountMatch = AMOUNT_REGEX.find(messageBody)
        val amount = amountMatch?.groups?.get(1)?.value?.replace(",", "")?.toDoubleOrNull()
            ?: return null // If no amount is found, it's not a transaction we can parse.

        // 2. Determine the transaction type (income or expense)
        val transactionType = when {
            EXPENSE_KEYWORDS_REGEX.containsMatchIn(messageBody) -> "expense"
            INCOME_KEYWORDS_REGEX.containsMatchIn(messageBody) -> "income"
            else -> return null // If no keywords are found, we can't determine the type.
        }

        // 3. Try to find the merchant name
        val merchantMatch = MERCHANT_REGEX.find(messageBody)
        // Clean up the matched merchant name
        val merchantName = merchantMatch?.groups?.get(1)?.value?.trim()
            ?.replace(Regex("\\s+"), " ") // Replace multiple spaces with a single space
            ?.takeIf { it.isNotBlank() }

        return PotentialTransaction(
            amount = amount,
            transactionType = transactionType,
            merchantName = merchantName,
            originalMessage = messageBody
        )
    }
}
