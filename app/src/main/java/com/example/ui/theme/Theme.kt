package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = CipherPrimary,
    onPrimary = CipherOnPrimary,
    primaryContainer = CipherPrimaryContainer,
    onPrimaryContainer = CipherOnPrimaryContainer,
    inversePrimary = CipherInversePrimary,
    secondary = CipherSecondary,
    onSecondary = CipherOnSecondary,
    secondaryContainer = CipherSecondaryContainer,
    onSecondaryContainer = CipherOnSecondaryContainer,
    tertiary = CipherTertiary,
    onTertiary = CipherOnTertiary,
    tertiaryContainer = CipherTertiaryContainer,
    onTertiaryContainer = CipherOnTertiaryContainer,
    error = CipherError,
    onError = CipherOnError,
    errorContainer = CipherErrorContainer,
    onErrorContainer = CipherOnErrorContainer,
    background = CipherBackground,
    onBackground = CipherOnBackground,
    surface = CipherSurface,
    onSurface = CipherOnSurface,
    surfaceVariant = CipherSurfaceVariant,
    onSurfaceVariant = CipherOnSurfaceVariant,
    inverseSurface = CipherInverseSurface,
    inverseOnSurface = CipherInverseOnSurface,
    surfaceTint = CipherSurfaceTint,
    outline = CipherOutline,
    outlineVariant = CipherOutlineVariant
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = DarkColorScheme, typography = Typography, content = content)
}
