package com.spendwise.app.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.di.AppModule
import com.spendwise.app.domain.model.SavingsGoal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class SavingsGoalViewModel : ViewModel() {
    private val repository = AppModule.savingsGoalRepository

    private val _uiState = MutableStateFlow<GoalUiState>(GoalUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun addGoal(title: String, targetAmount: Double, deadline: Date?) {
        viewModelScope.launch {
            _uiState.value = GoalUiState.Loading
            val goal = SavingsGoal(
                title = title,
                targetAmount = targetAmount,
                currentAmount = 0.0,
                deadline = deadline,
                createdAt = Date(),
                updatedAt = Date()
            )
            val result = repository.addSavingsGoal(goal)
            if (result.isSuccess) {
                _uiState.value = GoalUiState.Success
            } else {
                _uiState.value = GoalUiState.Error(result.exceptionOrNull()?.message ?: "Failed to add goal")
            }
        }
    }

    fun updateGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            _uiState.value = GoalUiState.Loading
            val result = repository.updateSavingsGoal(goal)
            if (result.isSuccess) {
                _uiState.value = GoalUiState.Success
            } else {
                _uiState.value = GoalUiState.Error(result.exceptionOrNull()?.message ?: "Failed to update goal")
            }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            _uiState.value = GoalUiState.Loading
            val result = repository.deleteSavingsGoal(goalId)
            if (result.isSuccess) {
                _uiState.value = GoalUiState.Success
            } else {
                _uiState.value = GoalUiState.Error(result.exceptionOrNull()?.message ?: "Failed to delete goal")
            }
        }
    }

    fun resetState() {
        _uiState.value = GoalUiState.Idle
    }
}

sealed class GoalUiState {
    object Idle : GoalUiState()
    object Loading : GoalUiState()
    object Success : GoalUiState()
    data class Error(val message: String) : GoalUiState()
}
