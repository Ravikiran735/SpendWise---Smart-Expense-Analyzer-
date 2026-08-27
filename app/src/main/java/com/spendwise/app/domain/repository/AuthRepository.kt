package com.spendwise.app.domain.repository

import com.spendwise.app.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun register(
        name: String,
        email: String,
        password: String,
        currency: String = "INR",
        monthlyIncome: Double = 0.0
    ): Result<Unit>
    suspend fun logout()
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun updateProfile(user: User): Result<Unit>
}
