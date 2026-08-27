package com.spendwise.app.domain.repository

import com.spendwise.app.domain.model.Income
import kotlinx.coroutines.flow.Flow

interface IncomeRepository {
    fun getIncomes(): Flow<List<Income>>
    suspend fun addIncome(income: Income): Result<Unit>
    suspend fun updateIncome(income: Income): Result<Unit>
    suspend fun deleteIncome(incomeId: String): Result<Unit>
}
