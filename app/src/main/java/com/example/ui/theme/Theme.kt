package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme =
  darkColorScheme(
    primary = FanBluePrimary,
    onPrimary = Color.White,
    primaryContainer = FanOnBlueContainer,
    onPrimaryContainer = FanBlueContainer,
    secondary = FanSlateContainer,
    onSecondary = FanOnSlateContainer,
    tertiary = FanAmberAccent,
    tertiaryContainer = FanOnAmberContainer,
    onTertiaryContainer = FanAmberContainer,
    background = FanSurfaceDark,
    onBackground = FanOnSurfaceDark,
    surface = FanCardDark,
    onSurface = FanOnSurfaceDark,
    surfaceVariant = FanOutlineDark,
    onSurfaceVariant = FanOnSurfaceMutedDark,
    outline = FanOutlineDark,
    error = FanError,
    errorContainer = FanErrorContainer,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = FanBluePrimary,
    onPrimary = Color.White,
    primaryContainer = FanBlueContainer,
    onPrimaryContainer = FanOnBlueContainer,
    secondary = FanSlateSecondary,
    onSecondary = Color.White,
    secondaryContainer = FanSlateContainer,
    onSecondaryContainer = FanOnSlateContainer,
    tertiary = FanAmberAccent,
    tertiaryContainer = FanAmberContainer,
    onTertiaryContainer = FanOnAmberContainer,
    background = FanSurfaceLight,
    onBackground = FanOnSurfaceLight,
    surface = FanCardLight,
    onSurface = FanOnSurfaceLight,
    surfaceVariant = FanSlateContainer,
    onSurfaceVariant = FanOnSurfaceMutedLight,
    outline = FanOutlineLight,
    error = FanError,
    errorContainer = FanErrorContainer,
  )

val FanSiteShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
  )

@Composable
fun FanSiteTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = FanSiteShapes,
    content = content,
  )
}
