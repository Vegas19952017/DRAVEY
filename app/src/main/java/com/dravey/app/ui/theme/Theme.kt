package com.dravey.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF003A52),
    primaryContainer = Color(0xFF004F72),
    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFF00391A),
    secondaryContainer = Color(0xFF1B5E20),
    tertiary = Color(0xFFFFB74D),
    background = Color(0xFF0D1117),
    surface = Color(0xFF161B22),
    surfaceVariant = Color(0xFF21262D),
    onBackground = Color(0xFFE6EDF3),
    onSurface = Color(0xFFE6EDF3),
    onSurfaceVariant = Color(0xFF8B949E),
    error = Color(0xFFEF5350),
    outline = Color(0xFF30363D)
)

@Composable
fun DraveyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
