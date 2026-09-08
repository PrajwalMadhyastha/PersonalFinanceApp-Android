package io.pm.finlight

interface ISmsRepository {
    suspend fun fetchAllSms(startDate: Long? = null): List<SmsMessage>

    suspend fun getSmsDetailsById(lookupValue: Long): SmsMessage?
}
