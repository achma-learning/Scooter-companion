package com.example.kymcoagility50.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val KymcoRed = Color(0xFFE30613)
val KymcoRedDark = Color(0xFFB30510)
val PrivateGreen = Color(0xFF2E7D32)

/** Status colours shared with the website (web/css/app.css --ok / --soon / --due). */
@Immutable
data class StatusColors(val ok: Color, val soon: Color, val due: Color, val muted: Color)

val LocalStatusColors = staticCompositionLocalOf {
    StatusColors(Color(0xFF1F9D57), Color(0xFFD98A00), Color(0xFFD62C36), Color(0xFF8A8F99))
}

private val LightColors = lightColorScheme(
    primary = KymcoRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD7),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF2B2F38),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E8EE),
    onSecondaryContainer = Color(0xFF1A1C22),
    tertiary = PrivateGreen,
    background = Color(0xFFF5F6F8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDEEF2),
    onSurfaceVariant = Color(0xFF4A4F59),
    outline = Color(0xFFC9CCD3)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF4D52),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5C0A0E),
    onPrimaryContainer = Color(0xFFFFDAD7),
    secondary = Color(0xFFC9CCD3),
    onSecondary = Color(0xFF15171C),
    secondaryContainer = Color(0xFF2A2E37),
    onSecondaryContainer = Color(0xFFE6E8EE),
    tertiary = Color(0xFF81C784),
    background = Color(0xFF0F1115),
    surface = Color(0xFF171A20),
    surfaceVariant = Color(0xFF22262E),
    onSurfaceVariant = Color(0xFFAEB3BD),
    outline = Color(0xFF3A3F49)
)

@Composable
fun KymcoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
