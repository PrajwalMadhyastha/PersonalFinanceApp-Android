package com.example.personalfinanceapp

import android.content.Context
import android.provider.Telephony
import android.util.Log

/**
 * A repository class dedicated to handling data operations related to SMS messages.
 * This abstracts the logic of querying the Android ContentResolver away from ViewModels or Workers.
 */
class SmsRepository(private val context: Context) {
    /**
     * Fetches all SMS messages from the device's inbox.
     * @return A list of SmsMessage objects.
     */
    fun fetchAllSms(startDate: Long?): List<SmsMessage> {
        val smsList = mutableListOf<SmsMessage>()
        // Define the columns we want to retrieve
        val projection =
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
            )
        val selection: String?
        val selectionArgs: Array<String>?

        if (startDate != null) {
            selection = "${Telephony.Sms.DATE} >= ?"
            selectionArgs = arrayOf(startDate.toString())
            Log.d("SmsRepository", "Querying SMS with start date: $startDate")
        } else {
            selection = null
            selectionArgs = null
            Log.d("SmsRepository", "Querying all SMS messages.")
        }

        // Query the SMS inbox, sorting by date in descending order
        val cursor =
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                null,
                null,
                "date DESC",
            )

        cursor?.use {
            // Get column indices once for efficiency
            val idIndex = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (it.moveToNext()) {
                smsList.add(
                    SmsMessage(
                        id = it.getLong(idIndex),
                        sender = it.getString(addressIndex) ?: "Unknown",
                        body = it.getString(bodyIndex) ?: "",
                        date = it.getLong(dateIndex),
                    ),
                )
            }
        }
        return smsList
    }
}
