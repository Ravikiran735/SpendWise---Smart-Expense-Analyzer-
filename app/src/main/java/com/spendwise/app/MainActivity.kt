package com.spendwise.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.spendwise.app.di.AppModule
import com.spendwise.app.domain.model.UserSettings
import com.spendwise.app.presentation.navigation.SpendWiseNavigation
import com.spendwise.app.ui.theme.SpendWiseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val userSettings by AppModule.settingsRepository.getUserSettings()
                .collectAsState(initial = UserSettings())

            val isDark = userSettings.theme != "light"

            SpendWiseTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SpendWiseNavigation()
                }
            }
        }
    }
}
