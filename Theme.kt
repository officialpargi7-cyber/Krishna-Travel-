package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = KrishnaCrimsonLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF450A0A),
    onPrimaryContainer = Color(0xFFFECACA),
    secondary = HighwayAmberBright,
    onSecondary = AsphaltSlate,
    secondaryContainer = Color(0xFF451A03),
    onSecondaryContainer = HighwayAmberContainer,
    tertiary = VerifiedTealBright,
    onTertiary = AsphaltSlate,
    tertiaryContainer = Color(0xFF042F2E),
    onTertiaryContainer = VerifiedTealContainer,
    background = NightBackground,
    onBackground = NightTextPrimary,
    surface = NightSurface,
    onSurface = NightTextPrimary,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightTextSecondary,
    outline = NightOutline,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

private val LightColorScheme = lightColorScheme(
    primary = KrishnaCrimson,
    onPrimary = Color.White,
    primaryContainer = KrishnaCrimsonContainer,
    onPrimaryContainer = OnKrishnaCrimsonContainer,
    secondary = HighwayAmber,
    onSecondary = Color.White,
    secondaryContainer = HighwayAmberContainer,
    onSecondaryContainer = OnHighwayAmberContainer,
    tertiary = VerifiedTeal,
    onTertiary = Color.White,
    tertiaryContainer = VerifiedTealContainer,
    onTertiaryContainer = OnVerifiedTealContainer,
    background = PaperBackground,
    onBackground = PaperTextPrimary,
    surface = PaperSurface,
    onSurface = PaperTextPrimary,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = PaperTextSecondary,
    outline = PaperOutline,
    error = ExpenseRose,
    onError = Color.White
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KrishnaTravelsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
