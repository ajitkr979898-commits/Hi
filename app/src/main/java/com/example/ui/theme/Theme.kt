package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LumaDarkColorScheme = darkColorScheme(
    primary = LumaRose,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3E1022),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = LumaViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2C1B4E),
    onSecondaryContainer = Color(0xFFE9DDFF),
    tertiary = LumaAmber,
    onTertiary = Color(0xFF281600),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceElevated,
    surfaceContainerHigh = DarkSurfaceHighlight,
    outline = DarkOutline,
    outlineVariant = Color(0xFF1F2330),
    error = LumaRose,
    onError = Color.White
)

private val LumaLightColorScheme = lightColorScheme(
    primary = LumaRose,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4EC),
    onPrimaryContainer = Color(0xFF54001B),
    secondary = LumaViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF2E1065),
    tertiary = LumaAmber,
    onTertiary = Color(0xFF281600),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurfaceElevated,
    surfaceContainer = LightSurfaceElevated,
    surfaceContainerHigh = LightSurfaceHighlight,
    outline = LightOutline,
    outlineVariant = Color(0xFFECEFF6),
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val LumaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LumaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) LumaDarkColorScheme else LumaLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LumaTypography,
        shapes = LumaShapes,
        content = content
    )
}
