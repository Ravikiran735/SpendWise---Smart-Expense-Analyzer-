package com.spendwise.app.presentation.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.di.AppModule
import com.spendwise.app.domain.model.Income
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class IncomeViewModel : ViewModel() {
    private val repository = AppModule.incomeRepository

    private val _uiState = MutableStateFlow<IncomeUiState>(IncomeUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun addIncome(amount: Double, source: String, description: String, date: Date) {
        viewModelScope.launch {
            _uiState.value = IncomeUiState.Loading
            val income = Income(
                amount = amount,
                source = source,
                description = description,
                date = date,
                createdAt = Date(),
                updatedAt = Date()
            )
            val result = repository.addIncome(income)
            if (result.isSuccess) {
                _uiState.value = IncomeUiState.Success
            } else {
                _uiState.value = IncomeUiState.Error(result.exceptionOrNull()?.message ?: "Failed to add income")
            }
        }
    }

    fun updateIncome(income: Income) {
        viewModelScope.launch {
            _uiState.value = IncomeUiState.Loading
            val result = repository.updateIncome(income)
            if (result.isSuccess) {
                _uiState.value = IncomeUiState.Success
            } else {
                _uiState.value = IncomeUiState.Error(result.exceptionOrNull()?.message ?: "Failed to update income")
            }
        }
    }

    fun deleteIncome(incomeId: String) {
        viewModelScope.launch {
            _uiState.value = IncomeUiState.Loading
            val result = repository.deleteIncome(incomeId)
            if (result.isSuccess) {
                _uiState.value = IncomeUiState.Success
            } else {
                _uiState.value = IncomeUiState.Error(result.exceptionOrNull()?.message ?: "Failed to delete income")
            }
        }
    }

    fun resetState() {
        _uiState.value = IncomeUiState.Idle
    }
}

sealed class IncomeUiState {
    object Idle : IncomeUiState()
    object Loading : IncomeUiState()
    object Success : IncomeUiState()
    data class Error(val message: String) : IncomeUiState()
}
