package adco.helixsongview.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HelixDarkColorScheme = darkColorScheme(
    primary = HelixGreenPrimary,
    onPrimary = HelixBackground,
    primaryContainer = HelixGreenHeader,
    onPrimaryContainer = HelixTextPrimary,
    secondary = HelixBluePrimary,
    onSecondary = HelixTextPrimary,
    secondaryContainer = HelixBlueHeader,
    onSecondaryContainer = HelixTextPrimary,
    tertiary = HelixSilverPrimary,
    onTertiary = HelixBackground,
    background = HelixBackground,
    onBackground = HelixTextPrimary,
    surface = HelixSurface,
    onSurface = HelixTextPrimary,
    surfaceVariant = HelixSurfaceVariant,
    onSurfaceVariant = HelixTextSecondary,
    outline = HelixCardBorder
)

@Composable
fun HelixMidiMixerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HelixDarkColorScheme,
        typography = Typography,
        content = content
    )
}
