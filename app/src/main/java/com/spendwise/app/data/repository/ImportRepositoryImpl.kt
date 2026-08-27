package com.spendwise.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.spendwise.app.domain.model.*
import com.spendwise.app.domain.repository.ImportRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class ImportRepositoryImpl(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ImportRepository {

    private val userId: String
        get() = auth.currentUser?.uid ?: ""

    override fun getImportHistory(): Flow<List<ImportHistory>> = callbackFlow {
        val subscription = if (userId.isEmpty()) {
            trySend(emptyList())
            null
        } else {
            firestore.collection("users").document(userId)
                .collection("importHistory")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents?.mapNotNull { doc ->
                        com.spendwise.app.utils.FirestoreParser.parseImportHistory(doc)
                    } ?: emptyList()
                    trySend(list)
                }
        }
        awaitClose { subscription?.remove() }
    }

    override suspend fun recordImport(history: ImportHistory): Result<String> {
        return try {
            if (userId.isEmpty()) return Result.failure(IllegalStateException("User not authenticated"))
            val docRef = if (history.importId.isNotBlank()) {
                firestore.collection("users").document(userId)
                    .collection("importHistory").document(history.importId)
            } else {
                firestore.collection("users").document(userId)
                    .collection("importHistory").document()
            }
            val record = history.copy(importId = docRef.id, userId = userId)
            docRef.set(record).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveImportedTransactions(
        importId: String,
        candidates: List<ImportCandidate>
    ): Result<Pair<Int, Int>> {
        return try {
            if (userId.isEmpty()) return Result.failure(IllegalStateException("User not authenticated"))

            var expCount = 0
            var incCount = 0

            // Filter selected & valid/review candidates (skip duplicates and invalid)
            val toImport = candidates.filter { 
                it.isSelected && (it.status == CandidateStatus.NEW || it.status == CandidateStatus.NEEDS_REVIEW) 
            }

            // Write in batches of 450 to stay well under Firestore 500 limit
            val chunked = toImport.chunked(450)
            for (chunk in chunked) {
                val batch = firestore.batch()
                for (item in chunk) {
                    if (item.type.equals("Income", ignoreCase = true)) {
                        val docRef = firestore.collection("users").document(userId)
                            .collection("incomes").document()
                        val income = Income(
                            id = docRef.id,
                            userId = userId,
                            amount = item.amount,
                            source = item.category.ifBlank { "Other" },
                            description = item.description,
                            date = item.date,
                            createdAt = Date(),
                            updatedAt = Date(),
                            origin = item.source.ifBlank { "CSV" },
                            importId = importId,
                            paymentMethod = item.paymentMethod.ifBlank { "Bank Transfer" },
                            reviewStatus = if (item.status == CandidateStatus.NEEDS_REVIEW) "needs_review" else "confirmed"
                        )
                        batch.set(docRef, income)
                        incCount++
                    } else {
                        val docRef = firestore.collection("users").document(userId)
                            .collection("expenses").document()
                        val expense = Expense(
                            id = docRef.id,
                            userId = userId,
                            amount = item.amount,
                            category = item.category.ifBlank { "Other" },
                            description = item.description,
                            paymentMethod = item.paymentMethod.ifBlank { "UPI" },
                            date = item.date,
                            createdAt = Date(),
                            updatedAt = Date(),
                            source = item.source.ifBlank { "CSV" },
                            importId = importId,
                            reviewStatus = if (item.status == CandidateStatus.NEEDS_REVIEW) "needs_review" else "confirmed"
                        )
                        batch.set(docRef, expense)
                        expCount++
                    }
                }
                batch.commit().await()
            }

            Result.success(Pair(expCount, incCount))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getImportPreferences(): Flow<ImportPreferences> = callbackFlow {
        val subscription = if (userId.isEmpty()) {
            trySend(ImportPreferences())
            null
        } else {
            firestore.collection("users").document(userId)
                .collection("settings").document("preferences")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    val prefs = snapshot?.toObject(ImportPreferences::class.java) ?: ImportPreferences()
                    trySend(prefs)
                }
        }
        awaitClose { subscription?.remove() }
    }

    override suspend fun updateImportPreferences(preferences: ImportPreferences): Result<Unit> {
        return try {
            if (userId.isEmpty()) return Result.failure(IllegalStateException("User not authenticated"))
            firestore.collection("users").document(userId)
                .collection("settings").document("preferences")
                .set(preferences).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
