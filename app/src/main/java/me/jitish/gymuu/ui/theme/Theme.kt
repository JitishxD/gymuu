package me.jitish.gymuu.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    secondary = GymMuted,
    tertiary = GymDanger,
    background = GymBlack,
    surface = GymCard,
    surfaceVariant = GymCardAlt,
    outline = GymBorder,
    onPrimary = GymBlack,
    onSecondary = Color.White,
    onTertiary = GymBlack,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = GymMuted
)

@Composable
fun GymuuTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
