package io.pm.finlight

import kotlinx.coroutines.flow.Flow

class MerchantRenameRuleRepository(private val dao: MerchantRenameRuleDao) {
    fun getAllRules(): Flow<List<MerchantRenameRule>> = dao.getAllRules()
    suspend fun insert(rule: MerchantRenameRule) = dao.insert(rule)
}
