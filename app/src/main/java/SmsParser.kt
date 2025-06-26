package io.pm.finlight

object SmsParser {
    private val AMOUNT_REGEX = "(?:rs|inr|rs\\.?)\\s*([\\d,]+\\.?\\d*)".toRegex(RegexOption.IGNORE_CASE)
    private val EXPENSE_KEYWORDS_REGEX = "\\b(spent|debited|paid|charged|payment of|purchase of)\\b".toRegex(RegexOption.IGNORE_CASE)
    private val INCOME_KEYWORDS_REGEX = "\\b(credited|received|deposited|refund of)\\b".toRegex(RegexOption.IGNORE_CASE)

    // --- UPDATED: More robust and better-prioritized list of patterns ---
    private val MERCHANT_REGEX_PATTERNS =
        listOf(
            // Priority 1: Look for income transactions from a sender.
            "(?:credited|received).*from\\s+([A-Za-z0-9\\s.&'-]+?)(?:\\.|$)".toRegex(RegexOption.IGNORE_CASE),
            // Priority 2: Specific HDFC pattern "at ..MERCHANT.. on" - made greedy to capture full name.
            "at\\s*\\.\\.\\s*([A-Za-z0-9_\\s]+)\\s*on".toRegex(RegexOption.IGNORE_CASE),
            // Priority 3: Specific pattern for "DAKSHIN CAFE credited"
            ";\\s*([A-Za-z0-9\\s.&'-]+?)\\s*credited".toRegex(RegexOption.IGNORE_CASE),
            // Priority 4: UPI transactions
            "UPI.*(?:to|\\bat\\b)\\s+([A-Za-z0-9\\s.&'()]+?)(?:\\s+on|\\s+Ref|$)".toRegex(RegexOption.IGNORE_CASE),
            // Priority 5: Virtual Payment Address (VPA)
            "to\\s+([a-zA-Z0-9.\\-_]+@[a-zA-Z0-9]+)".toRegex(RegexOption.IGNORE_CASE),
            // Priority 6: Generic "at" or "to" a merchant
            "(?:\\bat\\b|to\\s+)([A-Za-z0-9\\s.&'-]+?)(?:\\s+on\\s+|\\s+for\\s+|\\.|$|\\s+was\\s+)".toRegex(RegexOption.IGNORE_CASE),
            // Priority 7: Merchant name after "Info:"
            "Info:\\s*([A-Za-z0-9\\s.&'-]+)".toRegex(RegexOption.IGNORE_CASE),
        )

    fun parse(
        sms: SmsMessage,
        mappings: Map<String, String>,
    ): PotentialTransaction? {
        val messageBody = sms.body

        val amountMatch = AMOUNT_REGEX.find(messageBody)
        val amount =
            amountMatch?.groups?.get(1)?.value?.replace(",", "")?.toDoubleOrNull()
                ?: return null

        val transactionType =
            when {
                EXPENSE_KEYWORDS_REGEX.containsMatchIn(messageBody) -> "expense"
                INCOME_KEYWORDS_REGEX.containsMatchIn(messageBody) -> "income"
                else -> return null
            }

        var merchantName = mappings[sms.sender]

        if (merchantName == null) {
            for (pattern in MERCHANT_REGEX_PATTERNS) {
                val match = pattern.find(messageBody)
                if (match != null) {
                    // --- CORRECTED: Added a final .trim() to remove trailing spaces ---
                    val potentialName = match.groups[1]?.value?.replace("_", " ")?.replace(Regex("\\s+"), " ")?.trim()
                    if (!potentialName.isNullOrBlank() && !potentialName.contains("call", ignoreCase = true) && !potentialName.matches(Regex(".*\\d{6,}.*"))) {
                        merchantName = potentialName
                        break
                    }
                }
            }
        }

        return PotentialTransaction(
            sourceSmsId = sms.id,
            smsSender = sms.sender,
            amount = amount,
            transactionType = transactionType,
            merchantName = merchantName,
            originalMessage = messageBody,
        )
    }
}
