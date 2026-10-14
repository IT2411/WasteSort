package org.wastesort.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CAF50),
    secondary = Color(0xFF64B5F6),
    background = BackgroundDark,
    surface = SurfaceDark,
)

@Composable
fun WasteSortTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}
