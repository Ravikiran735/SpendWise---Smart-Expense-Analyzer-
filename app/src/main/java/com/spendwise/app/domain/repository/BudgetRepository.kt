package com.spendwise.app.domain.repository

import com.spendwise.app.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudgets(): Flow<List<Budget>>
    suspend fun addBudget(budget: Budget): Result<Unit>
    suspend fun updateBudget(budget: Budget): Result<Unit>
    suspend fun deleteBudget(budgetId: String): Result<Unit>
}
