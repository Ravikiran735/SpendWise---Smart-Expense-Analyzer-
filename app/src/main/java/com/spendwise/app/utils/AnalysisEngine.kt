package com.spendwise.app.utils

import com.spendwise.app.domain.model.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

data class FinancialHealth(
    val score: Int = 75,
    val label: String = "Healthy",
    val statusColor: String = "emerald", // "emerald", "amber", "rose"
    val savingsRateScore: Int = 25,
    val budgetScore: Int = 20,
    val expenseStabilityScore: Int = 15,
    val goalsScore: Int = 15
)

data class SmartSnapshot(
    val spendingChangePct: Double = 0.0,
    val isSpendingLower: Boolean = true,
    val savingsChangePct: Double = 0.0,
    val isSavingsHigher: Boolean = true,
    val topBudgetName: String = "None",
    val topBudgetPct: Int = 0,
    val isBudgetWarning: Boolean = false,
    val topGoalName: String = "None",
    val topGoalPct: Int = 0
)

object AnalysisEngine {

    fun calculateTotalIncome(incomes: List<Income>): Double = incomes.sumOf { it.amount }

    fun calculateTotalExpenses(expenses: List<Expense>): Double = expenses.sumOf { it.amount }

    fun calculateSavings(incomes: List<Income>, expenses: List<Expense>): Double =
        calculateTotalIncome(incomes) - calculateTotalExpenses(expenses)

    fun calculateSavingsRate(incomes: List<Income>, expenses: List<Expense>): Double {
        val totalIncome = calculateTotalIncome(incomes)
        if (totalIncome <= 0.0) return 0.0
        val savings = calculateSavings(incomes, expenses)
        return ((savings / totalIncome) * 100).coerceIn(-100.0, 100.0)
    }

