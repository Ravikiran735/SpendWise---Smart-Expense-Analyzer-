package com.spendwise.app.presentation.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.di.AppModule
import com.spendwise.app.domain.model.Expense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class ExpenseViewModel : ViewModel() {
    private val repository = AppModule.expenseRepository

    private val _uiState = MutableStateFlow<ExpenseUiState>(ExpenseUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun addExpense(amount: Double, category: String, description: String, paymentMethod: String, date: Date) {
        viewModelScope.launch {
            _uiState.value = ExpenseUiState.Loading
            val expense = Expense(
                amount = amount,
                category = category,
                description = description,
                paymentMethod = paymentMethod,
                date = date,
                createdAt = Date(),
                updatedAt = Date()
            )
            val result = repository.addExpense(expense)
            if (result.isSuccess) {
                _uiState.value = ExpenseUiState.Success
            } else {
                _uiState.value = ExpenseUiState.Error(result.exceptionOrNull()?.message ?: "Failed to add expense")
            }
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            _uiState.value = ExpenseUiState.Loading
            val result = repository.updateExpense(expense)
            if (result.isSuccess) {
                _uiState.value = ExpenseUiState.Success
            } else {
                _uiState.value = ExpenseUiState.Error(result.exceptionOrNull()?.message ?: "Failed to update expense")
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            _uiState.value = ExpenseUiState.Loading
            val result = repository.deleteExpense(expenseId)
            if (result.isSuccess) {
                _uiState.value = ExpenseUiState.Success
            } else {
                _uiState.value = ExpenseUiState.Error(result.exceptionOrNull()?.message ?: "Failed to delete expense")
            }
        }
    }

    fun resetState() {
        _uiState.value = ExpenseUiState.Idle
    }
}

sealed class ExpenseUiState {
    object Idle : ExpenseUiState()
    object Loading : ExpenseUiState()
    object Success : ExpenseUiState()
    data class Error(val message: String) : ExpenseUiState()
}
