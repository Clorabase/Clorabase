package com.clorabase.console.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Green,
    onPrimary = White,
    primaryContainer = LightGreen,
    onPrimaryContainer = Black,
    secondary = Pink,
    onSecondary = White,
    secondaryContainer = LightPink,
    onSecondaryContainer = Black,
    background = SurfaceLight,
    onBackground = Black,
    surface = White,
    onSurface = Black
)

private val DarkColorScheme = darkColorScheme(
    primary = Green,
    onPrimary = Black,
    primaryContainer = DarkGreen,
    onPrimaryContainer = White,
    secondary = Pink,
    onSecondary = Black,
    secondaryContainer = DarkPink,
    onSecondaryContainer = White,
    background = SurfaceDark,
    onBackground = White,
    surface = SurfaceDark,
    onSurface = White
)

@Composable
fun ClorabaseTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}