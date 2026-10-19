package com.hololingo.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = HoloLingoGreen,
    onPrimary = Color.White,
    secondary = HoloLingoMint,
    onSecondary = HoloLingoGreen,
    background = HoloLingoPaper,
    onBackground = HoloLingoInk,
    surface = Color.White,
    onSurface = HoloLingoInk,
)

private val DarkColors = darkColorScheme(
    primary = HoloLingoMint,
    onPrimary = HoloLingoGreen,
    secondary = HoloLingoMint,
    background = Color(0xFF101817),
    onBackground = Color(0xFFE7F0ED),
    surface = Color(0xFF182523),
    onSurface = Color(0xFFE7F0ED),
)

@Composable
fun HoloLingoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
