package com.example.kymcoagility50.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val KymcoRed = Color(0xFFE30613)
val KymcoRedDark = Color(0xFFB30510)
val PrivateGreen = Color(0xFF2E7D32)

private val LightColors = lightColorScheme(
    primary = KymcoRed,
    onPrimary = Color.White,
    secondary = PrivateGreen,
    onSecondary = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF6E6E),
    onPrimary = Color.Black,
    secondary = Color(0xFF81C784),
    onSecondary = Color.Black
)

@Composable
fun KymcoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
