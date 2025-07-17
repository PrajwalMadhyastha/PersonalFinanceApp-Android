// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/SmsParser.kt
// REASON: FEATURE - The parser now generates a "signature" for each SMS. This
// is done by removing volatile data (amounts, dates, times, ref numbers) to
// create a stable template of the message. This signature is the key to the new
// proactive recurring transaction detection feature. It is now included in the
// returned PotentialTransaction object.
// =================================================================================
package io.pm.finlight

import android.util.Log
import kotlinx.coroutines.flow.first
import java.util.regex.PatternSyntaxException

data class PotentialAccount(
    val formattedName: String,
    val accountType: String,
)

object SmsParser {
    private val CURRENCY_AMOUNT_REGEX = "(?:rs|inr|rs\\.?)\\s*([\\d,]+\\.?\\d*)".toRegex(RegexOption.IGNORE_CASE)
    private val KEYWORD_AMOUNT_REGEX = "(?:purchase of|payment of|spent|charged|credited with|debited for|credit of|for)\\s+([\\d,]+\\.?\\d*)".toRegex(RegexOption.IGNORE_CASE)
    private val EXPENSE_KEYWORDS_REGEX = "\\b(spent|debited|paid|charged|payment of|purchase of)\\b".toRegex(RegexOption.IGNORE_CASE)
    private val INCOME_KEYWORDS_REGEX = "\\b(credited|received|deposited|refund of)\\b".toRegex(RegexOption.IGNORE_CASE)
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

    // --- NEW: Regex to find and remove volatile parts of an SMS for signature generation ---
    private val VOLATILE_DATA_REGEX = listOf(
        "\\b(?:rs|inr)[\\s.]*\\d[\\d,.]*".toRegex(RegexOption.IGNORE_CASE), // Amounts (e.g., Rs. 1,234.56)
        "\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}".toRegex(), // Dates (e.g., 31-12-2024)
        "\\d{1,2}-\\w{3}-\\d{2,4}".toRegex(RegexOption.IGNORE_CASE), // Dates (e.g., 31-Dec-2024)
        "\\d{2,}:\\d{2,}(?::\\d{2,})?".toRegex(), // Times (e.g., 14:30:55)
        "\\b(?:ref no|txn id|upi ref|transaction id|ref id)\\s*[:.]?\\s*\\w*\\d+\\w*".toRegex(RegexOption.IGNORE_CASE), // Ref numbers
        "a/c no\\. \\S+".toRegex(RegexOption.IGNORE_CASE), // A/c numbers
        "avl bal[:]?[\\s.]*rs[\\s.]*\\d[\\d,.]*".toRegex(RegexOption.IGNORE_CASE), // Available balance
        "\\b\\d{4,}\\b".toRegex() // Any number with 4 or more digits (likely IDs, etc.)
    )

    /**
     * Generates a stable "signature" from an SMS body by removing volatile data.
     */
    private fun generateSmsSignature(body: String): String {
        var signature = body.lowercase()
        VOLATILE_DATA_REGEX.forEach { regex ->
            signature = regex.replace(signature, "")
        }
        // Normalize whitespace and hash the result for a clean, unique key
        return signature.replace(Regex("\\s+"), " ").trim().hashCode().toString()
    }


    private fun parseAccount(smsBody: String, sender: String): PotentialAccount? {
        for (pattern in ACCOUNT_PATTERNS) {
            val match = pattern.find(smsBody)
            if (match != null) {
                return when (pattern.pattern) {
                    "(ICICI Bank) Account XX(\\d{3,4}) credited" ->
                        PotentialAccount(formattedName = "${match.groupValues[1].trim()} - xx${match.groupValues[2].trim()}", accountType = "Bank Account")
                    "(HDFC Bank) : NEFT money transfer" ->
                        PotentialAccount(formattedName = match.groupValues[1].trim(), accountType = "Bank Account")
                    "spent from (Pluxee)\\s*(Meal Card wallet), card no\\.\\s*xx(\\d{4})" ->
                        PotentialAccount(formattedName = "${match.groupValues[1].trim()} - xx${match.groupValues[3].trim()}", accountType = match.groupValues[2].trim())
                    "on your (SBI) (Credit Card) ending with (\\d{4})" ->
                        PotentialAccount(formattedName = "${match.groupValues[1].trim()} - xx${match.groupValues[3].trim()}", accountType = match.groupValues[2].trim())
                    "On (HDFC Bank) (Card) (\\d{4})" ->
                        PotentialAccount(formattedName = "${match.groupValues[1].trim()} - xx${match.groupValues[3].trim()}", accountType = match.groupValues[2].trim())
                    "(ICICI Bank) Acct XX(\\d{3,4}) debited" ->
                        PotentialAccount(formattedName = "${match.groupValues[1].trim()} - xx${match.groupValues[2].trim()}", accountType = "Savings Account")
                    "Acct XX(\\d{3,4}) is credited.*-(ICICI Bank)" ->
                        PotentialAccount(formattedName = "${match.groupValues[2].trim()} - xx${match.groupValues[1].trim()}", accountType = "Savings Account")
                    else -> null
                }
            }
        }
        return null
    }

