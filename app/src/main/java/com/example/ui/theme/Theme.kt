package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val CodeXDarkColorScheme = darkColorScheme(
    primary = CodeXCrimson,
    onPrimary = CodeXWhite,
    primaryContainer = CodeXCrimsonContainer,
    onPrimaryContainer = Color(0xFFFFDADA),
    secondary = CodeXWhite,
    onSecondary = CodeXBlack,
    secondaryContainer = CodeXSurfaceHighlight,
    onSecondaryContainer = CodeXGrayLight,
    tertiary = CodeXInfo,
    onTertiary = CodeXBlack,
    tertiaryContainer = CodeXInfoContainer,
    onTertiaryContainer = Color(0xFFD0F0FF),
    background = CodeXBlack,
    onBackground = CodeXWhite,
    surface = CodeXSurface,
    onSurface = CodeXWhite,
    surfaceVariant = CodeXSurfaceElevated,
    onSurfaceVariant = CodeXGrayMuted,
    surfaceContainerLowest = CodeXBlack,
    surfaceContainerLow = CodeXSurface,
    surfaceContainer = CodeXSurfaceElevated,
    surfaceContainerHigh = CodeXSurfaceHighlight,
    surfaceContainerHighest = Color(0xFF2B2B36),
    outline = CodeXBorder,
    outlineVariant = Color(0xFF1E1E26),
    error = CodeXError,
    onError = CodeXWhite,
    errorContainer = CodeXErrorContainer,
    onErrorContainer = Color(0xFFFFD6D6)
)

private val CodeXLightColorScheme = lightColorScheme(
    primary = CodeXCrimson,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF1F242D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E7EB),
    onSecondaryContainer = Color(0xFF111827),
    background = Color(0xFFF4F5F7),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE8EAEE),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White
)

val CodeXShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun CodeXTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CodeXDarkColorScheme else CodeXLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CodeXTypography,
        shapes = CodeXShapes,
        content = content
    )
}
