package com.spendwise.app.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.spendwise.app.data.repository.*
import com.spendwise.app.domain.repository.*

object AppModule {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(auth, firestore)
    }

    val expenseRepository: ExpenseRepository by lazy {
        ExpenseRepositoryImpl(auth, firestore)
    }

    val incomeRepository: IncomeRepository by lazy {
        IncomeRepositoryImpl(auth, firestore)
    }

    val budgetRepository: BudgetRepository by lazy {
        BudgetRepositoryImpl(auth, firestore)
    }

    val savingsGoalRepository: SavingsGoalRepository by lazy {
        SavingsGoalRepositoryImpl(auth, firestore)
    }

    val importRepository: ImportRepository by lazy {
        ImportRepositoryImpl(auth, firestore)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(auth, firestore)
    }

    val aiAssistantRepository: AiAssistantRepository by lazy {
        AiAssistantRepositoryImpl(auth, firestore)
    }

    val aiAssistantUseCase: com.spendwise.app.domain.usecase.AiAssistantUseCase by lazy {
        com.spendwise.app.domain.usecase.AiAssistantUseCase(aiAssistantRepository)
    }
}
