package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Senior Care - Light Mode (Supreme contrast, comfortable navy/emerald/orange)
private val SeniorLightColorScheme = lightColorScheme(
    primary = SeniorPrimaryLight,
    onPrimary = SeniorOnPrimaryLight,
    secondary = SeniorSecondaryLight,
    onSecondary = SeniorOnSecondaryLight,
    tertiary = SeniorAccentLight,
    background = SeniorBackgroundLight,
    surface = SeniorSurfaceLight,
    onBackground = SeniorTextPrimaryLight,
    onSurface = SeniorTextPrimaryLight,
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = SeniorTextSecondaryLight
)

// Senior Care - Dark Mode (Relaxed, eye-friendly neon contrast on deep blue-black)
private val SeniorDarkColorScheme = darkColorScheme(
    primary = SeniorPrimaryDark,
    onPrimary = SeniorOnPrimaryDark,
    secondary = SeniorSecondaryDark,
    onSecondary = SeniorOnSecondaryDark,
    tertiary = SeniorAccentDark,
    background = SeniorBackgroundDark,
    surface = SeniorSurfaceDark,
    onBackground = SeniorTextPrimaryDark,
    onSurface = SeniorTextPrimaryDark,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = SeniorTextSecondaryDark
)

// Kids Zone Theme - Pastel bright bubblegum aesthetic
private val KidsColorScheme = lightColorScheme(
    primary = KidsPrimary,
    onPrimary = Color.White,
    secondary = KidsSecondary,
    onSecondary = Color.White,
    tertiary = KidsTertiary,
    onTertiary = Color.Black,
    background = KidsBackground,
    surface = KidsSurface,
    onBackground = KidsTextPrimary,
    onSurface = KidsTextPrimary,
    surfaceVariant = Color(0xFFFEF08A), // Light soft yellow
    onSurfaceVariant = KidsTextSecondary
)

@Composable
fun GenerationConnectTheme(
    isKidsMode: Boolean = false,
    darkTheme: Boolean = false, // Enforce light mode instead of dark mode
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        isKidsMode -> KidsColorScheme
        else -> SeniorLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
