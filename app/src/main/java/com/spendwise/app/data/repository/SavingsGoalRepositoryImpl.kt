package com.spendwise.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.spendwise.app.domain.model.SavingsGoal
import com.spendwise.app.domain.repository.SavingsGoalRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SavingsGoalRepositoryImpl(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : SavingsGoalRepository {

    private val userId: String
        get() = auth.currentUser?.uid ?: ""

    override fun getSavingsGoals(): Flow<List<SavingsGoal>> = callbackFlow {
        val subscription = if (userId.isEmpty()) {
            trySend(emptyList())
            null
        } else {
            firestore.collection("users").document(userId)
                .collection("savingsGoals")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val goals = snapshot?.documents?.mapNotNull { doc ->
                        com.spendwise.app.utils.FirestoreParser.parseSavingsGoal(doc)
                    } ?: emptyList()
                    trySend(goals)
                }
        }
        awaitClose { subscription?.remove() }
    }

    override suspend fun addSavingsGoal(goal: SavingsGoal): Result<Unit> {
        return try {
            val docRef = firestore.collection("users").document(userId)
                .collection("savingsGoals").document()
            val goalWithId = goal.copy(id = docRef.id, userId = userId)
            docRef.set(goalWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSavingsGoal(goal: SavingsGoal): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .collection("savingsGoals").document(goal.id)
                .set(goal).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSavingsGoal(goalId: String): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .collection("savingsGoals").document(goalId)
                .delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