    suspend fun parse(
        sms: SmsMessage,
        mappings: Map<String, String>,
        customSmsRuleDao: CustomSmsRuleDao,
        merchantRenameRuleDao: MerchantRenameRuleDao,
        ignoreRuleDao: IgnoreRuleDao,
        merchantCategoryMappingDao: MerchantCategoryMappingDao
    ): PotentialTransaction? {
        val messageBody = sms.body
        Log.d("SmsParser", "--- Parsing SMS from: ${sms.sender} ---")

        val ignorePhrases = ignoreRuleDao.getEnabledPhrases()
        for (phrase in ignorePhrases) {
            try {
                if (phrase.toRegex(RegexOption.IGNORE_CASE).containsMatchIn(messageBody)) {
                    Log.d("SmsParser", "Message contains ignore phrase '$phrase'. Ignoring.")
                    return null
                }
            } catch (e: PatternSyntaxException) {
                Log.e("SmsParser", "Invalid regex pattern in ignore phrase: '$phrase'", e)
            }
        }

        var extractedMerchant: String? = null
        var extractedAmount: Double? = null
        var extractedAccount: PotentialAccount? = null

        val allRules = customSmsRuleDao.getAllRules().first()
        val renameRules = merchantRenameRuleDao.getAllRules().first().associateBy({ it.originalName }, { it.newName })
        Log.d("SmsParser", "Found ${allRules.size} custom rules and ${renameRules.size} rename rules.")


        for (rule in allRules) {
            if (messageBody.contains(rule.triggerPhrase, ignoreCase = true)) {
                Log.d("SmsParser", "SUCCESS: Found matching trigger phrase '${rule.triggerPhrase}' for rule ID ${rule.id}.")

                rule.merchantRegex?.let { regexStr ->
                    try {
                        val match = regexStr.toRegex().find(messageBody)
                        if (match != null && match.groupValues.size > 1) {
                            extractedMerchant = match.groupValues[1].trim()
                        }
                    } catch (e: PatternSyntaxException) { /* Ignore */ }
                }

                rule.amountRegex?.let { regexStr ->
                    try {
                        val match = regexStr.toRegex().find(messageBody)
                        if (match != null && match.groupValues.size > 1) {
                            extractedAmount = match.groupValues[1].replace(",", "").toDoubleOrNull()
                        }
                    } catch (e: PatternSyntaxException) { /* Ignore */ }
                }

                rule.accountRegex?.let { regexStr ->
                    try {
                        val match = regexStr.toRegex().find(messageBody)
                        if (match != null && match.groupValues.size > 1) {
                            val accountName = match.groupValues[1].trim()
                            extractedAccount = PotentialAccount(formattedName = accountName, accountType = "Custom")
                            Log.d("SmsParser", "Extracted Account: '$accountName' using custom rule.")
                        }
                    } catch (e: PatternSyntaxException) {
                        Log.e("SmsParser", "Invalid account regex for rule ID ${rule.id}", e)
                    }
                }
                break
            }
        }

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

        if (merchantName != null && renameRules.containsKey(merchantName)) {
            val originalName = merchantName
            merchantName = renameRules[merchantName]
            Log.d("SmsParser", "Applied rename rule: '$originalName' -> '$merchantName'")
        }

        var learnedCategoryId: Int? = null
        if (merchantName != null) {
            learnedCategoryId = merchantCategoryMappingDao.getCategoryIdForMerchant(merchantName)
            if (learnedCategoryId != null) {
                Log.d("SmsParser", "Found learned category ID $learnedCategoryId for merchant '$merchantName'")
            }
        }

        val potentialAccount = extractedAccount ?: parseAccount(messageBody, sms.sender)
        val normalizedSender = sms.sender.filter { it.isDigit() }.takeLast(10)
        val normalizedBody = sms.body.trim().replace(Regex("\\s+"), " ")
        val smsHash = (normalizedSender + normalizedBody).hashCode().toString()
        // --- NEW: Generate and include the signature ---
        val smsSignature = generateSmsSignature(sms.body)


        return PotentialTransaction(
            sourceSmsId = sms.id,
            smsSender = sms.sender,
            amount = amount,
            transactionType = transactionType,
            merchantName = merchantName,
            originalMessage = messageBody,
            potentialAccount = potentialAccount,
            sourceSmsHash = smsHash,
            categoryId = learnedCategoryId,
            smsSignature = smsSignature // --- NEW
        )
    }
}
