package com.spendwise.app.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    
    object Dashboard : Screen("dashboard")
    object Transactions : Screen("transactions")
    object SmartImport : Screen("smart_import")
    object Budget : Screen("budget")
    object SavingsGoals : Screen("savings_goals")
    object AiAssistant : Screen("ai_assistant")
    object Insights : Screen("insights")
    object Reports : Screen("reports")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    
    object AddExpense : Screen("add_expense")
    object EditExpense : Screen("edit_expense/{expenseId}") {
        fun createRoute(expenseId: String) = "edit_expense/$expenseId"
    }
    
    object AddIncome : Screen("add_income")
    object EditIncome : Screen("edit_income/{incomeId}") {
        fun createRoute(incomeId: String) = "edit_income/$incomeId"
    }
}
