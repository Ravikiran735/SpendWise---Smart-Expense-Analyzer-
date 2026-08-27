package com.spendwise.app

import com.spendwise.app.domain.model.*
import com.spendwise.app.utils.AnalysisEngine
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class AnalysisEngineTest {

    @Test
    fun calculateTotalIncome_sumsCorrectly() {
        val incomes = listOf(
            Income(amount = 50000.0, source = "Salary"),
            Income(amount = 12000.0, source = "Freelance")
        )
        val total = AnalysisEngine.calculateTotalIncome(incomes)
        assertEquals(62000.0, total, 0.001)
    }

    @Test
    fun calculateTotalExpenses_sumsCorrectly() {
        val expenses = listOf(
            Expense(amount = 1500.0, category = "Food"),
            Expense(amount = 500.0, category = "Transport"),
            Expense(amount = 2000.0, category = "Shopping")
        )
        val total = AnalysisEngine.calculateTotalExpenses(expenses)
        assertEquals(4000.0, total, 0.001)
    }

    @Test
    fun calculateSavingsAndSavingsRate_computesCorrectly() {
        val incomes = listOf(Income(amount = 10000.0, source = "Salary"))
        val expenses = listOf(Expense(amount = 2500.0, category = "Food"))

        val savings = AnalysisEngine.calculateSavings(incomes, expenses)
        val savingsRate = AnalysisEngine.calculateSavingsRate(incomes, expenses)

        assertEquals(7500.0, savings, 0.001)
        assertEquals(75.0, savingsRate, 0.001)
    }

    @Test
    fun calculateSavingsRate_withZeroIncome_returnsZero() {
        val incomes = emptyList<Income>()
        val expenses = listOf(Expense(amount = 500.0, category = "Food"))

        val savingsRate = AnalysisEngine.calculateSavingsRate(incomes, expenses)
        assertEquals(0.0, savingsRate, 0.001)
    }

    @Test
    fun emptyTransactionLists_handledSafelyWithoutExceptions() {
        val emptyIncomes = emptyList<Income>()
        val emptyExpenses = emptyList<Expense>()
        val emptyBudgets = emptyList<Budget>()

        assertEquals(0.0, AnalysisEngine.calculateTotalIncome(emptyIncomes), 0.001)
        assertEquals(0.0, AnalysisEngine.calculateTotalExpenses(emptyExpenses), 0.001)
        assertEquals(0.0, AnalysisEngine.calculateSavings(emptyIncomes, emptyExpenses), 0.001)
        assertEquals(0.0, AnalysisEngine.calculateSavingsRate(emptyIncomes, emptyExpenses), 0.001)
        assertTrue(AnalysisEngine.calculateCategoryTotals(emptyExpenses).isEmpty())
        assertTrue(AnalysisEngine.detectRecurringMoneyMap(emptyExpenses).items.isEmpty())
        assertTrue(AnalysisEngine.generateInsights(emptyIncomes, emptyExpenses, emptyBudgets).isEmpty())
    }

    @Test
    fun calculateCategoryTotals_groupsAndSumsByCategory() {
        val expenses = listOf(
            Expense(amount = 500.0, category = "Food"),
            Expense(amount = 300.0, category = "Food"),
            Expense(amount = 200.0, category = "Transport")
        )
        val totals = AnalysisEngine.calculateCategoryTotals(expenses)

        assertEquals(800.0, totals["Food"] ?: 0.0, 0.001)
        assertEquals(200.0, totals["Transport"] ?: 0.0, 0.001)
    }

    @Test
    fun detectRecurringMoneyMap_findsRepeatedDescriptions() {
        val expenses = listOf(
            Expense(description = "Netflix", category = "Entertainment", amount = 650.0),
            Expense(description = "Netflix", category = "Entertainment", amount = 650.0),
            Expense(description = "netflix", category = "Entertainment", amount = 650.0),
            Expense(description = "Coffee", category = "Food", amount = 150.0)
        )
        val recurring = AnalysisEngine.detectRecurringMoneyMap(expenses).items

        assertTrue(recurring.isNotEmpty())
        assertEquals("Netflix", recurring.first().name)
        assertEquals(650.0, recurring.first().monthlyAmount, 0.001)
    }

    @Test
    fun generateInsights_createsBudgetAndSpendingInsights() {
        val incomes = listOf(Income(amount = 50000.0))
        val expenses = listOf(
            Expense(category = "Food", amount = 4500.0, description = "Lunch"),
            Expense(category = "Food", amount = 4800.0, description = "Dinner")
        )
        val budgets = listOf(
            Budget(category = "Food", amount = 10000.0)
        )

        val insights = AnalysisEngine.generateInsights(incomes, expenses, budgets)
        assertTrue(insights.any { it.type == InsightType.SPENDING })
        assertTrue(insights.any { it.type == InsightType.BUDGET })
    }

    @Test
    fun generateInsights_zeroBudget_doesNotCrash() {
        val incomes = listOf(Income(amount = 5000.0))
        val expenses = listOf(Expense(category = "Food", amount = 100.0))
        val budgets = listOf(Budget(category = "Food", amount = 0.0))

        val insights = AnalysisEngine.generateInsights(incomes, expenses, budgets)
        assertNotNull(insights)
    }
}
