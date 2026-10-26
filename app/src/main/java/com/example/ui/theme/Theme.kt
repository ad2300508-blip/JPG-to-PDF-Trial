package com.example.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.ThemeMode

private fun MorphColors.toMaterialScheme() = if (isDark) {
    darkColorScheme(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentSoft,
        onPrimaryContainer = accent,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = surface2,
        onSecondaryContainer = text,
        tertiary = accent,
        onTertiary = onAccent,
        background = bg,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = surface2,
        onSurfaceVariant = textMuted,
        surfaceContainer = surface,
        surfaceContainerHigh = surface2,
        surfaceContainerHighest = surface2,
        outline = border,
        outlineVariant = border,
        error = danger,
        onError = onAccent
    )
} else {
    lightColorScheme(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentSoft,
        onPrimaryContainer = accent,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = surface2,
        onSecondaryContainer = text,
        tertiary = accent,
        onTertiary = onAccent,
        background = bg,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = surface2,
        onSurfaceVariant = textMuted,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surface2,
        outline = border,
        outlineVariant = border,
        error = danger,
        onError = onAccent
    )
}

@Composable
fun OmniConvertTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    reduceMotion: Boolean = false,
    accent: AccentChoice = AccentChoice.INDIGO,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val target = (if (darkTheme) MorphDarkColors else MorphLightColors).withAccent(accent)
    // Switching theme cross-fades every colour instead of flashing.
    val morphColors = animateMorphColors(target, animate = !reduceMotion)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalMorphColors provides morphColors,
        LocalReduceMotion provides reduceMotion
    ) {
        MaterialTheme(
            colorScheme = morphColors.toMaterialScheme(),
            typography = Typography,
            content = content
        )
    }
}

@Composable
private fun animateMorphColors(target: MorphColors, animate: Boolean): MorphColors {
    val spec: AnimationSpec<Color> = if (animate) tween(durationMillis = 450) else snap()
    return MorphColors(
        bg = animateColorAsState(target.bg, spec, label = "bg").value,
        surface = animateColorAsState(target.surface, spec, label = "surface").value,
        surface2 = animateColorAsState(target.surface2, spec, label = "surface2").value,
        border = animateColorAsState(target.border, spec, label = "border").value,
        text = animateColorAsState(target.text, spec, label = "text").value,
        textMuted = animateColorAsState(target.textMuted, spec, label = "textMuted").value,
        accent = animateColorAsState(target.accent, spec, label = "accent").value,
        onAccent = animateColorAsState(target.onAccent, spec, label = "onAccent").value,
        accentSoft = animateColorAsState(target.accentSoft, spec, label = "accentSoft").value,
        danger = animateColorAsState(target.danger, spec, label = "danger").value,
        success = animateColorAsState(target.success, spec, label = "success").value,
        successSoft = animateColorAsState(target.successSoft, spec, label = "successSoft").value,
        ink = animateColorAsState(target.ink, spec, label = "ink").value,
        onInk = animateColorAsState(target.onInk, spec, label = "onInk").value,
        isDark = target.isDark
    )
}
