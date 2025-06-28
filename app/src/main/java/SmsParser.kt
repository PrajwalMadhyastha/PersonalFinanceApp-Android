package io.pm.finlight

import android.util.Log

data class PotentialAccount(
    val formattedName: String, // e.g., "SBI - xx3201"
    val accountType: String,   // e.g., "Credit Card"
)

object SmsParser {
    private const val TAG = "SmsParser"
    private val AMOUNT_REGEX = "(?:rs|inr|rs\\.?)\\s*([\\d,]+\\.?\\d*)".toRegex(RegexOption.IGNORE_CASE)
    private val EXPENSE_KEYWORDS_REGEX = "\\b(spent|debited|paid|charged|payment of|purchase of)\\b".toRegex(RegexOption.IGNORE_CASE)
    private val INCOME_KEYWORDS_REGEX = "\\b(credited|received|deposited|refund of)\\b".toRegex(RegexOption.IGNORE_CASE)

    private val ACCOUNT_PATTERNS =
        listOf(
            "(?:on your|on)\\s+([A-Za-z0-9\\s]+?)\\s+((?:Credit|Meal|Savings)?\\s*(?:Card|Account|Acct))\\s+(?:ending with|ending in|xx)?\\s*(\\d{3,4})".toRegex(RegexOption.IGNORE_CASE),
            "on\\s+([A-Za-z0-9\\s]+?)\\s+(card|account|acct|a/c)\\s+(\\d{3,4})".toRegex(RegexOption.IGNORE_CASE),
            "(?:acct|account|a/c)\\s+xx(\\d{3,4})".toRegex(RegexOption.IGNORE_CASE),
            "card no\\.\\s*xx(\\d{3,4})".toRegex(RegexOption.IGNORE_CASE)
        )

    private val MERCHANT_REGEX_PATTERNS =
        listOf(
            "(?:credited|received).*from\\s+([A-Za-z0-9\\s.&'-]+?)(?:\\.|$)".toRegex(RegexOption.IGNORE_CASE),
            "at\\s*\\.\\.\\s*([A-Za-z0-9_\\s]+)\\s*on".toRegex(RegexOption.IGNORE_CASE),
            ";\\s*([A-Za-z0-9\\s.&'-]+?)\\s*credited".toRegex(RegexOption.IGNORE_CASE),
            "UPI.*(?:to|\\bat\\b)\\s+([A-Za-z0-9\\s.&'()]+?)(?:\\s+on|\\s+Ref|$)".toRegex(RegexOption.IGNORE_CASE),
            "to\\s+([a-zA-Z0-9.\\-_]+@[a-zA-Z0-9]+)".toRegex(RegexOption.IGNORE_CASE),
            "(?:\\bat\\b|to\\s+)([A-Za-z0-9\\s.&'-]+?)(?:\\s+on\\s+|\\s+for\\s+|\\.|$|\\s+was\\s+)".toRegex(RegexOption.IGNORE_CASE),
            "Info:\\s*([A-Za-z0-9\\s.&'-]+)".toRegex(RegexOption.IGNORE_CASE),
        )

    private fun parseAccount(smsBody: String, sender: String): PotentialAccount? {
        for (pattern in ACCOUNT_PATTERNS) {
            val match = pattern.find(smsBody)
            if (match != null && match.groupValues.size >= 2) {
                return when (match.groupValues.size) {
                    2 -> {
                        val number = match.groupValues[1]
                        val bankName = sender.split("-").getOrNull(1)?.replaceBefore("B", "")?.replace("BK", "Bank") ?: "Unknown Bank"
                        PotentialAccount(
                            formattedName = "$bankName - xx$number",
                            accountType = "Savings Account"
                        )
                    }
                    4 -> {
                        val bankName = match.groupValues[1].trim()
                        val type = match.groupValues[2].trim()
                        val number = match.groupValues[3]
                        PotentialAccount(
                            formattedName = "$bankName - xx$number",
                            accountType = type
                        )
                    }
                    else -> null
                }
            }
        }
        Log.d(TAG, "No specific account pattern matched for sender: $sender")
        return null
    }


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
                    val potentialName = match.groups[1]?.value?.replace("_", " ")?.replace(Regex("\\s+"), " ")?.trim()
                    if (!potentialName.isNullOrBlank() && !potentialName.contains("call", ignoreCase = true) && !potentialName.matches(Regex(".*\\d{6,}.*"))) {
                        merchantName = potentialName
                        break
                    }
                }
            }
        }

        val potentialAccount = parseAccount(messageBody, sms.sender)

        // --- FINAL FIX: Normalize both the sender and the body for a stable hash ---
        val normalizedSender = sms.sender.filter { it.isDigit() }.takeLast(10)
        val normalizedBody = sms.body.trim().replace(Regex("\\s+"), " ")
        val smsHash = (normalizedSender + normalizedBody).hashCode().toString()

        // --- DIAGNOSTIC LOGGING ---
        Log.d("DeDupeDebug", "--- PARSING ---")
        Log.d("DeDupeDebug", "Raw Sender: ${sms.sender} -> Normalized Sender: $normalizedSender")
        Log.d("DeDupeDebug", "Normalized Body: '$normalizedBody'")
        Log.d("DeDupeDebug", "Generated Hash: $smsHash")
        Log.d("DeDupeDebug", "---------------")


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
