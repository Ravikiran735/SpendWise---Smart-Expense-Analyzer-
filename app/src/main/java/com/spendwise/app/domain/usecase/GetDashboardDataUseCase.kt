package com.spendwise.app.domain.usecase

import com.spendwise.app.domain.repository.*
import kotlinx.coroutines.flow.combine

class GetDashboardDataUseCase(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository
) {
    operator fun invoke() = combine(
        expenseRepository.getExpenses(),
        incomeRepository.getIncomes()
    ) { expenses, incomes ->
        // Could perform complex merging here
        Pair(expenses, incomes)
    }
}
