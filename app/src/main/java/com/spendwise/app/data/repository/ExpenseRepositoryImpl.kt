package com.spendwise.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.spendwise.app.domain.model.Expense
import com.spendwise.app.domain.repository.ExpenseRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ExpenseRepositoryImpl(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ExpenseRepository {

    private val userId: String
        get() = auth.currentUser?.uid ?: ""

    override fun getExpenses(): Flow<List<Expense>> = callbackFlow {
        val subscription = if (userId.isEmpty()) {
            trySend(emptyList())
            null
        } else {
            firestore.collection("users").document(userId)
                .collection("expenses")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val expenses = snapshot?.documents?.mapNotNull { doc ->
                        com.spendwise.app.utils.FirestoreParser.parseExpense(doc)
                    } ?: emptyList()
                    trySend(expenses)
                }
        }
        awaitClose { subscription?.remove() }
    }

    override suspend fun addExpense(expense: Expense): Result<Unit> {
        return try {
            val docRef = firestore.collection("users").document(userId)
                .collection("expenses").document()
            val expenseWithId = expense.copy(id = docRef.id, userId = userId)
            docRef.set(expenseWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateExpense(expense: Expense): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .collection("expenses").document(expense.id)
                .set(expense).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteExpense(expenseId: String): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .collection("expenses").document(expenseId)
                .delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
