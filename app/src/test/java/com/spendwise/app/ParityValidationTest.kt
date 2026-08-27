package com.spendwise.app

import com.spendwise.app.domain.model.*
import com.spendwise.app.utils.AnalysisEngine
import org.junit.Assert.*
import org.junit.Test
import java.util.*

/**
 * Authoritative Cross-Platform Parity Test Suite (Android Kotlin).
 * Must match server/test_parity.js down to 0.001 precision.
 */
class ParityValidationTest {

    // -------------------------------------------------------------
    // DATASET 1: Balanced User
    // Income: ₹60,000 | Expense: ₹35,000 | Savings: ₹25,000
    // -------------------------------------------------------------
    @Test
    fun testDataset1_BalancedUser() {
        val incomes = listOf(
            Income(id = "inc1", amount = 60000.0, source = "Salary")
        )
        val expenses = listOf(
            Expense(id = "exp1", amount = 10000.0, category = "Food", description = "Groceries", isEssential = true),
            Expense(id = "exp2", amount = 15000.0, category = "Rent", description = "Apartment Rent", isEssential = true),
            Expense(id = "exp3", amount = 5000.0, category = "Shopping", description = "Clothes", isEssential = false),
            Expense(id = "exp4", amount = 1000.0, category = "Subscriptions", description = "Netflix & Spotify", isEssential = false),
            Expense(id = "exp5", amount = 1000.0, category = "Utilities", description = "Electricity", isEssential = true),
            Expense(id = "exp6", amount = 1000.0, category = "Transport", description = "Metro pass", isEssential = true),
            Expense(id = "exp7", amount = 300.0, category = "Food", description = "Coffee", isEssential = false),
            Expense(id = "exp8", amount = 300.0, category = "Food", description = "Coffee", isEssential = false),
            Expense(id = "exp9", amount = 300.0, category = "Food", description = "Coffee", isEssential = false),
            Expense(id = "exp10", amount = 1100.0, category = "Other", description = "Miscellaneous", isEssential = false)
        )
        val budgets = listOf(
            Budget(id = "b1", category = "Food", amount = 12000.0),
            Budget(id = "b2", category = "Shopping", amount = 6000.0)
        )
        val goals = listOf(
            SavingsGoal(id = "g1", title = "Emergency Fund", targetAmount = 50000.0, currentAmount = 25000.0)
        )

        // 1. Cashflow metrics
        val totalInc = AnalysisEngine.calculateTotalIncome(incomes)
        val totalExp = AnalysisEngine.calculateTotalExpenses(expenses)
        val savings = AnalysisEngine.calculateSavings(incomes, expenses)
        val savingsRate = AnalysisEngine.calculateSavingsRate(incomes, expenses)

        assertEquals(60000.0, totalInc, 0.001)
        assertEquals(35000.0, totalExp, 0.001)
        assertEquals(25000.0, savings, 0.001)
        assertEquals(41.6666, savingsRate, 0.01)

        // 2. Financial Health Score
        val health = AnalysisEngine.calculateFinancialHealth(incomes, expenses, budgets, goals)
        // Savings (>=30% -> 35), Budget (0 exceeded, 1 warning -> 18), Stability (35k/60k=0.583 <=0.70 -> 16), Goals (25k/50k=0.5 -> 10)
        // Total = 35 + 18 + 16 + 10 = 79
        assertEquals(79, health.score)
        assertEquals("Healthy", health.label)

        // 3. Essential vs Discretionary
        val ess = AnalysisEngine.calculateEssentialVsDiscretionary(expenses)
        // Essential: Groceries (10k) + Rent (15k) + Utilities (1k) + Transport (1k) = 27k (77.14%)
        // Discretionary: Shopping (5k) + Subscriptions (1k) + Coffee (900) + Misc (1.1k) = 8k (22.86%)
        assertEquals(27000.0, ess.essentialTotal, 0.001)
        assertEquals(8000.0, ess.discretionaryTotal, 0.001)

        // 4. Spending Leaks
        val leaks = AnalysisEngine.detectSpendingLeaks(expenses)
        // Coffee (<800, count=3, total=900)
        assertTrue(leaks.any { it.name.equals("Coffee", ignoreCase = true) && it.frequencyCount == 3 && it.monthlyTotal == 900.0 })

        // 5. Purchase Impact Analyzer
        val verdictSafe = AnalysisEngine.evaluatePurchaseAffordability(10000.0, "Gadget", "Shopping", incomes, expenses, budgets, goals)
        assertEquals(AffordabilityRating.SAFE, verdictSafe.rating)

        val verdictCaution = AnalysisEngine.evaluatePurchaseAffordability(20000.0, "Laptop", "Shopping", incomes, expenses, budgets, goals)
        assertEquals(AffordabilityRating.CAUTION, verdictCaution.rating)

        val verdictDanger = AnalysisEngine.evaluatePurchaseAffordability(30000.0, "Luxury Watch", "Shopping", incomes, expenses, budgets, goals)
        assertEquals(AffordabilityRating.NOT_RECOMMENDED, verdictDanger.rating)
    }

