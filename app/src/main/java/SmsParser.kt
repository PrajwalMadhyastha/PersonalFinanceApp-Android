package io.pm.finlight

// Removed android.util.Log import as it's not needed and causes test failures.

data class PotentialAccount(
    val formattedName: String, // e.g., "SBI - xx3201"
    val accountType: String,   // e.g., "Credit Card"
)

object SmsParser {
    // private const val TAG = "SmsParser" // No longer needed
    private val AMOUNT_REGEX = "(?:rs|inr|rs\\.?)\\s*([\\d,]+\\.?\\d*)".toRegex(RegexOption.IGNORE_CASE)
    private val EXPENSE_KEYWORDS_REGEX = "\\b(spent|debited|paid|charged|payment of|purchase of)\\b".toRegex(RegexOption.IGNORE_CASE)
    private val INCOME_KEYWORDS_REGEX = "\\b(credited|received|deposited|refund of)\\b".toRegex(RegexOption.IGNORE_CASE)

    // --- FIX: Added "has been credited to" to ignore informational NEFT messages ---
    private val NEGATIVE_KEYWORDS_REGEX = "\\b(invoice of|payment of.*is successful|has been credited to)\\b".toRegex(RegexOption.IGNORE_CASE)


    // --- REFINED: Reordered patterns and made them more specific ---
    private val ACCOUNT_PATTERNS =
        listOf(
            // --- FIX: Added new, more specific patterns for ICICI and HDFC ---
            "(ICICI Bank) Account XX(\\d{3,4}) credited".toRegex(RegexOption.IGNORE_CASE),
            "(HDFC Bank) : NEFT money transfer".toRegex(RegexOption.IGNORE_CASE),
            "spent from (Pluxee)\\s*(Meal Card wallet), card no\\.\\s*xx(\\d{4})".toRegex(RegexOption.IGNORE_CASE),
            "on your (SBI) (Credit Card) ending with (\\d{4})".toRegex(RegexOption.IGNORE_CASE),
            "On (HDFC Bank) (Card) (\\d{4})".toRegex(RegexOption.IGNORE_CASE),
            "(ICICI Bank) Acct XX(\\d{3,4}) debited".toRegex(RegexOption.IGNORE_CASE),
            "Acct XX(\\d{3,4}) is credited.*-(ICICI Bank)".toRegex(RegexOption.IGNORE_CASE)
        )

    private val MERCHANT_REGEX_PATTERNS =
        listOf(
            "(?:credited|received).*from\\s+([A-Za-z0-9\\s.&'-]+?)(?:\\.|$)".toRegex(RegexOption.IGNORE_CASE),
            "at\\s*\\.\\.\\s*([A-Za-z0-9_\\s]+)\\s*on".toRegex(RegexOption.IGNORE_CASE),
            ";\\s*([A-Za-z0-9\\s.&'-]+?)\\s*credited".toRegex(RegexOption.IGNORE_CASE),
            "UPI.*(?:to|\\bat\\b)\\s+([A-Za-z0-9\\s.&'()]+?)(?:\\s+on|\\s+Ref|$)".toRegex(RegexOption.IGNORE_CASE),
            "to\\s+([a-zA-Z0-9.\\-_]+@[a-zA-Z0-9]+)".toRegex(RegexOption.IGNORE_CASE),
            "(?:\\bat\\b|to\\s+)([A-Za-z0-9\\s.&'-]+?)(?:\\s+on\\s+|\\s+for\\s+|\\.|$|\\s+was\\s+)".toRegex(RegexOption.IGNORE_CASE),
            // --- FIX: Made the colon optional and the capture non-greedy to correctly parse NEFT info ---
            "Info:?\\s*([A-Za-z0-9\\s.&'-]+?)(?:\\.|$)".toRegex(RegexOption.IGNORE_CASE)
        )

    private fun parseAccount(smsBody: String, sender: String): PotentialAccount? {
        for (pattern in ACCOUNT_PATTERNS) {
            val match = pattern.find(smsBody)
            if (match != null) {
                val pString = pattern.pattern
                // --- REFINED: Use pattern string to determine logic, which is more robust ---
                return when {
                    // --- FIX: Handle new ICICI credit pattern ---
                    pString.startsWith("(ICICI Bank) Account") -> {
                        val bank = match.groupValues[1].trim()
                        val number = match.groupValues[2].trim()
                        PotentialAccount(formattedName = "$bank - xx$number", accountType = "Bank Account")
                    }
                    // --- FIX: Handle new HDFC NEFT pattern ---
                    pString.startsWith("(HDFC Bank) : NEFT") -> {
                        val bank = match.groupValues[1].trim()
                        PotentialAccount(formattedName = bank, accountType = "Bank Account")
                    }
                    pString.startsWith("spent from") -> {
                        val group1 = match.groupValues[1].trim() // Brand (Pluxee)
                        val group2 = match.groupValues[2].trim() // Type (Meal Card wallet)
                        val number = match.groupValues[3].trim() // Number
                        PotentialAccount(formattedName = "$group1 - xx$number", accountType = group2)
                    }
                    // Handles SBI, HDFC
                    pString.startsWith("on your") || pString.startsWith("On") -> {
                        val group1 = match.groupValues[1].trim() // Bank or brand
                        val group2 = match.groupValues[2].trim() // Type
                        val number = match.groupValues[3].trim() // Number
                        PotentialAccount(formattedName = "$group1 - xx$number", accountType = group2)
                    }
                    // Handles ICICI Debit
                    pString.contains("debited") -> {
                        val bank = match.groupValues[1].trim()
                        val number = match.groupValues[2].trim()
                        PotentialAccount(formattedName = "$bank - xx$number", accountType = "Savings Account")
                    }
                    // Handles ICICI Credit
                    pString.contains("is credited") -> {
                        val number = match.groupValues[1].trim()
                        val bank = match.groupValues[2].trim()
                        PotentialAccount(formattedName = "$bank - xx$number", accountType = "Savings Account")
                    }
                    else -> null
                }
            }
        }
        return null
    }

    fun parse(
        sms: SmsMessage,
        mappings: Map<String, String>,
    ): PotentialTransaction? {
        val messageBody = sms.body

        // --- FIX: Check for negative keywords first to reject non-transactional messages ---
        if (NEGATIVE_KEYWORDS_REGEX.containsMatchIn(messageBody)) {
            return null
        }

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
                    val potentialName = match.groups[1]?.value?.replace("_", " ")?.replace(Regex("\\s+"), " ")?.trim()
                    // --- FIX: Allow NEFT references to pass the digit filter ---
                    if (!potentialName.isNullOrBlank() && !potentialName.contains("call", ignoreCase = true)) {
                        // If the name starts with NEFT, it's a valid transaction reference.
                        // Otherwise, apply the digit check to filter out phone numbers.
                        if (potentialName.startsWith("NEFT", ignoreCase = true) || !potentialName.matches(Regex(".*\\d{6,}.*"))) {
                            merchantName = potentialName
                            break
                        }
                    }
                }
            }
        }

        val potentialAccount = parseAccount(messageBody, sms.sender)

        val normalizedSender = sms.sender.filter { it.isDigit() }.takeLast(10)
        val normalizedBody = sms.body.trim().replace(Regex("\\s+"), " ")
        val smsHash = (normalizedSender + normalizedBody).hashCode().toString()

        return PotentialTransaction(
            sourceSmsId = sms.id,
            smsSender = sms.sender,
            amount = amount,
            transactionType = transactionType,
            merchantName = merchantName,
            originalMessage = messageBody,
            potentialAccount = potentialAccount,
            sourceSmsHash = smsHash
        )
    }
}
