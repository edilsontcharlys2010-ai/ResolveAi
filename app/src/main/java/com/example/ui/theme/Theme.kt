package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = Color(0xFF60A5FA),
  onPrimary = Color(0xFF0F172A),
  primaryContainer = Color(0xFF1E3A8A),
  onPrimaryContainer = Color(0xFFDBEAFE),
  secondary = Color(0xFF2DD4BF),
  onSecondary = Color(0xFF042F2E),
  secondaryContainer = Color(0xFF115E59),
  onSecondaryContainer = Color(0xFFCCFBF1),
  tertiary = Color(0xFFFBBF24),
  onTertiary = Color(0xFF451A03),
  background = SurfaceDark,
  onBackground = TextPrimaryDark,
  surface = CardDark,
  onSurface = TextPrimaryDark,
  surfaceVariant = Color(0xFF334155),
  onSurfaceVariant = Color(0xFFCBD5E1),
  error = Color(0xFFF87171),
  onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
  primary = BrandPrimary,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFEFF6FF),
  onPrimaryContainer = Color(0xFF1E3A8A),
  secondary = BrandSecondary,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFF0FDFA),
  onSecondaryContainer = Color(0xFF115E59),
  tertiary = BrandTertiary,
  onTertiary = Color.White,
  background = SurfaceLight,
  onBackground = TextPrimaryLight,
  surface = CardLight,
  onSurface = TextPrimaryLight,
  surfaceVariant = Color(0xFFF1F5F9),
  onSurfaceVariant = TextSecondaryLight,
  error = DangerRed,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep consistent branding
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
