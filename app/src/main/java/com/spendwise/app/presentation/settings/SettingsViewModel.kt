package com.spendwise.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.di.AppModule
import com.spendwise.app.domain.model.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val settingsRepository = AppModule.settingsRepository
    private val authRepository = AppModule.authRepository

    val userSettings: StateFlow<UserSettings> = settingsRepository.getUserSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    fun setTheme(theme: String) {
        viewModelScope.launch {
            settingsRepository.setTheme(theme)
        }
    }

    fun setCurrency(currency: String) {
        viewModelScope.launch {
            settingsRepository.setCurrency(currency)
        }
    }

    fun setNotificationPreference(key: String, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationPreference(key, enabled)
        }
    }

    fun setPrimaryGoal(goal: String) {
        viewModelScope.launch {
            val current = userSettings.value
            settingsRepository.updateUserSettings(current.copy(primaryGoal = goal))
        }
    }

    fun setFinancialMode(mode: String) {
        viewModelScope.launch {
            val current = userSettings.value
            settingsRepository.updateUserSettings(current.copy(financialMode = mode))
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