    // -------------------------------------------------------------
    // DATASET 2: Zero Income User
    // -------------------------------------------------------------
    @Test
    fun testDataset2_ZeroIncome() {
        val incomes = emptyList<Income>()
        val expenses = listOf(
            Expense(id = "e1", amount = 5000.0, category = "Food", description = "Meals")
        )
        val budgets = emptyList<Budget>()
        val goals = emptyList<SavingsGoal>()

        assertEquals(0.0, AnalysisEngine.calculateTotalIncome(incomes), 0.001)
        assertEquals(5000.0, AnalysisEngine.calculateTotalExpenses(expenses), 0.001)
        assertEquals(-5000.0, AnalysisEngine.calculateSavings(incomes, expenses), 0.001)
        assertEquals(0.0, AnalysisEngine.calculateSavingsRate(incomes, expenses), 0.001)

        val health = AnalysisEngine.calculateFinancialHealth(incomes, expenses, budgets, goals)
        // Savings (<=0 -> 5), Budget (no budgets -> 20), Stability (>0.85 -> 6), Goals (no goals -> 15) = 46
        assertEquals(46, health.score)
        assertEquals("Needs Attention", health.label)
    }

    // -------------------------------------------------------------
    // DATASET 3: High Discretionary Spender
    // -------------------------------------------------------------
    @Test
    fun testDataset3_HighDiscretionary() {
        val expenses = listOf(
            Expense(id = "e1", amount = 20000.0, category = "Food", isEssential = true),
            Expense(id = "e2", amount = 40000.0, category = "Shopping", isEssential = false),
            Expense(id = "e3", amount = 20000.0, category = "Entertainment", isEssential = false)
        )
        val ess = AnalysisEngine.calculateEssentialVsDiscretionary(expenses)
        assertEquals(20000.0, ess.essentialTotal, 0.001)
        assertEquals(60000.0, ess.discretionaryTotal, 0.001)
        assertEquals(25.0, ess.essentialPct, 0.01)
        assertEquals(75.0, ess.discretionaryPct, 0.01)
        assertTrue(ess.recommendation.contains("Discretionary spending is high"))
    }

    // -------------------------------------------------------------
    // DATASET 4: Over-Budget User
    // -------------------------------------------------------------
    @Test
    fun testDataset4_OverBudget() {
        val incomes = listOf(Income(amount = 50000.0))
        val expenses = listOf(
            Expense(amount = 15000.0, category = "Food"),
            Expense(amount = 8000.0, category = "Shopping")
        )
        val budgets = listOf(
            Budget(category = "Food", amount = 10000.0), // Exceeded (150%)
            Budget(category = "Shopping", amount = 5000.0) // Exceeded (160%)
        )
        val goals = emptyList<SavingsGoal>()

        val health = AnalysisEngine.calculateFinancialHealth(incomes, expenses, budgets, goals)
        // SavingsRate = (27k/50k)*100 = 54% -> 35 pts
        // Budgets: 2 exceeded -> 6 pts
        // Stability: 23k/50k = 0.46 <= 0.50 -> 20 pts
        // Goals: empty -> 15 pts
        // Total = 35 + 6 + 20 + 15 = 76
        assertEquals(76, health.score)
        assertEquals(6, health.budgetScore)
    }

    // -------------------------------------------------------------
    // DATASET 5: What-If Simulation
    // -------------------------------------------------------------
    @Test
    fun testDataset5_WhatIfSimulation() {
        val incomes = listOf(Income(amount = 60000.0))
        val expenses = listOf(
            Expense(amount = 12000.0, category = "Food"),
            Expense(amount = 8000.0, category = "Shopping"),
            Expense(amount = 20000.0, category = "Rent")
        )
        val budgets = emptyList<Budget>()
        val goals = listOf(
            SavingsGoal(title = "Car", targetAmount = 100000.0, currentAmount = 50000.0)
        )

        // Reduce Food by 2000, Reduce Shopping by 1000, Increase Income by 5000
        val deltas = mapOf("Food" to -2000.0, "Shopping" to -1000.0)
        val sim = AnalysisEngine.calculateWhatIfSimulation(
            incomes = incomes,
            expenses = expenses,
            budgets = budgets,
            goals = goals,
            simulatedIncomeDelta = 5000.0,
            simulatedCategoryDeltas = deltas,
            simulatedRecurringDelta = 0.0
        )

        // Current: Inc 60k, Exp 40k, Savings 20k, Rate 33.33%
        assertEquals(20000.0, sim.currentMonthlySavings, 0.001)
        // Projected: Inc 65k, Exp 37k, Savings 28k
        assertEquals(28000.0, sim.projectedMonthlySavings, 0.001)
        // Improvement: (28k - 20k)/20k * 100 = 40.0%
        assertEquals(40.0, sim.savingsImprovementPct, 0.001)
    }
}