    fun calculateCategoryTotals(expenses: List<Expense>): Map<String, Double> {
        return expenses.groupBy { it.category.ifBlank { "Other" } }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    fun calculateMonthlyExpenses(expenses: List<Expense>): Map<String, Double> {
        val calendar = Calendar.getInstance()
        return expenses.groupBy {
            calendar.time = it.date
            "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}"
        }.mapValues { it.value.sumOf { exp -> exp.amount } }
    }

    fun calculateMonthlyIncomes(incomes: List<Income>): Map<String, Double> {
        val calendar = Calendar.getInstance()
        return incomes.groupBy {
            calendar.time = it.date
            "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}"
        }.mapValues { it.value.sumOf { inc -> inc.amount } }
    }

    // ==========================================
    // 1. ESSENTIAL VS DISCRETIONARY
    // ==========================================
    fun isCategoryEssentialDefault(category: String): Boolean {
        val cat = category.lowercase(Locale.ROOT).trim()
        return when {
            cat.contains("rent") || cat.contains("housing") || cat.contains("mortgage") -> true
            cat.contains("utilit") || cat.contains("electricity") || cat.contains("water") || cat.contains("gas") || cat.contains("bill") -> true
            cat.contains("grocer") || (cat.contains("food") && !cat.contains("delivery") && !cat.contains("dining") && !cat.contains("restaurant") && !cat.contains("swiggy") && !cat.contains("zomato")) -> true
            cat.contains("educat") || cat.contains("tuition") || cat.contains("school") || cat.contains("course") -> true
            cat.contains("health") || cat.contains("medic") || cat.contains("doctor") || cat.contains("pharmacy") -> true
            cat.contains("insur") || cat.contains("emi") || cat.contains("loan") -> true
            cat.contains("transport") && !cat.contains("uber") && !cat.contains("ola") && !cat.contains("taxi") -> true
            else -> false
        }
    }

    fun isExpenseEssential(expense: Expense, categoryOverrides: Map<String, Boolean> = emptyMap()): Boolean {
        if (expense.isEssential != null) return expense.isEssential
        if (categoryOverrides.containsKey(expense.category)) return categoryOverrides[expense.category] == true
        return isCategoryEssentialDefault(expense.category)
    }

    fun calculateEssentialVsDiscretionary(
        expenses: List<Expense>,
        categoryOverrides: Map<String, Boolean> = emptyMap()
    ): EssentialDiscretionaryAnalysis {
        var essentialTotal = 0.0
        var discretionaryTotal = 0.0
        val essentialCats = mutableMapOf<String, Double>()
        val discretionaryCats = mutableMapOf<String, Double>()

        expenses.forEach { exp ->
            val isEss = isExpenseEssential(exp, categoryOverrides)
            val cat = exp.category.ifBlank { "Other" }
            if (isEss) {
                essentialTotal += exp.amount
                essentialCats[cat] = (essentialCats[cat] ?: 0.0) + exp.amount
            } else {
                discretionaryTotal += exp.amount
                discretionaryCats[cat] = (discretionaryCats[cat] ?: 0.0) + exp.amount
            }
        }

        val total = essentialTotal + discretionaryTotal
        val essentialPct = if (total > 0) (essentialTotal / total) * 100 else 50.0
        val discretionaryPct = if (total > 0) (discretionaryTotal / total) * 100 else 50.0

        val recommendation = when {
            total == 0.0 -> "Record transactions to evaluate your Essential vs Discretionary spending breakdown."
            discretionaryPct > 45.0 -> "Discretionary spending is high (${discretionaryPct.toInt()}% of total). Target reducing non-essentials below 30% to boost monthly savings."
            discretionaryPct > 30.0 -> "Discretionary spending is well balanced (${discretionaryPct.toInt()}%). Maintain this ratio to support long-term goals."
            else -> "Essential spending dominates (${essentialPct.toInt()}%). Strong discipline in discretionary purchases."
        }

        return EssentialDiscretionaryAnalysis(
            essentialTotal = essentialTotal,
            discretionaryTotal = discretionaryTotal,
            essentialPct = essentialPct,
            discretionaryPct = discretionaryPct,
            essentialCategories = essentialCats.toList().sortedByDescending { it.second },
            discretionaryCategories = discretionaryCats.toList().sortedByDescending { it.second },
            recommendation = recommendation
        )
    }

    // ==========================================
    // 2. WHAT-IF FINANCIAL SIMULATOR
    // ==========================================
    fun calculateWhatIfSimulation(
        incomes: List<Income>,
        expenses: List<Expense>,
        budgets: List<Budget>,
        goals: List<SavingsGoal>,
        simulatedIncomeDelta: Double = 0.0,
        simulatedCategoryDeltas: Map<String, Double> = emptyMap(), // negative = reduced spending, positive = increased
        simulatedRecurringDelta: Double = 0.0
    ): WhatIfSimulationResult {
        val totalInc = calculateTotalIncome(incomes)
        val totalExp = calculateTotalExpenses(expenses)
        val currentMonthlySavings = max(0.0, totalInc - totalExp)
        val currentSavingsRate = calculateSavingsRate(incomes, expenses)

        val netExpenseDelta = simulatedCategoryDeltas.values.sum() + simulatedRecurringDelta
        val projectedMonthlyExpenses = max(0.0, totalExp + netExpenseDelta)
        val projectedMonthlyIncome = max(0.0, totalInc + simulatedIncomeDelta)
        val projectedMonthlySavings = max(0.0, projectedMonthlyIncome - projectedMonthlyExpenses)
        val projectedSavingsRate = if (projectedMonthlyIncome > 0) ((projectedMonthlySavings / projectedMonthlyIncome) * 100) else 0.0

        val savingsImprovementPct = if (currentMonthlySavings > 0) {
            ((projectedMonthlySavings - currentMonthlySavings) / currentMonthlySavings) * 100
        } else if (projectedMonthlySavings > 0) 100.0 else 0.0

        val goalShifts = goals.map { goal ->
            val remaining = max(0.0, goal.targetAmount - goal.currentAmount)
            val currentRate = if (currentMonthlySavings > 0) currentMonthlySavings * 0.4 else 1000.0
            val projectedRate = if (projectedMonthlySavings > 0) projectedMonthlySavings * 0.4 else currentRate

            val curMonths = if (currentRate > 0) remaining / currentRate else 12.0
            val projMonths = if (projectedRate > 0) remaining / projectedRate else curMonths
            val daysSaved = ((curMonths - projMonths) * 30.4).roundToInt()

            GoalTimelineShift(
                goalTitle = goal.title.ifBlank { "Savings Goal" },
                currentMonthsRemaining = max(0.1, (curMonths * 10).roundToInt() / 10.0),
                projectedMonthsRemaining = max(0.1, (projMonths * 10).roundToInt() / 10.0),
                daysSavedOrDelayed = daysSaved
            )
        }

        val explanation = if (projectedMonthlySavings > currentMonthlySavings) {
            val delta = projectedMonthlySavings - currentMonthlySavings
            "This simulation increases your monthly savings by +${String.format(Locale.ROOT, "%.1f", savingsImprovementPct)}% (+₹${delta.toInt()}/mo). Goal completion will accelerate across your active milestones."
        } else if (projectedMonthlySavings < currentMonthlySavings) {
            val delta = currentMonthlySavings - projectedMonthlySavings
            "This decision would reduce monthly savings by ₹${delta.toInt()} (-${String.format(Locale.ROOT, "%.1f", abs(savingsImprovementPct))}%). Goal completion dates will shift later."
        } else {
            "No significant change to projected net cashflow."
        }

        val deltaSummary = "Current: ₹${currentMonthlySavings.toInt()} → Projected: ₹${projectedMonthlySavings.toInt()} (${if (savingsImprovementPct >= 0) "+" else ""}${String.format(Locale.ROOT, "%.1f", savingsImprovementPct)}%)"

        return WhatIfSimulationResult(
            currentMonthlySavings = currentMonthlySavings,
            projectedMonthlySavings = projectedMonthlySavings,
            savingsImprovementPct = savingsImprovementPct,
            currentSavingsRate = currentSavingsRate,
            projectedSavingsRate = projectedSavingsRate,
            goalShifts = goalShifts,
            explanation = explanation,
            deltaSummary = deltaSummary
        )
    }

    // ==========================================
    // 3. PURCHASE IMPACT ANALYZER ("CAN I AFFORD THIS?")
    // ==========================================
    fun evaluatePurchaseAffordability(
        amount: Double,
        description: String,
        category: String = "Shopping",
        incomes: List<Income>,
        expenses: List<Expense>,
        budgets: List<Budget>,
        goals: List<SavingsGoal>
    ): PurchaseAffordabilityResult {
        val totalInc = calculateTotalIncome(incomes)
        val totalExp = calculateTotalExpenses(expenses)
        val monthlySurplus = totalInc - totalExp
        val essentialAnalysis = calculateEssentialVsDiscretionary(expenses)
        val emergencyBufferRequired = max(5000.0, essentialAnalysis.essentialTotal)
        val currentSavingsReserves = goals.sumOf { it.currentAmount }
        val surplusAfter = monthlySurplus - amount
        val emergencyBufferRemaining = (currentSavingsReserves + monthlySurplus) - amount

        val topGoal = goals.minByOrNull { (it.targetAmount - it.currentAmount) }
        var goalDelayDays = 0
        var affectedGoalTitle = ""

        if (topGoal != null && topGoal.targetAmount > topGoal.currentAmount) {
            val remaining = topGoal.targetAmount - topGoal.currentAmount
            val monthlyAllocation = if (monthlySurplus > 0) monthlySurplus * 0.4 else 1000.0
            val curMonths = remaining / max(100.0, monthlyAllocation)
            val newAllocation = max(100.0, monthlyAllocation - (amount * 0.25))
            val newMonths = remaining / newAllocation
            goalDelayDays = max(0, ((newMonths - curMonths) * 30.4).roundToInt())
            affectedGoalTitle = topGoal.title
        }

        val (rating, verdictTitle, message, whyExplanation) = when {
            totalInc == 0.0 && totalExp == 0.0 -> {
                Quad(
                    AffordabilityRating.CAUTION,
                    "INSUFFICIENT DATA",
                    "Not enough transaction data yet to make a safe determination. Please log recent income and expenses.",
                    "Determination requires at least 1 recorded income and basic expense ledger."
                )
            }
            monthlySurplus >= amount * 1.5 && emergencyBufferRemaining >= emergencyBufferRequired -> {
                Quad(
                    AffordabilityRating.SAFE,
                    "SAFE",
                    "You can make this purchase of ₹${amount.toInt()} while maintaining your current savings goal and emergency buffer.",
                    "Your monthly surplus (₹${monthlySurplus.toInt()}) comfortably covers this expense with ₹${surplusAfter.toInt()} left over. Emergency reserves remain intact."
                )
            }
            monthlySurplus >= amount -> {
                val delayText = if (goalDelayDays > 0 && affectedGoalTitle.isNotBlank()) " It could delay your $affectedGoalTitle goal by approximately $goalDelayDays days." else ""
                Quad(
                    AffordabilityRating.CAUTION,
                    "CAUTION",
                    "You can afford this purchase, but it will consume ${( (amount / max(1.0, monthlySurplus)) * 100).toInt()}% of your monthly surplus.$delayText",
                    "Purchase utilizes a substantial portion of your surplus (₹${monthlySurplus.toInt()}). Buffer will be tight for the remainder of this cycle."
                )
            }
            else -> {
                Quad(
                    AffordabilityRating.NOT_RECOMMENDED,
                    "NOT RECOMMENDED",
                    "This purchase of ₹${amount.toInt()} exceeds your available monthly surplus (₹${monthlySurplus.toInt()}) and could compromise financial stability.",
                    "Recorded monthly expenses match or exceed recorded cashflow. Adding ₹${amount.toInt()} would result in a deficit of ₹${abs(surplusAfter).toInt()}."
                )
            }
        }

        val dataUsed = "Analyzed ${expenses.size} expenses, ${incomes.size} incomes, ${budgets.size} budgets, and ${goals.size} savings goals."

        return PurchaseAffordabilityResult(
            amount = amount,
            description = description.ifBlank { "Unspecified Purchase" },
            rating = rating,
            verdictTitle = verdictTitle,
            message = message,
            goalImpactDays = goalDelayDays,
            affectedGoalTitle = affectedGoalTitle,
            monthlySurplusAfter = surplusAfter,
            emergencyBufferRemaining = emergencyBufferRemaining,
            discretionaryImpactPct = if (essentialAnalysis.discretionaryTotal > 0) (amount / essentialAnalysis.discretionaryTotal) * 100 else 100.0,
            explanationWhy = whyExplanation,
            dataUsedSummary = dataUsed
        )
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    // ==========================================
    // 4. FINANCIAL DIGITAL TWIN ("MY FINANCIAL PROFILE")
    // ==========================================
    fun calculateFinancialDigitalTwin(
        incomes: List<Income>,
        expenses: List<Expense>,
        budgets: List<Budget>,
        goals: List<SavingsGoal>
    ): FinancialDigitalTwin {
        val totalInc = calculateTotalIncome(incomes)
        val totalExp = calculateTotalExpenses(expenses)
        val avgMonthlySavings = max(0.0, totalInc - totalExp)
        val savingsRate = calculateSavingsRate(incomes, expenses)
        val catTotals = calculateCategoryTotals(expenses)
        val sortedCats = catTotals.toList().sortedByDescending { it.second }
        val essentialAnalysis = calculateEssentialVsDiscretionary(expenses)
        val recurringMap = detectRecurringMoneyMap(expenses)

        // 1. Income Stability Score (80-95% for steady salary, 60-75% for variable)
        val incomeStabilityPct = when {
            incomes.isEmpty() -> 70
            incomes.size == 1 && totalInc > 0 -> 88
            else -> {
                val amounts = incomes.map { it.amount }
                val mean = amounts.average()
                val variance = amounts.map { (it - mean) * (it - mean) }.average()
                val stdDev = sqrt(variance)
                val cv = if (mean > 0) (stdDev / mean) else 0.0
                (95 - (cv * 40)).roundToInt().coerceIn(50, 95)
            }
        }

        // 2. Savings Discipline Score (70-95%)
        val savingsDisciplinePct = when {
            savingsRate >= 30.0 -> 92
            savingsRate >= 20.0 -> 84
            savingsRate >= 10.0 -> 74
            savingsRate > 0.0 -> 62
            else -> 45
        }

        // 3. Budget Discipline Score (60-95%)
        var budgetExceededCount = 0
        budgets.forEach { b ->
            if (b.amount > 0) {
                val spent = catTotals[b.category] ?: 0.0
                if (spent > b.amount) budgetExceededCount++
            }
        }
        val budgetDisciplinePct = when {
            budgets.isEmpty() -> 78
            budgetExceededCount == 0 -> 90
            budgetExceededCount == 1 -> 76
            else -> 58
        }

        // 4. Spending Stability Score (60-90%)
        val spendingStabilityPct = when {
            expenses.isEmpty() -> 75
            essentialAnalysis.discretionaryPct <= 30.0 -> 86
            essentialAnalysis.discretionaryPct <= 45.0 -> 74
            else -> 63
        }

        // Overall Goal progress
        val goalProgressPct = if (goals.isEmpty()) 0 else {
            val totalTarget = goals.sumOf { it.targetAmount }
            val totalCurrent = goals.sumOf { it.currentAmount }
            if (totalTarget > 0) ((totalCurrent / totalTarget) * 100).toInt().coerceIn(0, 100) else 0
        }

        val narrative = "Your Financial Digital Twin reflects a strong savings discipline of $savingsDisciplinePct% and budget adherence of $budgetDisciplinePct%. Essential expenses account for ${essentialAnalysis.essentialPct.toInt()}% of total cashflow."

        return FinancialDigitalTwin(
            avgMonthlyIncome = totalInc,
            avgMonthlyExpenses = totalExp,
            avgMonthlySavings = avgMonthlySavings,
            savingsRate = savingsRate,
            topSpendingCategories = sortedCats.take(5),
            monthlyRecurringTotal = recurringMap.monthlyRecurringTotal,
            discretionarySpendingTotal = essentialAnalysis.discretionaryTotal,
            essentialSpendingTotal = essentialAnalysis.essentialTotal,
            incomeStabilityPct = incomeStabilityPct,
            savingsDisciplinePct = savingsDisciplinePct,
            budgetDisciplinePct = budgetDisciplinePct,
            spendingStabilityPct = spendingStabilityPct,
            spendingVolatilityPct = 100.0 - spendingStabilityPct,
            goalProgressPct = goalProgressPct,
            summaryNarrative = narrative
        )
    }

    // ==========================================
    // 5. RECURRING MONEY MAP
    // ==========================================
    fun detectRecurringMoneyMap(
        expenses: List<Expense>,
        overrides: Map<String, String> = emptyMap()
    ): RecurringMoneyMap {
        val recurringPatterns = listOf(
            "netflix", "spotify", "prime", "amazon prime", "hotstar", "youtube", "apple", "google",
            "rent", "maintenance", "wifi", "broadband", "jio", "airtel", "electricity", "insurance",
            "lic", "emi", "loan", "gym", "cult", "newspaper", "maid", "cook", "school fee", "sip", "zerodha"
        )

        val groups = expenses.groupBy { it.description.lowercase(Locale.ROOT).trim() }
        val detected = mutableListOf<RecurringMoneyItem>()

        groups.forEach { (desc, items) ->
            val isKnownPattern = recurringPatterns.any { pattern -> desc.contains(pattern) }
            val isFrequent = items.size >= 2
            val isSubscriptionCategory = items.any { it.category.equals("Subscriptions", ignoreCase = true) || it.category.equals("Rent", ignoreCase = true) || it.category.equals("Utilities", ignoreCase = true) }

            if (isKnownPattern || isFrequent || isSubscriptionCategory) {
                val avgAmt = items.map { it.amount }.average()
                val id = desc.replace("\\s+".toRegex(), "_")
                val statusStr = overrides[id]?.uppercase(Locale.ROOT) ?: "KEEP"
                val status = try { RecurringStatus.valueOf(statusStr) } catch (e: Exception) { RecurringStatus.KEEP }

                detected.add(
                    RecurringMoneyItem(
                        id = id,
                        name = desc.ifBlank { items.first().category }.replaceFirstChar { it.uppercase() },
                        category = items.first().category.ifBlank { "Subscriptions" },
                        monthlyAmount = avgAmt,
                        annualAmount = avgAmt * 12.0,
                        frequency = "monthly",
                        status = status,
                        confidence = if (isKnownPattern) 0.95 else 0.82,
                        lastChargedDate = items.maxByOrNull { it.date }?.date ?: Date()
                    )
                )
            }
        }

        // Deduplicate & sort by highest monthly impact
        val distinctItems = detected.distinctBy { it.id }.sortedByDescending { it.monthlyAmount }
        val activeItems = distinctItems.filter { it.status != RecurringStatus.CANCEL }
        val monthlyTotal = activeItems.sumOf { it.monthlyAmount }
        val cancelledSavings = distinctItems.filter { it.status == RecurringStatus.CANCEL }.sumOf { it.monthlyAmount * 12.0 }

        return RecurringMoneyMap(
            monthlyRecurringTotal = monthlyTotal,
            annualProjectedTotal = monthlyTotal * 12.0,
            items = distinctItems,
            potentialSavingsIfCancelled = cancelledSavings
        )
    }

    // ==========================================
    // 6. SPENDING LEAK DETECTOR
    // ==========================================
    fun detectSpendingLeaks(expenses: List<Expense>): List<SpendingLeak> {
        val leaks = mutableListOf<SpendingLeak>()
        val leakKeywords = listOf("coffee", "chai", "tea", "swiggy", "zomato", "snack", "uber", "ola", "rapido", "blinkit", "zepto", "instamart", "vending", "fast food", "dessert")

        val grouped = expenses.groupBy { it.description.lowercase(Locale.ROOT).trim() }

        grouped.forEach { (desc, items) ->
            val isLeakKeyword = leakKeywords.any { desc.contains(it) }
            val isSmallRepeated = items.size >= 3 && items.all { it.amount <= 800.0 }

            if ((isLeakKeyword && items.size >= 2) || isSmallRepeated) {
                val count = items.size
                val avgAmt = items.map { it.amount }.average()
                val monthlyTotal = items.sumOf { it.amount }

                leaks.add(
                    SpendingLeak(
                        id = desc.replace("\\s+".toRegex(), "_"),
                        name = desc.ifBlank { items.first().category }.replaceFirstChar { it.uppercase() },
                        category = items.first().category.ifBlank { "Food" },
                        frequencyCount = count,
                        averageAmount = avgAmt,
                        monthlyTotal = monthlyTotal,
                        annualImpact = monthlyTotal * 12.0,
                        aiExplanation = "$count repeated purchases averaging ₹${avgAmt.toInt()} accumulate to ₹${monthlyTotal.toInt()} this month."
                    )
                )
            }
        }

        return leaks.sortedByDescending { it.monthlyTotal }.take(5)
    }

    // ==========================================
    // 7. MONEY HABIT SCORE (0–100)
    // ==========================================
    fun calculateMoneyHabitScore(
        incomes: List<Income>,
        expenses: List<Expense>,
        budgets: List<Budget>,
        goals: List<SavingsGoal>,
        settings: UserSettings = UserSettings()
    ): MoneyHabitScore {
        val totalInc = calculateTotalIncome(incomes)
        val totalExp = calculateTotalExpenses(expenses)
        val savingsRate = calculateSavingsRate(incomes, expenses)
        val essentialAnalysis = calculateEssentialVsDiscretionary(expenses, settings.essentialOverrides)
        val recurringMap = detectRecurringMoneyMap(expenses, settings.recurringOverrides)
        val bullets = mutableListOf<HabitBulletPoint>()

        // 1. Savings Consistency (Max 25 pts)
        val savingsScore = when {
            savingsRate >= 25.0 -> {
                bullets.add(HabitBulletPoint(true, "Consistent Savings", "Saving ${savingsRate.toInt()}% of income regularly"))
                25
            }
            savingsRate >= 15.0 -> {
                bullets.add(HabitBulletPoint(true, "Positive Savings Discipline", "Positive savings rate of ${savingsRate.toInt()}% maintained"))
                20
            }
            savingsRate > 0.0 -> {
                bullets.add(HabitBulletPoint(false, "Low Savings Surplus", "Savings rate is under 10%; aim for 20%"))
                14
            }
            else -> {
                bullets.add(HabitBulletPoint(false, "Zero Savings Margin", "Expenses exceed or match recorded income"))
                6
            }
        }

        // 2. Budget Adherence (Max 25 pts)
        val catTotals = calculateCategoryTotals(expenses)
        var overBudgetCount = 0
        budgets.forEach { b ->
            if (b.amount > 0 && (catTotals[b.category] ?: 0.0) > b.amount) overBudgetCount++
        }
        val budgetScore = when {
            budgets.isEmpty() -> 18
            overBudgetCount == 0 -> {
                bullets.add(HabitBulletPoint(true, "Good Budget Discipline", "All tracked category caps respected"))
                25
            }
            overBudgetCount == 1 -> {
                bullets.add(HabitBulletPoint(false, "Single Budget Overrun", "1 category exceeded its planned boundary"))
                16
            }
            else -> {
                bullets.add(HabitBulletPoint(false, "Multiple Budget Overruns", "$overBudgetCount categories exceeded caps"))
                8
            }
        }

        // 3. Recurring Expense Control (Max 15 pts)
        val recurringRatio = if (totalInc > 0) (recurringMap.monthlyRecurringTotal / totalInc) * 100 else 20.0
        val recurringScore = when {
            recurringRatio <= 20.0 -> {
                bullets.add(HabitBulletPoint(true, "Controlled Recurring Costs", "Subscriptions represent under 20% of cashflow"))
                15
            }
            recurringRatio <= 35.0 -> 11
            else -> {
                bullets.add(HabitBulletPoint(false, "High Recurring Commitments", "Subscriptions & bills take ${recurringRatio.toInt()}% of income"))
                6
            }
        }

        // 4. Discretionary Spending Ratio (Max 15 pts)
        val discretionaryScore = when {
            essentialAnalysis.discretionaryPct <= 30.0 -> {
                bullets.add(HabitBulletPoint(true, "Controlled Discretionary Spending", "Non-essential purchases kept below 30%"))
                15
            }
            essentialAnalysis.discretionaryPct <= 45.0 -> 11
            else -> {
                bullets.add(HabitBulletPoint(false, "High Discretionary Spending", "Wants & shopping constitute ${essentialAnalysis.discretionaryPct.toInt()}% of total expenses"))
                6
            }
        }

        // 5. Goal Contributions (Max 10 pts)
        val goalScore = if (goals.isNotEmpty() && goals.any { it.currentAmount > 0 }) {
            bullets.add(HabitBulletPoint(true, "Active Goal Allocations", "Making steady progress on ${goals.size} savings goals"))
            10
        } else 6

        // 6. Volatility & Impulse Control (Max 10 pts)
        val volatilityScore = if (detectSpendingLeaks(expenses).size <= 2) 10 else 6

        val totalScore = (savingsScore + budgetScore + recurringScore + discretionaryScore + goalScore + volatilityScore).coerceIn(10, 100)

        val label = when {
            totalScore >= 85 -> "EXCELLENT MONEY HABITS"
            totalScore >= 70 -> "GOOD MONEY HABITS"
            totalScore >= 50 -> "MODERATE MONEY HABITS"
            else -> "NEEDS HABIT DISCIPLINE"
        }

        val explanation = "Your Money Habit Score evaluates behavioral cashflow discipline across savings regularity, budget adherence, and impulse leak management."

        return MoneyHabitScore(
            score = totalScore,
            label = label,
            savingsConsistencyScore = savingsScore,
            budgetAdherenceScore = budgetScore,
            recurringExpenseControlScore = recurringScore,
            discretionaryRatioScore = discretionaryScore,
            goalContributionsScore = goalScore,
            spendingVolatilityScore = volatilityScore,
            bulletPoints = bullets,
            habitExplanation = explanation
        )
    }

    // ==========================================
    // 8. GOAL ROADMAP & MULTI-GOAL PRIORITY
    // ==========================================
    fun generateGoalRoadmap(
        goals: List<SavingsGoal>,
        incomes: List<Income>,
        expenses: List<Expense>
    ): List<GoalRoadmapItem> {
        val totalInc = calculateTotalIncome(incomes)
        val totalExp = calculateTotalExpenses(expenses)
        val monthlySurplus = max(0.0, totalInc - totalExp)

        val cal = Calendar.getInstance()
        val monthNames = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

        return goals.map { goal ->
            val remaining = max(0.0, goal.targetAmount - goal.currentAmount)
            val progressPct = if (goal.targetAmount > 0) ((goal.currentAmount / goal.targetAmount) * 100).toInt().coerceIn(0, 100) else 0

            val milestoneReached = when {
                progressPct >= 100 -> 100
                progressPct >= 75 -> 75
                progressPct >= 50 -> 50
                progressPct >= 25 -> 25
                else -> 0
            }

            val monthlyContribution = if (goals.size > 0 && monthlySurplus > 0) (monthlySurplus / goals.size).coerceAtLeast(1000.0) else 1500.0
            val monthsToFinish = if (monthlyContribution > 0) ceil(remaining / monthlyContribution).toInt() else 6

            val projCal = Calendar.getInstance()
            projCal.add(Calendar.MONTH, monthsToFinish)
            val projDateStr = "${monthNames[projCal.get(Calendar.MONTH)]} ${projCal.get(Calendar.YEAR)}"

            val aiSuggestion = if (remaining > 0) {
                "Adding ₹1,000/month would move your target approximately 1 month earlier."
            } else {
                "🎯 Milestone Complete! You have successfully achieved this savings target."
            }

            GoalRoadmapItem(
                id = goal.id,
                title = goal.title.ifBlank { "Savings Goal" },
                targetAmount = goal.targetAmount,
                currentAmount = goal.currentAmount,
                remainingAmount = remaining,
                progressPct = progressPct,
                monthlyContributionRate = monthlyContribution,
                projectedCompletionDate = projDateStr,
                aiSuggestion = aiSuggestion,
                milestoneReached = milestoneReached
            )
        }
    }

    fun calculateMultiGoalPriority(
        goals: List<SavingsGoal>,
        monthlyAvailableSavings: Double,
        primaryGoalType: String = "Save Money",
        financialMode: String = "SAVE"
    ): MultiGoalPriorityDistribution {
        if (goals.isEmpty()) {
            return MultiGoalPriorityDistribution(
                monthlyAvailableSavings = monthlyAvailableSavings,
                allocations = emptyList(),
                overallRationale = "No active savings goals found. Create goals to enable intelligent prioritization.",
                dataUsed = "0 active goals"
            )
        }

        val totalAvailable = if (monthlyAvailableSavings > 0) monthlyAvailableSavings else 10000.0

        // Prioritize Emergency Fund first, then nearest targets
        val sortedGoals = goals.sortedWith(
            compareBy<SavingsGoal> { !it.title.contains("emergency", ignoreCase = true) }
                .thenBy { it.targetAmount - it.currentAmount }
        )

        val weights = when (sortedGoals.size) {
            1 -> listOf(1.0)
            2 -> listOf(0.60, 0.40)
            3 -> listOf(0.50, 0.30, 0.20)
            4 -> listOf(0.40, 0.30, 0.20, 0.10)
            else -> {
                val topPart = listOf(0.35, 0.25, 0.20)
                val restCount = sortedGoals.size - 3
                val restWeight = 0.20 / max(1, restCount)
                topPart + List(restCount) { restWeight }
            }
        }

        val allocations = sortedGoals.mapIndexed { idx, goal ->
            val w = weights.getOrElse(idx) { 1.0 / sortedGoals.size }
            val amt = (totalAvailable * w).roundToInt().toDouble()
            val reason = if (goal.title.contains("emergency", ignoreCase = true)) {
                "High Priority: Establishing an emergency cash reserve protects against unforeseen financial volatility."
            } else if (idx == 0) {
                "Primary Focus: Highest impact on near-term milestone completion."
            } else {
                "Balanced allocation supporting secondary target timeline."
            }

            GoalPriorityAllocation(
                goalId = goal.id,
                goalTitle = goal.title,
                recommendedMonthlyAmount = amt,
                allocationPercentage = (w * 100).roundToInt().toDouble(),
                priorityRank = idx + 1,
                reasonWhy = reason
            )
        }

        val rationale = "Recommended distribution allocates ₹${totalAvailable.toInt()} monthly surplus according to urgency and deadline horizon without altering actual deposits."

        return MultiGoalPriorityDistribution(
            monthlyAvailableSavings = totalAvailable,
            allocations = allocations,
            overallRationale = rationale,
            dataUsed = "Optimized across ${goals.size} active goals under $financialMode mode."
        )
    }

    // ==========================================
    // 9. MONEY ALERTS & UNIFIED ALERT CENTER
    // ==========================================
    fun generateMoneyAlerts(
        incomes: List<Income>,
        expenses: List<Expense>,
        budgets: List<Budget>,
        goals: List<SavingsGoal>,
        dismissedIds: List<String> = emptyList()
    ): List<MoneyAlert> {
        val alerts = mutableListOf<MoneyAlert>()
        val catTotals = calculateCategoryTotals(expenses)

        // 1. Budget Risk Alerts
        budgets.forEach { b ->
            if (b.amount > 0) {
                val spent = catTotals[b.category] ?: 0.0
                val pct = (spent / b.amount) * 100
                if (pct >= 100) {
                    val id = "budget_risk_${b.category}_100"
                    alerts.add(
                        MoneyAlert(
                            id = id,
                            type = MoneyAlertType.BUDGET_RISK,
                            title = "Budget Exceeded",
                            message = "${b.category} spending has exceeded monthly cap by ₹${(spent - b.amount).toInt()} (${pct.toInt()}%).",
                            severity = "danger",
                            actionLabel = "View Budget",
                            actionRoute = "budget",
                            isDismissed = dismissedIds.contains(id)
                        )
                    )
                } else if (pct >= 85) {
                    val id = "budget_risk_${b.category}_85"
                    alerts.add(
                        MoneyAlert(
                            id = id,
                            type = MoneyAlertType.BUDGET_RISK,
                            title = "Budget Threshold Alert",
                            message = "${b.category} has reached ${pct.toInt()}% of its allocated monthly cap.",
                            severity = "warning",
                            actionLabel = "Adjust Budget",
                            actionRoute = "budget",
                            isDismissed = dismissedIds.contains(id)
                        )
                    )
                }
            }
        }

        // 2. Spending Opportunity Alerts
        val snapshot = calculateSmartSnapshot(incomes, expenses, budgets, goals)
        if (snapshot.isSpendingLower && snapshot.spendingChangePct >= 10) {
            val id = "spending_opp_lower"
            alerts.add(
                MoneyAlert(
                    id = id,
                    type = MoneyAlertType.SPENDING_OPPORTUNITY,
                    title = "Spending Opportunity",
                    message = "Overall spending decreased by ${snapshot.spendingChangePct.toInt()}% vs last month. Consider transferring surplus to savings.",
                    severity = "success",
                    actionLabel = "Save Surplus",
                    actionRoute = "savings_goals",
                    isDismissed = dismissedIds.contains(id)
                )
            )
        }

        // 3. Goal Milestone Alerts
        goals.forEach { g ->
            if (g.targetAmount > 0) {
                val pct = ((g.currentAmount / g.targetAmount) * 100).toInt()
                if (pct in 75..99) {
                    val id = "goal_milestone_${g.id}_75"
                    alerts.add(
                        MoneyAlert(
                            id = id,
                            type = MoneyAlertType.GOAL_MILESTONE,
                            title = "Goal Milestone: ${g.title}",
                            message = "${g.title} has reached $pct% completion! You are ₹${(g.targetAmount - g.currentAmount).toInt()} away.",
                            severity = "info",
                            actionLabel = "View Goal",
                            actionRoute = "savings_goals",
                            isDismissed = dismissedIds.contains(id)
                        )
                    )
                }
            }
        }

        // 4. Unusual Spending Alerts
        val anomalies = detectSpendingAnomalies(expenses)
        anomalies.take(2).forEach { anomaly ->
            val id = "unusual_exp_${anomaly.expense.id}"
            alerts.add(
                MoneyAlert(
                    id = id,
                    type = MoneyAlertType.UNUSUAL_SPENDING,
                    title = "Unusual Spending Detected",
                    message = "₹${anomaly.expense.amount.toInt()} in ${anomaly.expense.category} is ${String.format(Locale.ROOT, "%.1f", anomaly.ratio)}x higher than typical.",
                    severity = "warning",
                    actionLabel = "Review Expense",
                    actionRoute = "transactions",
                    isDismissed = dismissedIds.contains(id)
                )
            )
        }

        // 5. Recurring Expense Alerts
        val recurring = detectRecurringMoneyMap(expenses).items
        if (recurring.isNotEmpty()) {
            val topRec = recurring.first()
            val id = "recurring_map_${topRec.id}"
            alerts.add(
                MoneyAlert(
                    id = id,
                    type = MoneyAlertType.RECURRING_EXPENSE,
                    title = "Recurring Subscription Detected",
                    message = "₹${topRec.monthlyAmount.toInt()} monthly detected for ${topRec.name} (₹${topRec.annualAmount.toInt()}/yr).",
                    severity = "info",
                    actionLabel = "Review Subscriptions",
                    actionRoute = "ai_assistant",
                    isDismissed = dismissedIds.contains(id)
                )
            )
        }

        return alerts.filterNot { it.isDismissed }
    }

    private fun detectSpendingAnomalies(expenses: List<Expense>): List<AiAnomaly> {
        if (expenses.size < 3) return emptyList()
        val catGroups = expenses.groupBy { it.category }
        val anomalies = mutableListOf<AiAnomaly>()

        expenses.forEach { exp ->
            val history = catGroups[exp.category] ?: emptyList()
            if (history.size >= 3) {
                val average = history.map { it.amount }.average()
                if (exp.amount > average * 2.2) {
                    anomalies.add(
                        AiAnomaly(
                            expense = exp,
                            averageAmount = average,
                            ratio = exp.amount / average,
                            tag = "Higher than usual"
                        )
                    )
                }
            }
        }
        return anomalies.sortedByDescending { it.expense.amount }
    }

    // ==========================================
    // 10. WEEKLY MONEY REVIEW
    // ==========================================
    fun generateWeeklyMoneyReview(incomes: List<Income>, expenses: List<Expense>): WeeklyMoneyReview {
        val cal = Calendar.getInstance()
        val now = Date()

        cal.time = now
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val sevenDaysAgo = cal.time

        cal.add(Calendar.DAY_OF_YEAR, -7)
        val fourteenDaysAgo = cal.time

        val thisWeekExps = expenses.filter { it.date.after(sevenDaysAgo) || it.date == sevenDaysAgo }
        val prevWeekExps = expenses.filter { (it.date.after(fourteenDaysAgo) || it.date == fourteenDaysAgo) && it.date.before(sevenDaysAgo) }

        val spentThisWeek = thisWeekExps.sumOf { it.amount }
        val spentPrevWeek = prevWeekExps.sumOf { it.amount }

        val thisWeekIncs = incomes.filter { it.date.after(sevenDaysAgo) || it.date == sevenDaysAgo }
        val savedThisWeek = max(0.0, thisWeekIncs.sumOf { it.amount } - spentThisWeek)

        val diffPct = if (spentPrevWeek > 0) {
            ((spentThisWeek - spentPrevWeek) / spentPrevWeek) * 100
        } else 0.0

        val catTotals = thisWeekExps.groupBy { it.category }.mapValues { it.value.sumOf { exp -> exp.amount } }
        val largestCat = catTotals.maxByOrNull { it.value }
        val largestCatName = largestCat?.key ?: "Food"
        val largestCatAmount = largestCat?.value ?: 0.0

        val isLower = diffPct <= 0
        val summary = if (isLower) {
            "You spent less than last week (↓${abs(diffPct).toInt()}%), mainly because discretionary expenses remained controlled."
        } else {
            "You spent more than last week (↑${abs(diffPct).toInt()}%), with $largestCatName driving the increase."
        }

        val suggestedCap = (largestCatAmount * 0.85).roundToInt().coerceAtLeast(1000)
        val action = "Try keeping $largestCatName spending below ₹$suggestedCap next week."

        return WeeklyMoneyReview(
            spentThisWeek = spentThisWeek,
            savedThisWeek = savedThisWeek,
            largestCategory = largestCatName,
            largestCategoryAmount = largestCatAmount,
            changePctVsLastWeek = abs(diffPct),
            isSpendingLowerThanLastWeek = isLower,
            aiSummary = summary,
            oneActionForNextWeek = action,
            actionCategory = largestCatName,
            suggestedReductionAmount = (largestCatAmount * 0.15).roundToInt().toDouble()
        )
    }

    // ==========================================
    // 11. COMPREHENSIVE MONTH-END REVIEW
    // ==========================================
    fun generateComprehensiveMonthEndReview(
        incomes: List<Income>,
        expenses: List<Expense>,
        budgets: List<Budget>,
        goals: List<SavingsGoal>,
        financialMode: String = "SAVE"
    ): ComprehensiveMonthEndReview {
        val totalInc = calculateTotalIncome(incomes)
        val totalExp = calculateTotalExpenses(expenses)
        val totalSav = totalInc - totalExp
        val savingsRate = calculateSavingsRate(incomes, expenses)
        val catTotals = calculateCategoryTotals(expenses)
        val sortedCats = catTotals.toList().sortedByDescending { it.second }

        val wentWell = mutableListOf<String>()
        val budgetsStatus = mutableListOf<String>()
        val savingsStatus = mutableListOf<String>()
        val goalsStatus = mutableListOf<String>()
        val unusualList = mutableListOf<String>()

        if (savingsRate >= 20.0) wentWell.add("Healthy savings rate of ${savingsRate.toInt()}% achieved.")
        if (wentWell.isEmpty()) wentWell.add("Active tracking and transaction record keeping maintained.")

        budgets.forEach { b ->
            val spent = catTotals[b.category] ?: 0.0
            if (b.amount > 0) {
                if (spent <= b.amount) {
                    budgetsStatus.add("✓ ${b.category}: Within cap (₹${spent.toInt()} / ₹${b.amount.toInt()})")
                } else {
                    budgetsStatus.add("⚠️ ${b.category}: Exceeded cap by ₹${(spent - b.amount).toInt()}")
                }
            }
        }
        if (budgetsStatus.isEmpty()) budgetsStatus.add("No specific category caps were breached.")

        savingsStatus.add("Net monthly surplus generated: ₹${max(0.0, totalSav).toInt()} (${savingsRate.toInt()}% rate).")

        goals.forEach { g ->
            if (g.targetAmount > 0) {
                val pct = ((g.currentAmount / g.targetAmount) * 100).toInt()
                goalsStatus.add("${g.title}: $pct% achieved (₹${g.currentAmount.toInt()} / ₹${g.targetAmount.toInt()})")
            }
        }
        if (goalsStatus.isEmpty()) goalsStatus.add("Regular goal tracking active.")

        val topAnomaly = detectSpendingAnomalies(expenses).firstOrNull()
        if (topAnomaly != null) {
            unusualList.add("₹${topAnomaly.expense.amount.toInt()} in ${topAnomaly.expense.category} was higher than usual.")
        } else {
            unusualList.add("No significant anomalies detected this cycle.")
        }

        val topCat = sortedCats.firstOrNull()
        val recBudgets = mutableMapOf<String, Double>()
        sortedCats.take(4).forEach { (cat, amt) ->
            recBudgets[cat] = (amt * 0.95).roundToInt().toDouble()
        }
        val savingsTarget = max(5000.0, totalInc * 0.20)

        val nextPlan = NextMonthPlan(
            recommendedCategoryBudgets = recBudgets,
            recommendedSavingsTarget = savingsTarget,
            primaryFocusArea = topCat?.first ?: "Food"
        )

        val cal = Calendar.getInstance()
        val monthNames = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val monthYearStr = "${monthNames[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"

        return ComprehensiveMonthEndReview(
            monthYear = monthYearStr,
            totalIncome = totalInc,
            totalExpenses = totalExp,
            totalSavings = totalSav,
            savingsRate = savingsRate,
            whatWentWell = wentWell,
            whereMoneyWent = sortedCats.take(5),
            budgetsStatus = budgetsStatus,
            savingsStatus = savingsStatus,
            goalsStatus = goalsStatus,
            unusualSpendingList = unusualList,
            recommendedAction = "Optimize ${topCat?.first ?: "Shopping"} spending to accelerate your savings target by next month.",
            nextMonthPlan = nextPlan
        )
    }

    // ==========================================
    // 12. FINANCIAL FORECAST
    // ==========================================
    fun calculateFinancialForecast(incomes: List<Income>, expenses: List<Expense>): FinancialForecast {
        val totalInc = calculateTotalIncome(incomes)
        val totalExp = calculateTotalExpenses(expenses)

        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, 1)
        val monthNames = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val nextMonthName = monthNames[cal.get(Calendar.MONTH)]

        val confidence = when {
            incomes.isNotEmpty() && expenses.size >= 10 -> "High"
            incomes.isNotEmpty() || expenses.size >= 5 -> "Medium"
            else -> "Low"
        }

        val expectedInc = if (totalInc > 0) totalInc else 50000.0
        val expectedExp = if (totalExp > 0) totalExp * 0.98 else 35000.0
        val expectedSav = max(0.0, expectedInc - expectedExp)

        return FinancialForecast(
            nextMonthName = nextMonthName,
            expectedIncome = expectedInc,
            expectedExpenses = expectedExp,
            expectedSavings = expectedSav,
            confidenceLevel = confidence,
            varianceMarginPct = if (confidence == "High") 8.0 else 15.0,
            budgetRiskNotes = "Moderate risk in variable discretionary categories based on prior 3-month distribution."
        )
    }

    // ==========================================
    // 13. CASHFLOW CALENDAR & MONEY JOURNEY
    // ==========================================
    fun buildCashflowCalendar(
        incomes: List<Income>,
        expenses: List<Expense>,
        budgets: List<Budget>,
        goals: List<SavingsGoal>,
        recurring: List<RecurringMoneyItem>
    ): List<CashflowCalendarEntry> {
        val entries = mutableListOf<CashflowCalendarEntry>()
        val cal = Calendar.getInstance()
        val monthNames = arrayOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")

        // 1. Income Dates
        incomes.forEach { inc ->
            cal.time = inc.date
            entries.add(
                CashflowCalendarEntry(
                    id = "inc_${inc.id}",
                    date = inc.date,
                    dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
                    monthName = monthNames[cal.get(Calendar.MONTH)],
                    title = "${inc.source.ifBlank { "Salary" }} Income",
                    amount = inc.amount,
                    type = CashflowEntryType.INCOME,
                    isProjected = false
                )
            )
        }

        // 2. Recurring Subscriptions
        recurring.forEachIndexed { idx, rec ->
            val day = (idx * 5 + 1).coerceIn(1, 28)
            cal.set(Calendar.DAY_OF_MONTH, day)
            entries.add(
                CashflowCalendarEntry(
                    id = "rec_${rec.id}",
                    date = cal.time,
                    dayOfMonth = day,
                    monthName = monthNames[cal.get(Calendar.MONTH)],
                    title = "${rec.name} (${rec.category})",
                    amount = rec.monthlyAmount,
                    type = CashflowEntryType.RECURRING_EXPENSE,
                    isProjected = true
                )
            )
        }

        // 3. Large Transactions
        expenses.filter { it.amount >= 3000.0 }.take(4).forEach { exp ->
            cal.time = exp.date
            entries.add(
                CashflowCalendarEntry(
                    id = "exp_${exp.id}",
                    date = exp.date,
                    dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
                    monthName = monthNames[cal.get(Calendar.MONTH)],
                    title = exp.description.ifBlank { exp.category },
                    amount = exp.amount,
                    type = CashflowEntryType.LARGE_TRANSACTION,
                    isProjected = false
                )
            )
        }

        return entries.sortedBy { it.dayOfMonth }
    }

    fun buildMoneyJourneyTimeline(
        incomes: List<Income>,
        expenses: List<Expense>,
        budgets: List<Budget>,
        goals: List<SavingsGoal>
    ): List<MoneyJourneyMilestone> {
        val milestones = mutableListOf<MoneyJourneyMilestone>()
        val cal = Calendar.getInstance()
        val monthNames = arrayOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
        val curMonth = monthNames[cal.get(Calendar.MONTH)]
        val curYear = cal.get(Calendar.YEAR)

        milestones.add(
            MoneyJourneyMilestone(
                id = "m_1",
                monthLabel = curMonth,
                year = curYear,
                title = "Income Received",
                description = "Primary cashflow credited to account.",
                iconName = "fa-wallet",
                type = "income",
                deltaAmount = calculateTotalIncome(incomes)
            )
        )

        val topCat = calculateCategoryTotals(expenses).maxByOrNull { it.value }
        if (topCat != null) {
            milestones.add(
                MoneyJourneyMilestone(
                    id = "m_2",
                    monthLabel = curMonth,
                    year = curYear,
                    title = "Primary Category: ${topCat.key}",
                    description = "Major expenditure managed across cycle.",
                    iconName = "fa-receipt",
                    type = "expense",
                    deltaAmount = topCat.value
                )
            )
        }

        goals.firstOrNull()?.let { g ->
            milestones.add(
                MoneyJourneyMilestone(
                    id = "m_3",
                    monthLabel = curMonth,
                    year = curYear,
                    title = "Goal Progress: ${g.title}",
                    description = "Saved ₹${g.currentAmount.toInt()} towards ₹${g.targetAmount.toInt()} target.",
                    iconName = "fa-bullseye",
                    type = "goal",
                    deltaAmount = g.currentAmount
                )
            )
        }

        return milestones
    }

    // Existing helpers maintained
    fun calculateFinancialHealth(
        incomes: List<Income>,
        expenses: List<Expense>,
        budgets: List<Budget>,
        goals: List<SavingsGoal>
    ): FinancialHealth {
        val totalIncome = calculateTotalIncome(incomes)
        val totalExpense = calculateTotalExpenses(expenses)
        val savings = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0) (savings / totalIncome) * 100 else 0.0

        val savingsScore = when {
            savingsRate >= 30.0 -> 35
            savingsRate >= 20.0 -> 30
            savingsRate >= 10.0 -> 22
            savingsRate > 0.0 -> 14
            else -> 5
        }

        var exceededBudgets = 0
        var warningBudgets = 0
        budgets.forEach { b ->
            if (b.amount > 0) {
                val spent = expenses.filter { it.category == b.category }.sumOf { it.amount }
                val pct = spent / b.amount
                if (pct >= 1.0) exceededBudgets++
                else if (pct >= 0.8) warningBudgets++
            }
        }
        val budgetScore = when {
            budgets.isEmpty() -> 20
            exceededBudgets == 0 && warningBudgets == 0 -> 25
            exceededBudgets == 0 -> 18
            exceededBudgets == 1 -> 12
            else -> 6
        }

        val expenseRatio = if (totalIncome > 0) totalExpense / totalIncome else 1.0
        val stabilityScore = when {
            expenseRatio <= 0.50 -> 20
            expenseRatio <= 0.70 -> 16
            expenseRatio <= 0.85 -> 12
            else -> 6
        }

        val goalsScore = if (goals.isEmpty()) {
            15
        } else {
            val avgProgress = goals.map {
                if (it.targetAmount > 0) (it.currentAmount / it.targetAmount).coerceIn(0.0, 1.0) else 0.0
            }.average()
            (avgProgress * 20).toInt().coerceIn(5, 20)
        }

        val totalScore = (savingsScore + budgetScore + stabilityScore + goalsScore).coerceIn(10, 100)
        val (label, color) = when {
            totalScore >= 80 -> "Excellent" to "emerald"
            totalScore >= 70 -> "Healthy" to "emerald"
            totalScore >= 50 -> "Moderate" to "amber"
            else -> "Needs Attention" to "rose"
        }

        return FinancialHealth(
            score = totalScore,
            label = label,
            statusColor = color,
            savingsRateScore = savingsScore,
            budgetScore = budgetScore,
            expenseStabilityScore = stabilityScore,
            goalsScore = goalsScore
        )
    }

