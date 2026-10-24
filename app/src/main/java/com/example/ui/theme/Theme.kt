package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = SurfaceCanvas,
    primaryContainer = Slate800,
    onPrimaryContainer = Slate100,
    secondary = IndigoAccent,
    onSecondary = SurfaceCanvas,
    secondaryContainer = Slate700,
    onSecondaryContainer = Slate100,
    tertiary = EmeraldAccent,
    onTertiary = SurfaceCanvas,
    background = SurfaceCanvas,
    onBackground = Slate100,
    surface = SurfaceCard,
    onSurface = Slate100,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = Slate300,
    outline = BorderSubtle,
    outlineVariant = Slate700,
    error = RoseAccent,
    onError = SurfaceCanvas
)

@Composable
fun OmniConvertTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = SurfaceCanvas.toArgb()
                window.navigationBarColor = SurfaceCanvas.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
