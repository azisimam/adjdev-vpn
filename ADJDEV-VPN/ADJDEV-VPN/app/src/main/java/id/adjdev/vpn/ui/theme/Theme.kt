package id.adjdev.vpn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BluePrimary,
    onPrimary = White,
    secondary = Navy,
    onSecondary = White,
    background = OffWhite,
    onBackground = NavyDark,
    surface = White,
    onSurface = NavyDark,
    error = StatusFailed
)

private val DarkColors = darkColorScheme(
    primary = BlueLight,
    onPrimary = NavyDark,
    secondary = BluePrimary,
    onSecondary = White,
    background = NavyDark,
    onBackground = OffWhite,
    surface = Navy,
    onSurface = OffWhite,
    error = StatusFailed
)

@Composable
fun AdjdevVpnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
