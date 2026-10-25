package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Light Theme Tokens
val OmniLightBg = Color(0xFFF6F8FA)
val OmniLightSurface = Color(0xFFFFFFFF)
val OmniLightSurface2 = Color(0xFFEDF1F4)
val OmniLightBorder = Color(0xFFE1E6EB)
val OmniLightText = Color(0xFF12171C)
val OmniLightTextMuted = Color(0xFF5B6672)
val OmniLightAccent = Color(0xFF0A7F62)
val OmniLightOnAccent = Color(0xFFFFFFFF)
val OmniLightAccentSoft = Color(0x1F0A7F62) // 12% opacity
val OmniLightDanger = Color(0xFFC93B31)

// Dark Theme Tokens
val OmniDarkBg = Color(0xFF0F1317)
val OmniDarkSurface = Color(0xFF171C21)
val OmniDarkSurface2 = Color(0xFF202730)
val OmniDarkBorder = Color(0xFF2B333C)
val OmniDarkText = Color(0xFFE8EDF1)
val OmniDarkTextMuted = Color(0xFF9AA5B1)
val OmniDarkAccent = Color(0xFF3DD6B0)
val OmniDarkOnAccent = Color(0xFF06231C)
val OmniDarkAccentSoft = Color(0x243DD6B0) // 14% opacity
val OmniDarkDanger = Color(0xFFF2766B)

// Default / Active Dark References
val OmniBg = OmniDarkBg
val OmniSurface = OmniDarkSurface
val OmniSurface2 = OmniDarkSurface2
val OmniBorder = OmniDarkBorder
val OmniText = OmniDarkText
val OmniTextMuted = OmniDarkTextMuted
val OmniAccent = OmniDarkAccent
val OmniAccentSoft = OmniDarkAccentSoft
val OmniOnAccent = OmniDarkOnAccent
val OmniDanger = OmniDarkDanger

// Category Tints (Used for icon tiles and empty state illustrations: 14% opacity background + full color icon)
val TintPdf = Color(0xFFE5604F)
val TintMerge = Color(0xFF3F8CFF)
val TintImage = Color(0xFFF2A93B)
val TintAudio = Color(0xFFE4609F)
val TintZip = Color(0xFF8F8A7A)
val TintAi = OmniDarkAccent

// Shape Tokens
val RadiusInput = 14.dp
val RadiusButton = 16.dp
val RadiusCard = 20.dp
val RadiusPill = 24.dp
val RadiusSheet = 28.dp

val RadiusSm = 10.dp
val RadiusMd = 14.dp
val RadiusLg = 20.dp

// Backward-compatible semantic aliases
val SurfaceCanvas = OmniBg
val SurfaceCard = OmniSurface
val SurfaceElevated = OmniSurface2
val BorderSubtle = OmniBorder
val BorderFocused = OmniAccent
val BorderCrisp = OmniBorder
val CardSurface = OmniSurface

val Slate950 = OmniBg
val Slate900 = OmniSurface
val Slate800 = OmniSurface2
val Slate700 = OmniBorder
val Slate600 = Color(0xFF475569)
val Slate400 = OmniTextMuted
val Slate300 = Color(0xFFCBD5E1)
val Slate100 = OmniText

val CyanAccent = OmniAccent
val CyanGlow = OmniAccentSoft
val IndigoAccent = OmniAccent
val EmeraldAccent = OmniAccent
val AmberAccent = OmniAccent
val RoseAccent = OmniDanger
val PdfGreen = OmniAccent
val PdfRed = OmniDanger
val PdfPurple = OmniAccent
val PdfOrange = OmniAccent
val SuccessGreen = OmniAccent
val AccentCyan = OmniAccent
val WarningAmber = OmniAccent
val BadgeMd = OmniAccent
val BadgeMdBg = OmniAccentSoft
