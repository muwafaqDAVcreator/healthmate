package com.healthmate.app.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Indigo brand colour used for the logo, buttons and selected items. */
val BrandIndigo = Color(0xFF4F46E5)
val BrandViolet = Color(0xFF8B5CF6)

// Indigo / violet Material 3 tonal colours.
private val LightColors = lightColorScheme(
    primary = BrandIndigo,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE2E0FF),
    onPrimaryContainer = Color(0xFF15105E),
    secondary = Color(0xFF5D5C72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3E0F9),
    onSecondaryContainer = Color(0xFF1A1A2C),
    tertiary = Color(0xFF7C4DCC),
    tertiaryContainer = Color(0xFFEDDCFF),
    background = Color(0xFFF6F6FB),
    onBackground = Color(0xFF1A1B23),
    surface = Color(0xFFF6F6FB),
    onSurface = Color(0xFF1A1B23),
    surfaceVariant = Color(0xFFE4E1EC),
    onSurfaceVariant = Color(0xFF47464F),
    outline = Color(0xFF787680),
    outlineVariant = Color(0xFFC8C5D0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F0F7),
    surfaceContainer = Color(0xFFEAEAF2),
    surfaceContainerHigh = Color(0xFFE4E4EC),
    surfaceContainerHighest = Color(0xFFDEDEE7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC1C1FF),
    onPrimary = Color(0xFF221A8F),
    primaryContainer = Color(0xFF3B33B8),
    onPrimaryContainer = Color(0xFFE2E0FF),
    secondary = Color(0xFFC6C4DD),
    onSecondary = Color(0xFF2F2F42),
    secondaryContainer = Color(0xFF45455A),
    onSecondaryContainer = Color(0xFFE3E0F9),
    tertiary = Color(0xFFD6BAFF),
    tertiaryContainer = Color(0xFF5A2EA6),
    background = Color(0xFF121318),
    onBackground = Color(0xFFE4E1EA),
    surface = Color(0xFF121318),
    onSurface = Color(0xFFE4E1EA),
    surfaceVariant = Color(0xFF47464F),
    onSurfaceVariant = Color(0xFFC8C5D0),
    outline = Color(0xFF928F9A),
    outlineVariant = Color(0xFF47464F),
    // Cards use the "lowest" container, so in dark mode it is slightly
    // lighter than the background to keep cards visible.
    surfaceContainerLowest = Color(0xFF1C1D25),
    surfaceContainerLow = Color(0xFF1A1B21),
    surfaceContainer = Color(0xFF1F1F26),
    surfaceContainerHigh = Color(0xFF292930),
    surfaceContainerHighest = Color(0xFF34343B),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

/** Light or dark theme, following the phone setting. */
@Composable
fun HealthMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        content = content,
    )
}
