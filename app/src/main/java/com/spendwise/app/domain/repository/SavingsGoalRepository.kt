package com.spendwise.app.domain.repository

import com.spendwise.app.domain.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

interface SavingsGoalRepository {
    fun getSavingsGoals(): Flow<List<SavingsGoal>>
    suspend fun addSavingsGoal(goal: SavingsGoal): Result<Unit>
    suspend fun updateSavingsGoal(goal: SavingsGoal): Result<Unit>
    suspend fun deleteSavingsGoal(goalId: String): Result<Unit>
}
