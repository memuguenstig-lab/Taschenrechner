package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Using default sans-serif for Inter since we can't easily download it
val displayLg = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 64.sp,
    lineHeight = 72.sp,
    letterSpacing = (-0.02).sp
)

val displayLgMobile = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 48.sp,
    lineHeight = 56.sp
)

val headlineMd = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Medium,
    fontSize = 24.sp,
    lineHeight = 32.sp
)

val bodyLg = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 28.sp
)

val labelMono = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.05.sp
)

val Typography = Typography(
    displayLarge = displayLg,
    displayMedium = displayLgMobile,
    headlineMedium = headlineMd,
    bodyLarge = bodyLg,
    labelMedium = labelMono
)
