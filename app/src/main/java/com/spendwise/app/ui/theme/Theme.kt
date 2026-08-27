package com.spendwise.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = TextDarkPrimary,
    primaryContainer = Color(0xFF2E2E5D),
    onPrimaryContainer = TextDarkPrimary,
    secondary = AccentEmerald,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1E3A34),
    onSecondaryContainer = AccentEmerald,
    tertiary = AccentAmber,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF3D2F1B),
    onTertiaryContainer = AccentAmber,
    error = AccentRose,
    onError = Color.White,
    errorContainer = Color(0xFF4C1D24),
    onErrorContainer = AccentRose,
    background = BgDarkPrimary,
    onBackground = TextDarkPrimary,
    surface = BgDarkSecondary,
    onSurface = TextDarkPrimary,
    surfaceVariant = BgDarkCard,
    onSurfaceVariant = TextDarkSecondary,
    outline = BorderDark,
    outlineVariant = BorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = AccentEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = AccentAmber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF92400E),
    error = AccentRose,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF9F1239),
    background = BgLightPrimary,
    onBackground = TextLightPrimary,
    surface = BgLightCard,
    onSurface = TextLightPrimary,
    surfaceVariant = BgLightSecondary,
    onSurfaceVariant = TextLightSecondary,
    outline = BorderLight,
    outlineVariant = BorderLight
)

@Composable
fun SpendWiseTheme(
    darkTheme: Boolean = true, // Default matching SpendWise dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
