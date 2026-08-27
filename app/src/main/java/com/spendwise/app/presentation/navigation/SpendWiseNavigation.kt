package com.spendwise.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.spendwise.app.presentation.auth.LoginScreen
import com.spendwise.app.presentation.auth.RegisterScreen
import com.spendwise.app.presentation.auth.ForgotPasswordScreen
import com.spendwise.app.presentation.dashboard.DashboardScreen
import com.spendwise.app.presentation.transactions.TransactionsScreen
import com.spendwise.app.presentation.importdata.SmartImportScreen
import com.spendwise.app.presentation.ai.AiAssistantScreen
import com.spendwise.app.presentation.budget.BudgetScreen
import com.spendwise.app.presentation.insights.InsightsScreen
import com.spendwise.app.presentation.profile.ProfileScreen
import com.spendwise.app.presentation.reports.ReportsScreen
import com.spendwise.app.presentation.settings.SettingsScreen
import com.spendwise.app.presentation.expense.AddExpenseScreen
import com.spendwise.app.presentation.expense.CameraScreen
import com.spendwise.app.presentation.expense.EditExpenseScreen
import com.spendwise.app.presentation.goals.SavingsGoalsScreen
import com.spendwise.app.presentation.income.AddIncomeScreen
import com.spendwise.app.presentation.income.EditIncomeScreen
import com.spendwise.app.presentation.splash.SplashScreen

@Composable
fun SpendWiseNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController)
        }
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(navController)
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController)
        }
        composable(Screen.Transactions.route) {
            TransactionsScreen(navController)
        }
        composable(Screen.SmartImport.route) {
            SmartImportScreen(navController)
        }
        composable(Screen.Budget.route) {
            BudgetScreen(navController)
        }
        composable(Screen.AiAssistant.route) {
            AiAssistantScreen(navController)
        }
        composable(Screen.Insights.route) {
            InsightsScreen(navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController)
        }
        composable(Screen.AddExpense.route) {
            AddExpenseScreen(navController)
        }
        composable("camera") {
            CameraScreen(navController) { amount, desc ->
                navController.previousBackStackEntry?.savedStateHandle?.set("scannedAmount", amount)
                navController.previousBackStackEntry?.savedStateHandle?.set("scannedDesc", desc)
            }
        }
        composable(Screen.EditExpense.route) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId") ?: ""
            EditExpenseScreen(navController, expenseId)
        }
        composable(Screen.AddIncome.route) {
            AddIncomeScreen(navController)
        }
        composable(Screen.EditIncome.route) { backStackEntry ->
            val incomeId = backStackEntry.arguments?.getString("incomeId") ?: ""
            EditIncomeScreen(navController, incomeId)
        }
        composable(Screen.SavingsGoals.route) {
            SavingsGoalsScreen(navController)
        }
        composable(Screen.Reports.route) {
            ReportsScreen(navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController)
        }
    }
}
