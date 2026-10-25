package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = OmniDarkAccent,
    onPrimary = OmniDarkOnAccent,
    primaryContainer = OmniDarkAccentSoft,
    onPrimaryContainer = OmniDarkAccent,
    secondary = OmniDarkAccent,
    onSecondary = OmniDarkOnAccent,
    secondaryContainer = OmniDarkSurface2,
    onSecondaryContainer = OmniDarkText,
    tertiary = OmniDarkAccent,
    onTertiary = OmniDarkOnAccent,
    background = OmniDarkBg,
    onBackground = OmniDarkText,
    surface = OmniDarkSurface,
    onSurface = OmniDarkText,
    surfaceVariant = OmniDarkSurface2,
    onSurfaceVariant = OmniDarkTextMuted,
    outline = OmniDarkBorder,
    outlineVariant = OmniDarkBorder,
    error = OmniDarkDanger,
    onError = OmniDarkOnAccent
)

private val LightColorScheme = lightColorScheme(
    primary = OmniLightAccent,
    onPrimary = OmniLightOnAccent,
    primaryContainer = OmniLightAccentSoft,
    onPrimaryContainer = OmniLightAccent,
    secondary = OmniLightAccent,
    onSecondary = OmniLightOnAccent,
    secondaryContainer = OmniLightSurface2,
    onSecondaryContainer = OmniLightText,
    tertiary = OmniLightAccent,
    onTertiary = OmniLightOnAccent,
    background = OmniLightBg,
    onBackground = OmniLightText,
    surface = OmniLightSurface,
    onSurface = OmniLightText,
    surfaceVariant = OmniLightSurface2,
    onSurfaceVariant = OmniLightTextMuted,
    outline = OmniLightBorder,
    outlineVariant = OmniLightBorder,
    error = OmniLightDanger,
    onError = OmniLightOnAccent
)

@Composable
fun OmniConvertTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
