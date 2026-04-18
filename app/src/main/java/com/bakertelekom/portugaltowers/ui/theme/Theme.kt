package com.bakertelekom.portugaltowers.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF0055B4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E3FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF006D3C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7F4E3),
    onSecondaryContainer = Color(0xFF002111),
    background = Color(0xFFF8F9FD),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE1E5EF),
    onSurfaceVariant = Color(0xFF444752),
    outline = Color(0xFF747781),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFADC7FF),
    onPrimary = Color(0xFF002E69),
    primaryContainer = Color(0xFF004491),
    onPrimaryContainer = Color(0xFFD8E3FF),
    secondary = Color(0xFFB9DCC4),
    onSecondary = Color(0xFF07351D),
    secondaryContainer = Color(0xFF1F4D32),
    onSecondaryContainer = Color(0xFFD7F4E3),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF444752),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    error = Color(0xFFFFB4AB),
)

@Composable
fun PortugalTowersTheme(
    useDynamicColor: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors: ColorScheme = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) DarkColors else LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = androidx.compose.material3.Typography(),
        content = content,
    )
}