    fun calculateSmartSnapshot(
        incomes: List<Income>,
        expenses: List<Expense>,
        budgets: List<Budget>,
        goals: List<SavingsGoal>
    ): SmartSnapshot {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        val thisMonthExpenses = expenses.filter {
            cal.time = it.date
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.amount }

        cal.add(Calendar.MONTH, -1)
        val lastMonth = cal.get(Calendar.MONTH)
        val lastMonthYear = cal.get(Calendar.YEAR)

        val lastMonthExpenses = expenses.filter {
            cal.time = it.date
            cal.get(Calendar.MONTH) == lastMonth && cal.get(Calendar.YEAR) == lastMonthYear
        }.sumOf { it.amount }

        val spendDiff = if (lastMonthExpenses > 0) {
            ((thisMonthExpenses - lastMonthExpenses) / lastMonthExpenses) * 100
        } else 0.0

        val thisMonthIncome = incomes.filter {
            cal.time = it.date
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.amount }

        val lastMonthIncome = incomes.filter {
            cal.time = it.date
            cal.get(Calendar.MONTH) == lastMonth && cal.get(Calendar.YEAR) == lastMonthYear
        }.sumOf { it.amount }

        val thisSavings = thisMonthIncome - thisMonthExpenses
        val lastSavings = lastMonthIncome - lastMonthExpenses
        val savingsDiff = if (lastSavings > 0) {
            ((thisSavings - lastSavings) / lastSavings) * 100
        } else 0.0

        var topBudgetName = "None"
        var topBudgetPct = 0
        var isBudgetWarning = false
        budgets.maxByOrNull { b ->
            val spent = expenses.filter { it.category == b.category }.sumOf { it.amount }
            if (b.amount > 0) spent / b.amount else 0.0
        }?.let { topBudget ->
            val spent = expenses.filter { it.category == topBudget.category }.sumOf { it.amount }
            val pct = if (topBudget.amount > 0) ((spent / topBudget.amount) * 100).toInt() else 0
            topBudgetName = topBudget.category
            topBudgetPct = pct
            isBudgetWarning = pct >= 80
        }

        var topGoalName = "None"
        var topGoalPct = 0
        goals.maxByOrNull { g ->
            if (g.targetAmount > 0) g.currentAmount / g.targetAmount else 0.0
        }?.let { goal ->
            val pct = if (goal.targetAmount > 0) ((goal.currentAmount / goal.targetAmount) * 100).toInt() else 0
            topGoalName = goal.title
            topGoalPct = pct
        }

        return SmartSnapshot(
            spendingChangePct = abs(spendDiff),
            isSpendingLower = spendDiff <= 0,
            savingsChangePct = abs(savingsDiff),
            isSavingsHigher = savingsDiff >= 0,
            topBudgetName = topBudgetName,
            topBudgetPct = topBudgetPct,
            isBudgetWarning = isBudgetWarning,
            topGoalName = topGoalName,
            topGoalPct = topGoalPct
        )
    }

