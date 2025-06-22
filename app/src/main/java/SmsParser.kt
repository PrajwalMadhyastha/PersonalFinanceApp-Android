package com.example.personalfinanceapp

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

    // --- NEW: A prioritized list of regular expressions for finding the merchant ---
    private val MERCHANT_REGEX_PATTERNS = listOf(
        // Pattern 1: Look for transactions made via UPI, often includes "UPI:"
        "UPI.*(?:to|\\bat\\b)\\s+([A-Za-z0-9\\s.&'()]+?)(?:\\s+on|\\s+Ref|$)".toRegex(RegexOption.IGNORE_CASE),
        // Pattern 2: Standard pattern for "at" or "to" a merchant, ending with a clear stop word.
        "(?:\\bat\\b|to\\s+)([A-Za-z0-9\\s.&'-]+?)(?:\\s+on\\s+|\\s+for\\s+|\\.|$|\\s+was\\s+)".toRegex(RegexOption.IGNORE_CASE),
        // Pattern 3: Look for merchant names that are often in ALL CAPS, especially after the amount.
        "rs\\s*[\\d,.]+\\s(?:debited|spent at|to)\\s+([A-Z\\s&]+)(?:\\s+on|$)".toRegex(RegexOption.IGNORE_CASE),
        // Pattern 4: Look for VPA (Virtual Payment Address) like 'someone@bank'
        "to\\s+([a-zA-Z0-9.\\-_]+@[a-zA-Z0-9]+)".toRegex(RegexOption.IGNORE_CASE),
        // Pattern 5: A more generic catch-all that looks for capitalized words after an expense keyword.
        "(?:spent|paid) at ([A-Z][A-Za-z\\s]+)(?:\\s+on|$)".toRegex(RegexOption.IGNORE_CASE),
        // Pattern 6: Look for merchant name after "Info:"
        "Info:\\s*([A-Za-z0-9\\s.&'-]+)".toRegex(RegexOption.IGNORE_CASE)
    )

    /**
     * Parses the body of an SMS message to find transaction details.
     *
     * @param messageBody The full text of the SMS message.
     * @return A [PotentialTransaction] object if details are found, otherwise null.
     */
    fun parse(messageBody: String): PotentialTransaction? {
        val amountMatch = AMOUNT_REGEX.find(messageBody)
        val amount = amountMatch?.groups?.get(1)?.value?.replace(",", "")?.toDoubleOrNull()
            ?: return null

        val transactionType = when {
            EXPENSE_KEYWORDS_REGEX.containsMatchIn(messageBody) -> "expense"
            INCOME_KEYWORDS_REGEX.containsMatchIn(messageBody) -> "income"
            else -> return null
        }

        // --- UPDATED: Iterate through the patterns to find the best merchant match ---
        var merchantName: String? = null
        for (pattern in MERCHANT_REGEX_PATTERNS) {
            val match = pattern.find(messageBody)
            if (match != null) {
                merchantName = match.groups[1]?.value?.trim()
                    ?.replace(Regex("\\s+"), " ") // Consolidate whitespace
                    ?.takeIf { it.isNotBlank() && !it.matches(Regex(".*\\d{4,}.*")) } // Ignore if it contains long numbers
                if (merchantName != null) break // Found a good match, stop searching
            }
        }

        // Final fallback if no specific merchant is found, check for "credited" context
        if(merchantName == null && transactionType == "expense") {
            val creditedMatch = ";\\s*(.*?)\\s*credited".toRegex(RegexOption.IGNORE_CASE).find(messageBody)
            if(creditedMatch != null){
                merchantName = creditedMatch.groups[1]?.value?.trim()
            }
        }

        return PotentialTransaction(
            amount = amount,
            transactionType = transactionType,
            merchantName = merchantName,
            originalMessage = messageBody
        )
    }
}
