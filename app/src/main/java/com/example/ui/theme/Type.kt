package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PlusJakartaSans = FontFamily(
  Font(R.font.plus_jakarta_sans, FontWeight.Normal)
)

val JetBrainsMono = FontFamily(
  Font(R.font.jetbrains_mono, FontWeight.Normal)
)

val Typography = Typography(
  displayLarge = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.Bold,
    fontSize = 32.sp,
    lineHeight = 38.sp,
    letterSpacing = (-0.5).sp
  ),
  displayMedium = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.Bold,
    fontSize = 26.sp,
    lineHeight = 32.sp,
    letterSpacing = (-0.25).sp
  ),
  titleLarge = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    letterSpacing = 0.sp
  ),
  titleMedium = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.1.sp
  ),
  titleSmall = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.1.sp
  ),
  bodyLarge = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.2.sp
  ),
  bodyMedium = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.2.sp
  ),
  bodySmall = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.3.sp
  ),
  labelLarge = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.Bold,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.4.sp
  ),
  labelMedium = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    lineHeight = 15.sp,
    letterSpacing = 0.8.sp
  ),
  labelSmall = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.Bold,
    fontSize = 10.sp,
    lineHeight = 14.sp,
    letterSpacing = 1.2.sp // High-end tracked uppercase metadata label
  )
)

// Monospace Code / Markdown Typography
val CodeTextStyle = TextStyle(
  fontFamily = JetBrainsMono,
  fontWeight = FontWeight.Normal,
  fontSize = 12.sp,
  lineHeight = 18.sp,
  letterSpacing = 0.1.sp
)
