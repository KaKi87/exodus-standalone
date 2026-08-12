package eu.exodus.standalone.analyzer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary = Color(0xFF1B5E8C)
private val PrimaryDark = Color(0xFF0F3D5C)
private val Accent = Color(0xFFE85D4C)

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E8F7),
    secondary = Accent,
    background = Color(0xFFF5F7FA),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
)

@Composable
fun ExodusAnalyzerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
