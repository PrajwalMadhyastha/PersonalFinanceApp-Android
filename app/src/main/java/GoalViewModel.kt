// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/GoalViewModel.kt
// REASON: FIX - The unused `getGoalById` function has been removed to resolve
// the "UnusedSymbol" warning, cleaning up the ViewModel's public API.
// =================================================================================
package io.pm.finlight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalViewModel(application: Application) : AndroidViewModel(application) {

    private val goalRepository: GoalRepository
    val allGoals: StateFlow<List<GoalWithAccountName>>

    init {
        val db = AppDatabase.getInstance(application)
        goalRepository = GoalRepository(db.goalDao())
        allGoals = goalRepository.getAllGoalsWithAccountName()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    fun saveGoal(
        id: Int?,
        name: String,
        targetAmount: Double,
        savedAmount: Double,
        targetDate: Long?,
        accountId: Int
    ) {
        viewModelScope.launch {
            val goal = Goal(
                id = id ?: 0,
                name = name,
                targetAmount = targetAmount,
                savedAmount = savedAmount,
                targetDate = targetDate,
                accountId = accountId
            )
            if (id == null) {
                goalRepository.insert(goal)
            } else {
                goalRepository.update(goal)
            }
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            goalRepository.delete(goal)
        }
    }
}
