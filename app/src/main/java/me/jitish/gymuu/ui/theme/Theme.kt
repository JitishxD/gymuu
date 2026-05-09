package me.jitish.gymuu.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    secondary = GymuuMuted,
    tertiary = GymuuDanger,
    background = GymuuBlack,
    surface = GymuuCard,
    surfaceVariant = GymuuCardAlt,
    outline = GymuuBorder,
    onPrimary = GymuuBlack,
    onSecondary = Color.White,
    onTertiary = GymuuBlack,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = GymuuMuted
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
