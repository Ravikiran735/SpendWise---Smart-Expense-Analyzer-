package com.spendwise.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.spendwise.app.domain.model.Income
import com.spendwise.app.domain.repository.IncomeRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class IncomeRepositoryImpl(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : IncomeRepository {

    private val userId: String
        get() = auth.currentUser?.uid ?: ""

    override fun getIncomes(): Flow<List<Income>> = callbackFlow {
        val subscription = if (userId.isEmpty()) {
            trySend(emptyList())
            null
        } else {
            firestore.collection("users").document(userId)
                .collection("incomes")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val incomes = snapshot?.documents?.mapNotNull { doc ->
                        com.spendwise.app.utils.FirestoreParser.parseIncome(doc)
                    } ?: emptyList()
                    trySend(incomes)
                }
        }
        awaitClose { subscription?.remove() }
    }

    override suspend fun addIncome(income: Income): Result<Unit> {
        return try {
            val docRef = firestore.collection("users").document(userId)
                .collection("incomes").document()
            val incomeWithId = income.copy(id = docRef.id, userId = userId)
            docRef.set(incomeWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateIncome(income: Income): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .collection("incomes").document(income.id)
                .set(income).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteIncome(incomeId: String): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .collection("incomes").document(incomeId)
                .delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
