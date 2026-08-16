package com.projecteternal.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF06213F),
    secondary = Color(0xFFF4A261),
    tertiary = Color(0xFF9C7BFF),
    background = Color(0xFF14161B),
    surface = Color(0xFF1E222B),
    surfaceVariant = Color(0xFF2A2F3A),
    onSurface = Color(0xFFE3E6EC),
    onSurfaceVariant = Color(0xFFA6ADBB),
    error = Color(0xFFF28B82),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5FA8),
    secondary = Color(0xFFB45D1E),
    background = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF),
)

@Composable
fun EternalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
