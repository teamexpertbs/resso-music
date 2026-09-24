package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RessoDarkColorScheme =
  darkColorScheme(
    primary = RessoPrimary,
    onPrimary = Color.White,
    secondary = RessoSecondary,
    onSecondary = Color.Black,
    tertiary = RessoTertiary,
    background = RessoBackground,
    onBackground = RessoOnBackground,
    surface = RessoSurface,
    onSurface = RessoOnSurface,
    surfaceVariant = RessoSurfaceVariant,
    onSurfaceVariant = RessoTextSecondary
  )

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = RessoDarkColorScheme,
    typography = Typography,
    content = content
  )
}