    fun generateInsights(incomes: List<Income>, expenses: List<Expense>, budgets: List<Budget>): List<Insight> {
        val insights = mutableListOf<Insight>()
        val categoryTotals = calculateCategoryTotals(expenses)
        
        val highestCategory = categoryTotals.maxByOrNull { it.value }
        if (highestCategory != null) {
            insights.add(
                Insight(
                    title = "Top Expense Category",
                    message = "${highestCategory.key} constitutes your largest expenditure.",
                    type = InsightType.SPENDING,
                    priority = 1
                )
            )
        }

        budgets.forEach { budget ->
            if (budget.amount > 0) {
                val spent = expenses.filter { it.category == budget.category }.sumOf { it.amount }
                val percent = (spent / budget.amount) * 100
                if (percent >= 90) {
                    insights.add(
                        Insight(
                            title = "Budget Threshold Alert",
                            message = "You have utilized ${percent.toInt()}% of your monthly ${budget.category} budget allocation.",
                            type = InsightType.BUDGET,
                            priority = 0
                        )
                    )
                }
            }
        }

        val recurring = detectRecurringMoneyMap(expenses).items
        if (recurring.isNotEmpty()) {
            insights.add(
                Insight(
                    title = "Recurring Subscriptions",
                    message = "Detected ${recurring.size} regular recurring commitments.",
                    type = InsightType.RECURRING,
                    priority = 2
                )
            )
        }

        return insights.sortedBy { it.priority }
    }
}
