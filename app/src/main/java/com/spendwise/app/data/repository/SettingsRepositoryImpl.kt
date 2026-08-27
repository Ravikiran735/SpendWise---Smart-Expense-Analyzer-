package com.spendwise.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.spendwise.app.domain.model.UserSettings
import com.spendwise.app.domain.repository.SettingsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class SettingsRepositoryImpl(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : SettingsRepository {

    private val userId: String
        get() = auth.currentUser?.uid ?: ""

    override fun getUserSettings(): Flow<UserSettings> = callbackFlow {
        val listener = if (userId.isEmpty()) {
            trySend(UserSettings())
            null
        } else {
            firestore.collection("users").document(userId)
                .collection("settings").document("preferences")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(UserSettings())
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val settings = snapshot.toObject(UserSettings::class.java) ?: UserSettings()
                        trySend(settings)
                    } else {
                        val defaultSettings = UserSettings()
                        trySend(defaultSettings)
                    }
                }
        }
        awaitClose { listener?.remove() }
    }

    override suspend fun updateUserSettings(settings: UserSettings): Result<Unit> {
        return try {
            if (userId.isEmpty()) return Result.failure(IllegalStateException("User not authenticated"))

            firestore.collection("users").document(userId)
                .collection("settings").document("preferences")
                .set(settings.copy(updatedAt = Date())).await()

            // Also synchronize currency on user profile document
            firestore.collection("users").document(userId)
                .update(
                    mapOf(
                        "currency" to settings.currency,
                        "updatedAt" to Date()
                    )
                ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setTheme(theme: String): Result<Unit> {
        return try {
            if (userId.isEmpty()) return Result.failure(IllegalStateException("User not authenticated"))

            firestore.collection("users").document(userId)
                .collection("settings").document("preferences")
                .update(
                    mapOf(
                        "theme" to theme,
                        "updatedAt" to Date()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setCurrency(currency: String): Result<Unit> {
        return try {
            if (userId.isEmpty()) return Result.failure(IllegalStateException("User not authenticated"))

            firestore.collection("users").document(userId)
                .collection("settings").document("preferences")
                .update(
                    mapOf(
                        "currency" to currency,
                        "updatedAt" to Date()
                    )
                ).await()

            firestore.collection("users").document(userId)
                .update(
                    mapOf(
                        "currency" to currency,
                        "updatedAt" to Date()
                    )
                ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setNotificationPreference(key: String, enabled: Boolean): Result<Unit> {
        return try {
            if (userId.isEmpty()) return Result.failure(IllegalStateException("User not authenticated"))

            firestore.collection("users").document(userId)
                .collection("settings").document("preferences")
                .update(
                    mapOf(
                        key to enabled,
                        "updatedAt" to Date()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
