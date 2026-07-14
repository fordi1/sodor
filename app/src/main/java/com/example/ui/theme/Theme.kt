package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    tertiary = Tertiary,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = Color(0xFF334155),
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = Color(0xFFCBD5E1),
    primaryContainer = Color(0xFF1E40AF),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondaryContainer = Color(0xFF0F766E),
    onSecondaryContainer = Color(0xFFCCFBF1),
    outline = Color(0xFF475569)
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = Secondary,
    tertiary = Tertiary,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF64748B),
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E40AF),
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF0F766E),
    outline = Color(0xFFE2E8F0)
)

val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun MyApplicationTheme(
    mode: String = "SYSTEM", // "LIGHT", "DARK", "SYSTEM"
    dynamicColor: Boolean = false, // Set to false to maintain professional branding consistency
    content: @Composable () -> Unit
) {
    val darkTheme = when (mode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
