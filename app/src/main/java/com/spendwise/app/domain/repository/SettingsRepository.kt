package com.spendwise.app.domain.repository

import com.spendwise.app.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getUserSettings(): Flow<UserSettings>
    suspend fun updateUserSettings(settings: UserSettings): Result<Unit>
    suspend fun setTheme(theme: String): Result<Unit>
    suspend fun setCurrency(currency: String): Result<Unit>
    suspend fun setNotificationPreference(key: String, enabled: Boolean): Result<Unit>
}
