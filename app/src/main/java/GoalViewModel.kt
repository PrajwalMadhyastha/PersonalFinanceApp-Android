// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/GoalViewModel.kt
// REASON: FIX - Re-added the `getGoalById` function. This function is essential
// for the new dedicated `AddEditGoalScreen` to fetch an existing goal's data
// when operating in "edit mode". Its absence was causing the compilation errors.
// =================================================================================
package io.pm.finlight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
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

    // --- NEW: Function to get a single goal by its ID ---
    fun getGoalById(id: Int): Flow<Goal?> {
        return goalRepository.getGoalById(id)
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
