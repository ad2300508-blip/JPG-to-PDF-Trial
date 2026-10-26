package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Morph design tokens. Every screen reads colors through [LocalMorphColors] so the
 * whole app switches between light and dark without per-screen changes.
 */
@Immutable
data class MorphColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val text: Color,
    val textMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val danger: Color,
    val success: Color,
    val successSoft: Color,
    val ink: Color,
    val onInk: Color,
    val isDark: Boolean
)

val MorphLightColors = MorphColors(
    bg = Color(0xFFF3F5FB),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEDF0F6),
    border = Color(0xFFE2E6EE),
    text = Color(0xFF10131A),
    textMuted = Color(0xFF5E6573),
    accent = Color(0xFF3050E3),
    onAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFE5EAFC),
    danger = Color(0xFFC0362C),
    success = Color(0xFF16875A),
    successSoft = Color(0xFFDDF2E7),
    ink = Color(0xFF111827),
    onInk = Color(0xFFFFFFFF),
    isDark = false
)

val MorphDarkColors = MorphColors(
    bg = Color(0xFF0D1017),
    surface = Color(0xFF161A23),
    surface2 = Color(0xFF20252F),
    border = Color(0xFF2B313D),
    text = Color(0xFFE9ECF3),
    textMuted = Color(0xFF9AA3B5),
    accent = Color(0xFF7F95FF),
    onAccent = Color(0xFF0A1033),
    accentSoft = Color(0xFF222A4A),
    danger = Color(0xFFFF7A6E),
    success = Color(0xFF4CC38A),
    successSoft = Color(0xFF173528),
    ink = Color(0xFF1E2433),
    onInk = Color(0xFFFFFFFF),
    isDark = true
)

val LocalMorphColors = staticCompositionLocalOf { MorphLightColors }

// Active theme tokens (resolved from the current theme)
val OmniBg: Color @Composable @ReadOnlyComposable get() = LocalMorphColors.current.bg
val OmniSurface: Color @Composable @ReadOnlyComposable get() = LocalMorphColors.current.surface
val OmniSurface2: Color @Composable @ReadOnlyComposable get() = LocalMorphColors.current.surface2
val OmniBorder: Color @Composable @ReadOnlyComposable get() = LocalMorphColors.current.border
val OmniText: Color @Composable @ReadOnlyComposable get() = LocalMorphColors.current.text
val OmniTextMuted: Color @Composable @ReadOnlyComposable get() = LocalMorphColors.current.textMuted
val OmniAccent: Color @Composable @ReadOnlyComposable get() = LocalMorphColors.current.accent
val OmniAccentSoft: Color @Composable @ReadOnlyComposable get() = LocalMorphColors.current.accentSoft
val OmniOnAccent: Color @Composable @ReadOnlyComposable get() = LocalMorphColors.current.onAccent
val OmniDanger: Color @Composable @ReadOnlyComposable get() = LocalMorphColors.current.danger
val OmniSuccess: Color @Composable @ReadOnlyComposable get() = LocalMorphColors.current.success
val OmniSuccessSoft: Color @Composable @ReadOnlyComposable get() = LocalMorphColors.current.successSoft
val OmniInk: Color @Composable @ReadOnlyComposable get() = LocalMorphColors.current.ink
val OmniOnInk: Color @Composable @ReadOnlyComposable get() = LocalMorphColors.current.onInk

// Category Tints (icon tiles: 14% opacity background + full color icon, readable in both themes)
val TintPdf = Color(0xFF3B5BE8)
val TintMerge = Color(0xFF5B6CF0)
val TintImage = Color(0xFF16A08A)
val TintAudio = Color(0xFFD9548F)
val TintZip = Color(0xFFE07A2E)
val TintAi = Color(0xFF6E7BF2)
val TintOcr = Color(0xFF14A38B)

// Shape Tokens
val RadiusInput = 14.dp
val RadiusButton = 16.dp
val RadiusCard = 20.dp
val RadiusPill = 24.dp
val RadiusSheet = 28.dp

val RadiusSm = 12.dp
val RadiusMd = 16.dp
val RadiusLg = 20.dp

// Backward-compatible semantic aliases
val SurfaceCanvas: Color @Composable @ReadOnlyComposable get() = OmniBg
val SurfaceCard: Color @Composable @ReadOnlyComposable get() = OmniSurface
val SurfaceElevated: Color @Composable @ReadOnlyComposable get() = OmniSurface2
val BorderSubtle: Color @Composable @ReadOnlyComposable get() = OmniBorder
val BorderFocused: Color @Composable @ReadOnlyComposable get() = OmniAccent
val BorderCrisp: Color @Composable @ReadOnlyComposable get() = OmniBorder
val CardSurface: Color @Composable @ReadOnlyComposable get() = OmniSurface

val Slate950: Color @Composable @ReadOnlyComposable get() = OmniBg
val Slate900: Color @Composable @ReadOnlyComposable get() = OmniSurface
val Slate800: Color @Composable @ReadOnlyComposable get() = OmniSurface2
val Slate700: Color @Composable @ReadOnlyComposable get() = OmniBorder
val Slate600 = Color(0xFF64748B)
val Slate400: Color @Composable @ReadOnlyComposable get() = OmniTextMuted
val Slate300 = Color(0xFFCBD5E1)
val Slate100: Color @Composable @ReadOnlyComposable get() = OmniText

val CyanAccent: Color @Composable @ReadOnlyComposable get() = OmniAccent
val CyanGlow: Color @Composable @ReadOnlyComposable get() = OmniAccentSoft
val IndigoAccent: Color @Composable @ReadOnlyComposable get() = OmniAccent
val EmeraldAccent: Color @Composable @ReadOnlyComposable get() = OmniSuccess
val AmberAccent: Color @Composable @ReadOnlyComposable get() = OmniAccent
val RoseAccent: Color @Composable @ReadOnlyComposable get() = OmniDanger
val PdfGreen: Color @Composable @ReadOnlyComposable get() = OmniSuccess
val PdfRed: Color @Composable @ReadOnlyComposable get() = OmniDanger
val PdfPurple: Color @Composable @ReadOnlyComposable get() = OmniAccent
val PdfOrange: Color @Composable @ReadOnlyComposable get() = OmniAccent
val SuccessGreen: Color @Composable @ReadOnlyComposable get() = OmniSuccess
val AccentCyan: Color @Composable @ReadOnlyComposable get() = OmniAccent
val WarningAmber: Color @Composable @ReadOnlyComposable get() = OmniAccent
val BadgeMd: Color @Composable @ReadOnlyComposable get() = OmniAccent
val BadgeMdBg: Color @Composable @ReadOnlyComposable get() = OmniAccentSoft
