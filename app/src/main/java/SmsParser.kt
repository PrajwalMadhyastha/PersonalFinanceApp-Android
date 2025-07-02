// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/SmsParser.kt
// REASON: ARCHITECTURAL REFACTOR - The parsing logic is completely overhauled.
// It no longer checks the sender. Instead, it fetches all custom rules and
// iterates through them, checking if a rule's 'triggerPhrase' is present in
// the SMS body. If a match is found, it applies the corresponding regex
// patterns from that rule to extract the data.
// =================================================================================
package io.pm.finlight

import android.util.Log
import kotlinx.coroutines.flow.first
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

data class PotentialAccount(
    val formattedName: String, // e.g., "SBI - xx3201"
    val accountType: String,   // e.g., "Credit Card"
)

object SmsParser {
    // This is a high-precision regex that looks for explicit currency symbols.
    private val CURRENCY_AMOUNT_REGEX = "(?:rs|inr|rs\\.?)\\s*([\\d,]+\\.?\\d*)".toRegex(RegexOption.IGNORE_CASE)
    // This is a fallback regex that looks for keywords often preceding an amount.
    private val KEYWORD_AMOUNT_REGEX = "(?:purchase of|payment of|spent|charged|credited with|debited for|credit of|for)\\s+([\\d,]+\\.?\\d*)".toRegex(RegexOption.IGNORE_CASE)

    private val EXPENSE_KEYWORDS_REGEX = "\\b(spent|debited|paid|charged|payment of|purchase of)\\b".toRegex(RegexOption.IGNORE_CASE)
    private val INCOME_KEYWORDS_REGEX = "\\b(credited|received|deposited|refund of)\\b".toRegex(RegexOption.IGNORE_CASE)
    private val NEGATIVE_KEYWORDS_REGEX = "\\b(invoice of|payment of.*is successful|has been credited to)\\b".toRegex(RegexOption.IGNORE_CASE)


    private val ACCOUNT_PATTERNS =
        listOf(
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
            "Info:?\\s*([A-Za-z0-9\\s.&'-]+?)(?:\\.|$)".toRegex(RegexOption.IGNORE_CASE)
        )

    private fun parseAccount(smsBody: String, sender: String): PotentialAccount? {
        for (pattern in ACCOUNT_PATTERNS) {
            val match = pattern.find(smsBody)
            if (match != null) {
                val pString = pattern.pattern
                return when {
                    pString.startsWith("(ICICI Bank) Account") -> PotentialAccount(formattedName = "${match.groupValues[1].trim()} - xx${match.groupValues[2].trim()}", accountType = "Bank Account")
                    pString.startsWith("(HDFC Bank) : NEFT") -> PotentialAccount(formattedName = match.groupValues[1].trim(), accountType = "Bank Account")
                    pString.startsWith("spent from") -> PotentialAccount(formattedName = "${match.groupValues[1].trim()} - xx${match.groupValues[3].trim()}", accountType = match.groupValues[2].trim())
                    pString.startsWith("on your") || pString.startsWith("On") -> PotentialAccount(formattedName = "${match.groupValues[1].trim()} - xx${match.groupValues[3].trim()}", accountType = match.groupValues[2].trim())
                    pString.contains("debited") -> PotentialAccount(formattedName = "${match.groupValues[1].trim()} - xx${match.groupValues[2].trim()}", accountType = "Savings Account")
                    pString.contains("is credited") -> PotentialAccount(formattedName = "${match.groupValues[2].trim()} - xx${match.groupValues[1].trim()}", accountType = "Savings Account")
                    else -> null
                }
            }
        }
        return null
    }

    suspend fun parse(
        sms: SmsMessage,
        mappings: Map<String, String>,
        customSmsRuleDao: CustomSmsRuleDao
    ): PotentialTransaction? {
        val messageBody = sms.body
        Log.d("SmsParser", "--- Parsing SMS from: ${sms.sender} ---")
        Log.d("SmsParser", "Body: $messageBody")

        if (NEGATIVE_KEYWORDS_REGEX.containsMatchIn(messageBody)) {
            Log.d("SmsParser", "Message contains negative keywords. Ignoring.")
            return null
        }

        var extractedMerchant: String? = null
        var extractedAmount: Double? = null

        // --- REWRITTEN LOGIC: Iterate through all trigger-based rules ---
        val allRules = customSmsRuleDao.getAllRules()
        Log.d("SmsParser", "Found ${allRules.size} total custom rules to check.")

        for (rule in allRules) {
            if (messageBody.contains(rule.triggerPhrase, ignoreCase = true)) {
                Log.d("SmsParser", "SUCCESS: Found matching trigger phrase '${rule.triggerPhrase}' for rule ID ${rule.id}.")

                // Trigger matched, now apply the specific regex patterns from this rule.
                rule.merchantRegex?.let { regexStr ->
                    try {
                        val match = regexStr.toRegex().find(messageBody)
                        if (match != null && match.groupValues.size > 1) {
                            extractedMerchant = match.groupValues[1].trim()
                            Log.d("SmsParser", "Extracted Merchant: '$extractedMerchant' using regex: $regexStr")
                        }
                    } catch (e: PatternSyntaxException) {
                        Log.e("SmsParser", "Invalid merchant regex for rule ID ${rule.id}", e)
                    }
                }

                rule.amountRegex?.let { regexStr ->
                    try {
                        val match = regexStr.toRegex().find(messageBody)
                        if (match != null && match.groupValues.size > 1) {
                            extractedAmount = match.groupValues[1].replace(",", "").toDoubleOrNull()
                            Log.d("SmsParser", "Extracted Amount: '$extractedAmount' using regex: $regexStr")
                        }
                    } catch (e: PatternSyntaxException) {
                        Log.e("SmsParser", "Invalid amount regex for rule ID ${rule.id}", e)
                    }
                }
                // Stop after the first matching rule is applied.
                break
            }
        }

        // Fallback to default parsing if custom rules didn't find anything.
        val amount = extractedAmount
            ?: CURRENCY_AMOUNT_REGEX.find(messageBody)?.groups?.get(1)?.value?.replace(",", "")?.toDoubleOrNull()
            ?: KEYWORD_AMOUNT_REGEX.find(messageBody)?.groups?.get(1)?.value?.replace(",", "")?.toDoubleOrNull()
            ?: return null

        val transactionType =
            when {
                EXPENSE_KEYWORDS_REGEX.containsMatchIn(messageBody) -> "expense"
                INCOME_KEYWORDS_REGEX.containsMatchIn(messageBody) -> "income"
                else -> return null
            }

        var merchantName = extractedMerchant ?: mappings[sms.sender]

        if (merchantName == null) {
            for (pattern in MERCHANT_REGEX_PATTERNS) {
                val match = pattern.find(messageBody)
                if (match != null) {
                    val potentialName = match.groups[1]?.value?.replace("_", " ")?.replace(Regex("\\s+"), " ")?.trim()
                    if (!potentialName.isNullOrBlank() && !potentialName.contains("call", ignoreCase = true)) {
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
