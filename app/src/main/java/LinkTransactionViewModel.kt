// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/LinkTransactionViewModel.kt
// REASON: NEW FILE - This ViewModel provides the logic for the new "Link
// Transaction" screen. It takes a PotentialTransaction, finds linkable
// existing transactions using the repository's smart query, and exposes them
// to the UI. It also contains the logic to perform the link by updating the
// chosen transaction with the SMS hash.
// =================================================================================
package io.pm.finlight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LinkTransactionViewModelFactory(
    private val application: Application,
    private val potentialTransaction: PotentialTransaction
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LinkTransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LinkTransactionViewModel(application, potentialTransaction) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class LinkTransactionViewModel(
    application: Application,
    private val potentialTransaction: PotentialTransaction
) : AndroidViewModel(application) {

    private val transactionRepository: TransactionRepository = TransactionRepository(AppDatabase.getInstance(application).transactionDao())

    private val _linkableTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val linkableTransactions = _linkableTransactions.asStateFlow()

    init {
        findMatches()
    }

    private fun findMatches() {
        viewModelScope.launch {
            val results = transactionRepository.findLinkableTransactions(
                smsDate = potentialTransaction.sourceSmsId,
                smsAmount = potentialTransaction.amount,
                transactionType = potentialTransaction.transactionType
            )
            _linkableTransactions.value = results
        }
    }

    fun linkTransaction(selectedTransactionId: Int, onComplete: () -> Unit) {
        viewModelScope.launch {
            potentialTransaction.sourceSmsHash?.let { hash ->
                transactionRepository.setSmsHash(selectedTransactionId, hash)
            }
            onComplete()
        }
    }
}
