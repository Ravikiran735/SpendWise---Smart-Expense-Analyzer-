package com.spendwise.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.spendwise.app.domain.model.Budget
import com.spendwise.app.domain.repository.BudgetRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BudgetRepositoryImpl(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : BudgetRepository {

    private val userId: String
        get() = auth.currentUser?.uid ?: ""

    override fun getBudgets(): Flow<List<Budget>> = callbackFlow {
        val subscription = if (userId.isEmpty()) {
            trySend(emptyList())
            null
        } else {
            firestore.collection("users").document(userId)
                .collection("budgets")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val budgets = snapshot?.documents?.mapNotNull { doc ->
                        com.spendwise.app.utils.FirestoreParser.parseBudget(doc)
                    } ?: emptyList()
                    trySend(budgets)
                }
        }
        awaitClose { subscription?.remove() }
    }

    override suspend fun addBudget(budget: Budget): Result<Unit> {
        return try {
            val docRef = firestore.collection("users").document(userId)
                .collection("budgets").document()
            val budgetWithId = budget.copy(id = docRef.id, userId = userId)
            docRef.set(budgetWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateBudget(budget: Budget): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .collection("budgets").document(budget.id)
                .set(budget).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteBudget(budgetId: String): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .collection("budgets").document(budgetId)
                .delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
