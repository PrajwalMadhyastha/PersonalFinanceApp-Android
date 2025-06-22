package com.example.personalfinanceapp

object SmsParser {

    private val AMOUNT_REGEX = "(?:rs|inr|rs\\.?)\\s*([\\d,]+\\.?\\d*)".toRegex(RegexOption.IGNORE_CASE)
    private val EXPENSE_KEYWORDS_REGEX = "\\b(spent|debited|paid|charged|payment of|purchase of)\\b".toRegex(RegexOption.IGNORE_CASE)
    private val INCOME_KEYWORDS_REGEX = "\\b(credited|received|deposited)\\b".toRegex(RegexOption.IGNORE_CASE)

    private val MERCHANT_REGEX_PATTERNS = listOf(
        "at\\s*\\.\\.\\s*([A-Za-z0-9_\\s]+?)\\s*on".toRegex(RegexOption.IGNORE_CASE),
        "UPI.*(?:to|\\bat\\b)\\s+([A-Za-z0-9\\s.&'()]+?)(?:\\s+on|\\s+Ref|$)".toRegex(RegexOption.IGNORE_CASE),
        "(?:\\bat\\b|to\\s+)([A-Za-z0-9\\s.&'-]+?)(?:\\s+on\\s+|\\s+for\\s+|\\.|$|\\s+was\\s+)".toRegex(RegexOption.IGNORE_CASE),
        "to\\s+([a-zA-Z0-9.\\-_]+@[a-zA-Z0-9]+)".toRegex(RegexOption.IGNORE_CASE),
        "Info:\\s*([A-Za-z0-9\\s.&'-]+)".toRegex(RegexOption.IGNORE_CASE)
    )

    /**
     * UPDATED: The parse function now uses the app's "memory" (merchant mappings)
     * to override Regex parsing when possible.
     */
    fun parse(sms: SmsMessage, mappings: Map<String, String>): PotentialTransaction? {
        val messageBody = sms.body
        val amountMatch = AMOUNT_REGEX.find(messageBody)
        val amount = amountMatch?.groups?.get(1)?.value?.replace(",", "")?.toDoubleOrNull()
            ?: return null

        val transactionType = when {
            EXPENSE_KEYWORDS_REGEX.containsMatchIn(messageBody) -> "expense"
            INCOME_KEYWORDS_REGEX.containsMatchIn(messageBody) -> "income"
            else -> return null
        }

        // --- NEW: Self-Learning Logic ---
        // 1. Check if a mapping already exists for this sender.
        var merchantName = mappings[sms.sender]

        // 2. If no mapping exists, fall back to Regex parsing.
        if (merchantName == null) {
            for (pattern in MERCHANT_REGEX_PATTERNS) {
                val match = pattern.find(messageBody)
                if (match != null) {
                    merchantName = match.groups[1]?.value?.trim()?.replace("_", " ")?.replace(Regex("\\s+"), " ")
                    if (merchantName != null) break
                }
            }
            if(merchantName == null && transactionType == "expense") {
                val creditedMatch = ";\\s*(.*?)\\s*credited".toRegex(RegexOption.IGNORE_CASE).find(messageBody)
                if(creditedMatch != null){
                    merchantName = creditedMatch.groups[1]?.value?.trim()
                }
            }
        }

        return PotentialTransaction(
            sourceSmsId = sms.id,
            smsSender = sms.sender, // Keep track of the sender
            amount = amount,
            transactionType = transactionType,
            merchantName = merchantName,
            originalMessage = messageBody
        )
    }
}
