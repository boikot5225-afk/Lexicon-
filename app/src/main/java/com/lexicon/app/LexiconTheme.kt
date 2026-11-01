package com.lexicon.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightLexicon = lightColorScheme(
    primary = Color(0xFF225DA3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E6FA),
    onPrimaryContainer = Color(0xFF0A2748),
    secondary = Color(0xFF476681),
    background = Color(0xFFF8FAFD),
    surface = Color(0xFFF8FAFD),
    surfaceContainerLow = Color(0xFFFFFFFF),
    onSurface = Color(0xFF152539),
    onSurfaceVariant = Color(0xFF58697B)
)

private val DarkLexicon = darkColorScheme(
    primary = Color(0xFF9DC8FF),
    onPrimary = Color(0xFF002E58),
    primaryContainer = Color(0xFF144A80),
    onPrimaryContainer = Color(0xFFD7E6FA),
    secondary = Color(0xFFAFCCE8),
    background = Color(0xFF101A28),
    surface = Color(0xFF101A28),
    surfaceContainerLow = Color(0xFF192638),
    onSurface = Color(0xFFE9F1FC),
    onSurfaceVariant = Color(0xFFB7C5D6)
)

private val LexiconShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LexiconTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkLexicon else LightLexicon,
        shapes = LexiconShapes,
        content = content
    )
}
