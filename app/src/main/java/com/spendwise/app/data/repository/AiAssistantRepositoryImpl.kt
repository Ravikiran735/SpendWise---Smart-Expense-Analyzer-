package com.spendwise.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.spendwise.app.domain.model.*
import com.spendwise.app.domain.repository.AiAssistantRepository
import com.spendwise.app.utils.AnalysisEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

class AiAssistantRepositoryImpl(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AiAssistantRepository {

    private val userId: String
        get() = auth.currentUser?.uid ?: ""

    private val backendUrl = "http://10.0.2.2:5000/api/ai"

    override fun getConversationHistory(): Flow<List<AiMessage>> = callbackFlow {
        var listener: ListenerRegistration? = null

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            listener?.remove()
            listener = null

            if (uid.isNullOrBlank()) {
                trySend(emptyList())
            } else {
                listener = firestore.collection("users").document(uid)
                    .collection("aiConversations")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            return@addSnapshotListener
                        }

                        val messages = snapshot?.documents?.mapNotNull { doc ->
                            val senderStr = doc.getString("sender") ?: "AI"
                            val sender = if (senderStr.equals("user", ignoreCase = true)) AiMessageSender.USER else AiMessageSender.AI
                            val text = doc.getString("text") ?: ""
                            val timestamp = doc.safeDate("timestamp")
                            AiMessage(
                                id = doc.id,
                                sender = sender,
                                text = text,
                                timestamp = timestamp
                            )
                        } ?: emptyList()

                        trySend(messages)
                    }
            }
        }

        auth.addAuthStateListener(authListener)

        awaitClose {
            auth.removeAuthStateListener(authListener)
            listener?.remove()
        }
    }

    override suspend fun sendMessage(messageText: String): AiMessage = withContext(Dispatchers.IO) {
        val currentUid = auth.currentUser?.uid ?: ""

        // 1. Fetch user financial context safely on IO dispatcher
        val expenses = fetchUserExpenses()
        val incomes = fetchUserIncomes()
        val budgets = fetchUserBudgets()
        val goals = fetchUserGoals()
        val settings = fetchUserSettings()

        // 2. Persist user message to Firestore if logged in
        if (currentUid.isNotBlank()) {
            try {
                val convRef = firestore.collection("users").document(currentUid).collection("aiConversations")
                val userMsgMap = hashMapOf(
                    "sender" to "user",
                    "text" to messageText,
                    "timestamp" to Date()
                )
                convRef.add(userMsgMap)
            } catch (e: Exception) {
                // Non-blocking firestore sync
            }
        }

        // 3. Attempt server AI call with quick timeout; fallback gracefully to client intelligence engine
        var aiText: String? = null
        if (currentUid.isNotBlank()) {
            try {
                withTimeoutOrNull(2000L) {
                    val token = auth.currentUser?.getIdToken(false)?.await()?.token ?: "android_token"
                    aiText = callBackendAiChat(messageText, token, expenses, incomes, settings)
                }
            } catch (e: Exception) {
                aiText = null
            }
        }

        val finalAiText: String = if (!aiText.isNullOrBlank()) {
            aiText!!
        } else {
            generateDeterministicChatResponse(messageText, expenses, incomes, budgets, goals, settings)
        }

        // 4. Persist AI response to Firestore if logged in
        var generatedId = UUID.randomUUID().toString()
        if (currentUid.isNotBlank()) {
            try {
                val convRef = firestore.collection("users").document(currentUid).collection("aiConversations")
                val aiMsgMap = hashMapOf(
                    "sender" to "ai",
                    "text" to finalAiText,
                    "timestamp" to Date()
                )
                val doc = convRef.add(aiMsgMap).await()
                generatedId = doc.id
            } catch (e: Exception) {
                // Non-blocking firestore sync
            }
        }

        AiMessage(
            id = generatedId,
            sender = AiMessageSender.AI,
            text = finalAiText,
            timestamp = Date()
        )
    }

    override suspend fun clearConversation(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = userId
            if (uid.isBlank()) return@runCatching
            val snapshot = firestore.collection("users").document(uid)
                .collection("aiConversations").get().await()
            val batch = firestore.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
    }

    override suspend fun getSpendingAnalysis(): AiSpendingAnalysis = withContext(Dispatchers.IO) {
        val expenses = fetchUserExpenses()
        val incomes = fetchUserIncomes()

        val totalIncome = AnalysisEngine.calculateTotalIncome(incomes)
        val totalExpenses = AnalysisEngine.calculateTotalExpenses(expenses)
        val netSavings = totalIncome - totalExpenses
        val savingsRate = AnalysisEngine.calculateSavingsRate(incomes, expenses)
        val categoryTotals = AnalysisEngine.calculateCategoryTotals(expenses)
        val sortedCats = categoryTotals.toList().sortedByDescending { it.second }

        val explanation = when {
            totalIncome == 0.0 && totalExpenses == 0.0 ->
                "Not enough transaction history yet. Record your expenses and income to see personalized Copilot recommendations."
            savingsRate >= 30.0 ->
                "Your financial position is exceptionally healthy this month. You saved approximately ${savingsRate.toInt()}% of your income. Discretionary spending is well balanced."
            savingsRate >= 15.0 ->
                "Your financial position is healthy this month. You saved approximately ${savingsRate.toInt()}% of your income. ${sortedCats.firstOrNull()?.first ?: "Shopping"} is your largest expenditure."
            savingsRate > 0.0 ->
                "You have a positive savings rate of ${String.format(Locale.ROOT, "%.1f", savingsRate)}%, but primary categories are consuming most of your monthly cashflow."
            else ->
                "Your monthly expenditures currently match or exceed your recorded income. Immediate budget discipline is recommended."
        }

        AiSpendingAnalysis(
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netSavings = netSavings,
            savingsRate = savingsRate,
            explanation = explanation,
            topCategories = sortedCats.take(5)
        )
    }

    override suspend fun getBudgetRecommendations(): List<AiBudgetRecommendation> = withContext(Dispatchers.IO) {
        val expenses = fetchUserExpenses()
        val budgets = fetchUserBudgets()
        val catTotals = AnalysisEngine.calculateCategoryTotals(expenses)

        catTotals.map { (category, total) ->
            val existing = budgets.find { it.category.equals(category, ignoreCase = true) }
            val currentCap = existing?.amount ?: 0.0
            val recMin = ((total * 0.95) / 100).roundToInt() * 100.0
            val recMax = ((total * 1.10) / 100).roundToInt() * 100.0
            val recommended = ((total * 1.0) / 100).roundToInt() * 100.0

            val reason = if (currentCap > 0 && total > currentCap) {
                "You exceeded your previous cap (₹${currentCap.toInt()}). Aligning with recent spending prevents budget alerts."
            } else {
                "Your recent spending consistently falls within this range."
            }

            AiBudgetRecommendation(
                category = category,
                averageSpending = total,
                currentBudget = currentCap,
                recommendedAmount = recommended,
                recommendedRange = "₹${recMin.toInt()} – ₹${recMax.toInt()}",
                reason = reason
            )
        }
    }

    override suspend fun calculateSavingsPlan(targetAmount: Double, targetMonths: Int): AiSavingsPlan = withContext(Dispatchers.IO) {
        val goals = fetchUserGoals()

        val currentSavings = goals.sumOf { it.currentAmount }
        val remaining = max(0.0, targetAmount - currentSavings)

        val duration = if (targetMonths > 0) targetMonths else 6
        val suggestedMonthly = if (duration > 0) (remaining / duration).roundToInt().toDouble() else (remaining / 6).roundToInt().toDouble()

        AiSavingsPlan(
            targetAmount = targetAmount,
            currentSavings = currentSavings,
            remainingAmount = remaining,
            suggestedMonthlySaving = suggestedMonthly,
            estimatedDurationMonths = duration,
            explanation = "To achieve your target of ₹${targetAmount.toInt()}, contributing ₹${suggestedMonthly.toInt()} monthly over ~$duration months is projected based on your cashflow. (Estimate only)"
        )
    }

    override suspend fun getSpendingAnomalies(): List<AiAnomaly> = withContext(Dispatchers.IO) {
        val expenses = fetchUserExpenses()
        if (expenses.size < 3) return@withContext emptyList()

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
        anomalies.sortedByDescending { it.expense.amount }
    }

    override suspend fun getMonthlyReview(): AiMonthlyReview = withContext(Dispatchers.IO) {
        val expenses = fetchUserExpenses()
        val incomes = fetchUserIncomes()
        val budgets = fetchUserBudgets()
        val snapshot = AnalysisEngine.calculateSmartSnapshot(incomes, expenses, budgets, emptyList())

        val totalIncome = AnalysisEngine.calculateTotalIncome(incomes)
        val totalExpenses = AnalysisEngine.calculateTotalExpenses(expenses)
        val netSavings = totalIncome - totalExpenses
        val savingsRate = AnalysisEngine.calculateSavingsRate(incomes, expenses)
        val catTotals = AnalysisEngine.calculateCategoryTotals(expenses)

        val wentWell = mutableListOf<String>()
        val watchOut = mutableListOf<String>()
        val recs = mutableListOf<String>()

        if (savingsRate >= 20.0) {
            wentWell.add("Strong savings rate of ${savingsRate.toInt()}% achieved this month.")
        }
        if (snapshot.isSpendingLower && snapshot.spendingChangePct > 0) {
            wentWell.add("Overall expenses decreased by ${snapshot.spendingChangePct.toInt()}% vs last month.")
        }
        if (wentWell.isEmpty()) {
            wentWell.add("Regular transaction logging maintained across active categories.")
        }

        budgets.forEach { b ->
            val spent = catTotals[b.category] ?: 0.0
            if (b.amount > 0 && spent > b.amount) {
                watchOut.add("${b.category} exceeded its monthly budget cap by ₹${(spent - b.amount).toInt()}.")
            }
        }

        if (watchOut.isEmpty()) {
            watchOut.add("All monitored category budgets stayed within planned boundaries.")
        }

        val topCat = catTotals.maxByOrNull { it.value }
        if (topCat != null) {
            val cut = (topCat.value * 0.12).toInt()
            recs.add("Optimizing ${topCat.key} by ₹$cut next month could accelerate your savings.")
        }
        recs.add("Consider setting up dedicated monthly budget caps for variable discretionary purchases.")

        AiMonthlyReview(
            income = totalIncome,
            expenses = totalExpenses,
            savings = netSavings,
            savingsRate = savingsRate,
            whatWentWell = wentWell,
            watchOut = watchOut,
            recommendations = recs
        )
    }

    override suspend fun getHealthScoreExplanation(healthScore: Int): String = withContext(Dispatchers.IO) {
        val expenses = fetchUserExpenses()
        val incomes = fetchUserIncomes()
        val rate = AnalysisEngine.calculateSavingsRate(incomes, expenses).toInt()

        when {
            healthScore >= 80 -> "Your score is strong ($healthScore/100) because your savings rate is $rate% and most of your budgets remain within their limits."
            healthScore >= 70 -> "Your score is healthy ($healthScore/100) reflecting positive savings discipline ($rate%), though minor category overruns exist."
            healthScore >= 50 -> "Your score is moderate ($healthScore/100). Discretionary spending consumes a significant portion of income. Setting tighter caps is advised."
            else -> "Your score ($healthScore/100) requires attention. Recorded expenses exceed or match income. Budget review is strongly recommended."
        }
    }

    override suspend fun applyBudgetRecommendation(recommendation: AiBudgetRecommendation): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = userId
            if (uid.isBlank()) return@runCatching
            val budgetsRef = firestore.collection("users").document(uid).collection("budgets")
            val existingSnap = budgetsRef.whereEqualTo("category", recommendation.category).get().await()

            if (!existingSnap.isEmpty) {
                val docId = existingSnap.documents.first().id
                budgetsRef.document(docId).update("amount", recommendation.recommendedAmount).await()
            } else {
                val newBudget = hashMapOf(
                    "userId" to uid,
                    "category" to recommendation.category,
                    "amount" to recommendation.recommendedAmount,
                    "spentAmount" to 0.0
                )
                budgetsRef.add(newBudget).await()
            }
        }
    }

    // ==========================================
    // Decision Intelligence Extensions
    // ==========================================
    override suspend fun simulateWhatIf(
        simulatedIncomeDelta: Double,
        simulatedCategoryDeltas: Map<String, Double>,
        simulatedRecurringDelta: Double
    ): WhatIfSimulationResult = withContext(Dispatchers.IO) {
        val incomes = fetchUserIncomes()
        val expenses = fetchUserExpenses()
        val budgets = fetchUserBudgets()
        val goals = fetchUserGoals()
        AnalysisEngine.calculateWhatIfSimulation(
            incomes = incomes,
            expenses = expenses,
            budgets = budgets,
            goals = goals,
            simulatedIncomeDelta = simulatedIncomeDelta,
            simulatedCategoryDeltas = simulatedCategoryDeltas,
            simulatedRecurringDelta = simulatedRecurringDelta
        )
    }

    override suspend fun assessPurchaseAffordability(
        amount: Double,
        description: String,
        category: String
    ): PurchaseAffordabilityResult = withContext(Dispatchers.IO) {
        val incomes = fetchUserIncomes()
        val expenses = fetchUserExpenses()
        val budgets = fetchUserBudgets()
        val goals = fetchUserGoals()
        AnalysisEngine.evaluatePurchaseAffordability(
            amount = amount,
            description = description,
            category = category,
            incomes = incomes,
            expenses = expenses,
            budgets = budgets,
            goals = goals
        )
    }

    override suspend fun getFinancialDigitalTwin(): FinancialDigitalTwin = withContext(Dispatchers.IO) {
        val incomes = fetchUserIncomes()
        val expenses = fetchUserExpenses()
        val budgets = fetchUserBudgets()
        val goals = fetchUserGoals()
        AnalysisEngine.calculateFinancialDigitalTwin(incomes, expenses, budgets, goals)
    }

    override suspend fun getMoneyHabitScore(): MoneyHabitScore = withContext(Dispatchers.IO) {
        val incomes = fetchUserIncomes()
        val expenses = fetchUserExpenses()
        val budgets = fetchUserBudgets()
        val goals = fetchUserGoals()
        val settings = fetchUserSettings()
        AnalysisEngine.calculateMoneyHabitScore(incomes, expenses, budgets, goals, settings)
    }

    override suspend fun getEssentialVsDiscretionary(): EssentialDiscretionaryAnalysis = withContext(Dispatchers.IO) {
        val expenses = fetchUserExpenses()
        val settings = fetchUserSettings()
        AnalysisEngine.calculateEssentialVsDiscretionary(expenses, settings.essentialOverrides)
    }

    override suspend fun getRecurringMoneyMap(): RecurringMoneyMap = withContext(Dispatchers.IO) {
        val expenses = fetchUserExpenses()
        val settings = fetchUserSettings()
        AnalysisEngine.detectRecurringMoneyMap(expenses, settings.recurringOverrides)
    }

    override suspend fun getSpendingLeaks(): List<SpendingLeak> = withContext(Dispatchers.IO) {
        val expenses = fetchUserExpenses()
        AnalysisEngine.detectSpendingLeaks(expenses)
    }

    override suspend fun getGoalRoadmap(): List<GoalRoadmapItem> = withContext(Dispatchers.IO) {
        val goals = fetchUserGoals()
        val incomes = fetchUserIncomes()
        val expenses = fetchUserExpenses()
        AnalysisEngine.generateGoalRoadmap(goals, incomes, expenses)
    }

    override suspend fun getMultiGoalPriority(): MultiGoalPriorityDistribution = withContext(Dispatchers.IO) {
        val goals = fetchUserGoals()
        val incomes = fetchUserIncomes()
        val expenses = fetchUserExpenses()
        val settings = fetchUserSettings()
        val totalInc = AnalysisEngine.calculateTotalIncome(incomes)
        val totalExp = AnalysisEngine.calculateTotalExpenses(expenses)
        val surplus = max(0.0, totalInc - totalExp)
        AnalysisEngine.calculateMultiGoalPriority(goals, surplus, settings.primaryGoal, settings.financialMode)
    }

    override suspend fun getMoneyAlerts(): List<MoneyAlert> = withContext(Dispatchers.IO) {
        val incomes = fetchUserIncomes()
        val expenses = fetchUserExpenses()
        val budgets = fetchUserBudgets()
        val goals = fetchUserGoals()
        val settings = fetchUserSettings()
        AnalysisEngine.generateMoneyAlerts(incomes, expenses, budgets, goals, settings.dismissedAlertIds)
    }

    override suspend fun getWeeklyMoneyReview(): WeeklyMoneyReview = withContext(Dispatchers.IO) {
        val incomes = fetchUserIncomes()
        val expenses = fetchUserExpenses()
        AnalysisEngine.generateWeeklyMoneyReview(incomes, expenses)
    }

    override suspend fun getComprehensiveMonthEndReview(): ComprehensiveMonthEndReview = withContext(Dispatchers.IO) {
        val incomes = fetchUserIncomes()
        val expenses = fetchUserExpenses()
        val budgets = fetchUserBudgets()
        val goals = fetchUserGoals()
        val settings = fetchUserSettings()
        AnalysisEngine.generateComprehensiveMonthEndReview(incomes, expenses, budgets, goals, settings.financialMode)
    }

    override suspend fun getFinancialForecast(): FinancialForecast = withContext(Dispatchers.IO) {
        val incomes = fetchUserIncomes()
        val expenses = fetchUserExpenses()
        AnalysisEngine.calculateFinancialForecast(incomes, expenses)
    }

    override suspend fun getCashflowCalendar(): List<CashflowCalendarEntry> = withContext(Dispatchers.IO) {
        val incomes = fetchUserIncomes()
        val expenses = fetchUserExpenses()
        val budgets = fetchUserBudgets()
        val goals = fetchUserGoals()
        val settings = fetchUserSettings()
        val recurring = AnalysisEngine.detectRecurringMoneyMap(expenses, settings.recurringOverrides).items
        AnalysisEngine.buildCashflowCalendar(incomes, expenses, budgets, goals, recurring)
    }

    override suspend fun getMoneyJourneyTimeline(): List<MoneyJourneyMilestone> = withContext(Dispatchers.IO) {
        val incomes = fetchUserIncomes()
        val expenses = fetchUserExpenses()
        val budgets = fetchUserBudgets()
        val goals = fetchUserGoals()
        AnalysisEngine.buildMoneyJourneyTimeline(incomes, expenses, budgets, goals)
    }

    // Helper Firestore queries with robust type-safe field parsing
    private fun com.google.firebase.firestore.DocumentSnapshot.safeDouble(field: String, defaultVal: Double = 0.0): Double {
        val obj = get(field) ?: return defaultVal
        return when (obj) {
            is Number -> obj.toDouble()
            is String -> obj.toDoubleOrNull() ?: defaultVal
            else -> defaultVal
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.safeDate(field: String): Date {
        val obj = get(field) ?: return Date()
        return when (obj) {
            is com.google.firebase.Timestamp -> obj.toDate()
            is Date -> obj
            is Number -> Date(obj.toLong())
            is String -> {
                try {
                    java.text.SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(obj) ?: Date()
                } catch (e: Exception) {
                    Date()
                }
            }
            else -> Date()
        }
    }

    private suspend fun fetchUserExpenses(): List<Expense> = withContext(Dispatchers.IO) {
        val uid = userId
        if (uid.isBlank()) return@withContext emptyList()
        try {
            val snap = firestore.collection("users").document(uid).collection("expenses").get().await()
            snap.documents.mapNotNull { doc ->
                Expense(
                    id = doc.id,
                    userId = doc.getString("userId") ?: uid,
                    amount = doc.safeDouble("amount"),
                    category = doc.getString("category") ?: "Other",
                    description = doc.getString("description") ?: "",
                    paymentMethod = doc.getString("paymentMethod") ?: "UPI",
                    date = doc.safeDate("date"),
                    isEssential = doc.getBoolean("isEssential")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchUserIncomes(): List<Income> = withContext(Dispatchers.IO) {
        val uid = userId
        if (uid.isBlank()) return@withContext emptyList()
        try {
            val snap = firestore.collection("users").document(uid).collection("incomes").get().await()
            val list = snap.documents.mapNotNull { doc ->
                Income(
                    id = doc.id,
                    userId = doc.getString("userId") ?: uid,
                    amount = doc.safeDouble("amount"),
                    source = doc.getString("source") ?: "Salary",
                    description = doc.getString("description") ?: "",
                    date = doc.safeDate("date")
                )
            }
            if (list.isNotEmpty()) {
                list
            } else {
                // Fallback: Check if monthly income is present in User document
                val userDoc = firestore.collection("users").document(uid).get().await()
                val monthlyInc = userDoc.safeDouble("monthlyIncome", 0.0)
                if (monthlyInc > 0.0) {
                    listOf(
                        Income(
                            id = "profile_monthly_income",
                            userId = uid,
                            amount = monthlyInc,
                            source = "Salary / Profile Income",
                            description = "Estimated Monthly Income",
                            date = Date()
                        )
                    )
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchUserBudgets(): List<Budget> = withContext(Dispatchers.IO) {
        val uid = userId
        if (uid.isBlank()) return@withContext emptyList()
        try {
            val snap = firestore.collection("users").document(uid).collection("budgets").get().await()
            snap.documents.mapNotNull { doc ->
                Budget(
                    id = doc.id,
                    userId = doc.getString("userId") ?: uid,
                    category = doc.getString("category") ?: "Other",
                    amount = doc.safeDouble("amount"),
                    spentAmount = doc.safeDouble("spentAmount")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchUserGoals(): List<SavingsGoal> = withContext(Dispatchers.IO) {
        val uid = userId
        if (uid.isBlank()) return@withContext emptyList()
        try {
            val snap = firestore.collection("users").document(uid).collection("savingsGoals").get().await()
            snap.documents.mapNotNull { doc ->
                SavingsGoal(
                    id = doc.id,
                    userId = doc.getString("userId") ?: uid,
                    title = doc.getString("title") ?: "Goal",
                    targetAmount = doc.safeDouble("targetAmount"),
                    currentAmount = doc.safeDouble("currentAmount"),
                    deadline = doc.safeDate("deadline")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchUserSettings(): UserSettings = withContext(Dispatchers.IO) {
        val uid = userId
        if (uid.isBlank()) return@withContext UserSettings()
        try {
            val doc = firestore.collection("users").document(uid).collection("settings").document("preferences").get().await()
            if (doc.exists()) doc.toObject(UserSettings::class.java) ?: UserSettings() else UserSettings()
        } catch (e: Exception) {
            UserSettings()
        }
    }

    private fun callBackendAiChat(
        prompt: String,
        token: String,
        expenses: List<Expense>,
        incomes: List<Income>,
        settings: UserSettings
    ): String? {
        val url = URL("$backendUrl/chat")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.connectTimeout = 2000
        conn.readTimeout = 2000
        conn.doOutput = true

        val totalIncome = AnalysisEngine.calculateTotalIncome(incomes)
        val totalExpenses = AnalysisEngine.calculateTotalExpenses(expenses)
        val catTotals = AnalysisEngine.calculateCategoryTotals(expenses)

        val contextJson = JSONObject().apply {
            put("currency", "₹")
            put("financialMode", settings.financialMode)
            put("primaryGoal", settings.primaryGoal)
            put("metrics", JSONObject().apply {
                put("totalIncome", totalIncome)
                put("totalExpense", totalExpenses)
                put("savings", totalIncome - totalExpenses)
                put("savingsRate", AnalysisEngine.calculateSavingsRate(incomes, expenses))
                put("categoryTotals", JSONObject(catTotals))
            })
        }

        val requestBody = JSONObject().apply {
            put("message", prompt)
            put("context", contextJson)
        }

        OutputStreamWriter(conn.outputStream).use { it.write(requestBody.toString()) }

        if (conn.responseCode == 200) {
            val responseStr = BufferedReader(InputStreamReader(conn.inputStream)).readText()
            val resObj = JSONObject(responseStr)
            return resObj.optString("response")
        }
        return null
    }

    private fun generateDeterministicChatResponse(
        query: String,
        expenses: List<Expense>,
        incomes: List<Income>,
        budgets: List<Budget>,
        goals: List<SavingsGoal>,
        settings: UserSettings
    ): String {
        val q = query.lowercase(Locale.ROOT)
        val totalIncome = AnalysisEngine.calculateTotalIncome(incomes)
        val totalExpenses = AnalysisEngine.calculateTotalExpenses(expenses)
        val savings = totalIncome - totalExpenses
        val savingsRate = AnalysisEngine.calculateSavingsRate(incomes, expenses)
        val catTotals = AnalysisEngine.calculateCategoryTotals(expenses)
        val sortedCats = catTotals.toList().sortedByDescending { it.second }

        return when {
            // Affordability Assessment
            q.contains("afford") || q.contains("can i buy") || q.contains("should i buy") -> {
                val digits = Regex("[0-9]+[0-9,]*").find(query)?.value?.replace(",", "")?.toDoubleOrNull() ?: 10000.0
                val result = AnalysisEngine.evaluatePurchaseAffordability(digits, "Inquired Purchase", "Shopping", incomes, expenses, budgets, goals)
                """### ⚖️ Affordability Verdict: ${result.verdictTitle}
${result.message}

**WHY?**
${result.explanationWhy}

**DATA USED:**
${result.dataUsedSummary}"""
            }

            // Spending Comparison
            q.contains("spend more") || q.contains("more this month") || q.contains("compare") || q.contains("increase") || q.contains("comparison") -> {
                val snapshot = AnalysisEngine.calculateSmartSnapshot(incomes, expenses, budgets, goals)
                val isLower = snapshot.isSpendingLower
                val change = snapshot.spendingChangePct.toInt()
                val topCatsStr = if (sortedCats.isNotEmpty()) sortedCats.take(2).joinToString(" and ") { "${it.first} (₹${it.second.toInt()})" } else "No recorded categories"
                val diffText = if (isLower) "Your expenses are **$change% lower** compared with the baseline." else "Your expenses **increased by $change%** compared with the baseline."
                """### 📊 Spending Comparison
$diffText

- **Top Active Spending Areas:** $topCatsStr
- **Total Monitored Outflow:** ₹${totalExpenses.toInt()}
- **Current Savings Rate:** ${savingsRate.toInt()}%

**WHY?**
Variations in primary categories like ${sortedCats.firstOrNull()?.first ?: "Shopping"} drive the majority of your cashflow changes.

**DATA USED:**
Analyzed ${expenses.size} transactions across active accounts."""
            }

            // Top Spending Categories
            q.contains("where am i spending") || q.contains("spending the most") || q.contains("top categor") || q.contains("highest spend") -> {
                if (sortedCats.isEmpty()) {
                    """### 📊 Spending Breakdown
No expenses recorded for this period yet. Once you log or import expenses, I will break down your highest spending categories here."""
                } else {
                    val breakdown = sortedCats.take(5).joinToString("\n") { (cat, amt) ->
                        val pct = if (totalExpenses > 0) ((amt / totalExpenses) * 100).toInt() else 0
                        "- **$cat:** ₹${amt.toInt()} ($pct%)"
                    }
                    """### 📊 Top Spending Categories
Here is where your money is currently going:

$breakdown

**💡 Insight:**
${sortedCats.first().first} represents your largest single expense category at ₹${sortedCats.first().second.toInt()}."""
                }
            }

            // Savings Strategies
            q.contains("save more") || q.contains("how can i save") || q.contains("reduce") || q.contains("cut") -> {
                val topCat = sortedCats.firstOrNull()
                val potential = if (topCat != null) (topCat.second * 0.15).toInt() else 1000
                """### 💡 Actionable Savings Strategy
You are currently saving approximately **${savingsRate.toInt()}%** of your monthly income.

1. **Target Discretionary Caps:** Reducing your ${topCat?.first ?: "discretionary"} spending by 15% could free up **~₹$potential every month**.
2. **Automate Savings First:** Allocate ₹${(totalIncome * 0.2).toInt()} to your primary goal at the beginning of each cycle.
3. **Audit Recurring Costs:** Check for unutilized subscriptions in the *Recurring Map* tab.

**WHY?**
${topCat?.first ?: "Shopping"} constitutes your highest expenditure category this cycle.

**DATA USED:**
Calculated from ₹${totalExpenses.toInt()} in recorded expenses under **${settings.financialMode}** mode."""
            }

            // What-If Simulation Query
            q.contains("simulate") || q.contains("what-if") || q.contains("what if") -> {
                val digits = Regex("[0-9]+[0-9,]*").find(query)?.value?.replace(",", "")?.toDoubleOrNull() ?: 2000.0
                val catName = if (q.contains("food") || q.contains("dining")) "Food" else if (q.contains("shopping")) "Shopping" else "Discretionary"
                val simResult = AnalysisEngine.calculateWhatIfSimulation(
                    incomes = incomes,
                    expenses = expenses,
                    budgets = budgets,
                    goals = goals,
                    simulatedIncomeDelta = 0.0,
                    simulatedCategoryDeltas = mapOf(catName to -digits),
                    simulatedRecurringDelta = 0.0
                )
                """### 🔮 What-If Simulation: Reduce $catName by ₹${digits.toInt()}
- **Current Monthly Savings:** ₹${simResult.currentMonthlySavings.toInt()} (${simResult.currentSavingsRate.toInt()}%)
- **Projected Monthly Savings:** ₹${simResult.projectedMonthlySavings.toInt()} (${simResult.projectedSavingsRate.toInt()}%)
- **Savings Improvement:** +${String.format(Locale.ROOT, "%.1f", simResult.savingsImprovementPct)}%

**GOAL IMPACT:**
${if (simResult.goalShifts.isNotEmpty()) simResult.goalShifts.joinToString("\n") { "• **${it.goalTitle}**: Accelerated by ${it.daysSavedOrDelayed} days!" } else "Helps reach your future goals faster."}

*Tip: You can customize full sliders in the **What-If** tab.*"""
            }

            // Budget inquiry
            q.contains("budget") || q.contains("limits") || q.contains("caps") -> {
                """### 🎯 Budget Plan (${settings.financialMode} Mode)
Based on your income of **₹${totalIncome.toInt()}**, the recommended 50/30/20 allocation is:
- **Needs (Essential 50%):** ₹${(totalIncome * 0.5).toInt()}
- **Wants (Discretionary 30%):** ₹${(totalIncome * 0.3).toInt()}
- **Savings & Goals (20%):** ₹${(totalIncome * 0.2).toInt()}

Check the **Essential/Wants** tab for category-level breakdowns and active budget caps."""
            }

            // Habit Score / Health score
            q.contains("habit") || q.contains("health") || q.contains("score") -> {
                val habit = AnalysisEngine.calculateMoneyHabitScore(incomes, expenses, budgets, goals, settings)
                """### 🏅 Money Habit Score: ${habit.score}/100 (${habit.label})
Your overall financial discipline is **${habit.label}**.

${habit.bulletPoints.take(3).joinToString("\n") { (if (it.isPositive) "✅ " else "⚠️ ") + "**${it.title}**: ${it.description}" }}

Explore the full checklist in the **Habit Score** tab."""
            }

            // Recurring / Subscriptions
            q.contains("recurring") || q.contains("subscription") || q.contains("bill") -> {
                val recurring = AnalysisEngine.detectRecurringMoneyMap(expenses, settings.recurringOverrides)
                """### 🔄 Recurring Subscriptions & Bills
- **Total Recurring Outflow:** ₹${recurring.monthlyRecurringTotal.toInt()}/month
- **Detected Recurring Items:** ${recurring.items.size}

Check the **Recurring Map** tab to inspect all detected subscriptions and active bills."""
            }

            // Leaks / Waste
            q.contains("leak") || q.contains("waste") || q.contains("hidden") -> {
                val leaks = AnalysisEngine.detectSpendingLeaks(expenses)
                if (leaks.isEmpty()) {
                    """### 🔍 Spending Leaks Detection
Great news! No recurring micro-transaction leaks or abnormal spending patterns were detected in your recent records."""
                } else {
                    val leakList = leaks.take(3).joinToString("\n") { "• **${it.name}**: ₹${it.monthlyTotal.toInt()}/mo (${it.aiExplanation})" }
                    """### 🔍 Detected Spending Leaks
$leakList

Review all detected leak opportunities in the **Spending Leaks** tab."""
                }
            }

            // Default Copilot summary
            else -> {
                """### ✨ SpendWise Financial Copilot Overview
- **Recorded Income:** ₹${totalIncome.toInt()}
- **Total Expenses:** ₹${totalExpenses.toInt()}
- **Net Monthly Surplus:** ₹${savings.toInt()} (${savingsRate.toInt()}% rate)
- **Active Financial Mode:** ${settings.financialMode}

**Suggestions you can ask me:**
- *"Can I afford a ₹10,000 purchase?"*
- *"Why did I spend more this month?"*
- *"Where am I spending the most?"*
- *"Simulate ₹2,000 food reduction."*
- *"How can I save more money?"*"""
            }
        }
    }
}
