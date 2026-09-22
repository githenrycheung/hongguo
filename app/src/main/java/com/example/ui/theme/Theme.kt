package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HongguoTvColorScheme = darkColorScheme(
    primary = HongguoRed,
    onPrimary = Color.White,
    primaryContainer = HongguoRedDark,
    onPrimaryContainer = Color.White,
    secondary = HongguoGold,
    onSecondary = Color.Black,
    secondaryContainer = TvSurfaceElevated,
    onSecondaryContainer = TvTextPrimary,
    background = TvBackground,
    onBackground = TvTextPrimary,
    surface = TvSurface,
    onSurface = TvTextPrimary,
    surfaceVariant = TvSurfaceVariant,
    onSurfaceVariant = TvTextSecondary,
    outline = TvCardBorder
)

@Composable
fun HongguoTvTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HongguoTvColorScheme,
        typography = Typography,
        content = content
    )
}
