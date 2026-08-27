package com.spendwise.app.domain.repository

import com.spendwise.app.domain.model.Expense
import com.spendwise.app.domain.model.ImportCandidate
import com.spendwise.app.domain.model.ImportHistory
import com.spendwise.app.domain.model.ImportPreferences
import com.spendwise.app.domain.model.Income
import kotlinx.coroutines.flow.Flow

interface ImportRepository {
    fun getImportHistory(): Flow<List<ImportHistory>>
    suspend fun recordImport(history: ImportHistory): Result<String>
    suspend fun saveImportedTransactions(
        importId: String,
        candidates: List<ImportCandidate>
    ): Result<Pair<Int, Int>> // returns Pair(importedExpensesCount, importedIncomesCount)
    fun getImportPreferences(): Flow<ImportPreferences>
    suspend fun updateImportPreferences(preferences: ImportPreferences): Result<Unit>
}
