package com.manalejandro.mitension.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = LightError,
    outline = LightOutline,
    surfaceContainer = LightSurfaceContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = DarkError,
    outline = DarkOutline,
    surfaceContainer = DarkSurfaceContainer
)

enum class TextScale(val factor: Float) {
    NORMAL(1f),
    LARGE(1.2f),
    XLARGE(1.4f);

    companion object {
        fun fromName(name: String): TextScale = when (name) {
            "large" -> LARGE
            "xlarge" -> XLARGE
            else -> NORMAL
        }
    }
}

@Immutable
data class AppTypography(
    val typography: Typography
)

val LocalAppTypography = staticCompositionLocalOf {
    AppTypography(Typography)
}

@Composable
fun MiTensionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    textScale: TextScale = TextScale.NORMAL,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val baseTypography = Typography
    val scaled = when (textScale) {
        TextScale.NORMAL -> baseTypography
        else -> baseTypography.let { base ->
            Typography(
                displayLarge = base.displayLarge.scaledBy(textScale.factor),
                displayMedium = base.displayMedium.scaledBy(textScale.factor),
                displaySmall = base.displaySmall.scaledBy(textScale.factor),
                headlineLarge = base.headlineLarge.scaledBy(textScale.factor),
                headlineMedium = base.headlineMedium.scaledBy(textScale.factor),
                headlineSmall = base.headlineSmall.scaledBy(textScale.factor),
                titleLarge = base.titleLarge.scaledBy(textScale.factor),
                titleMedium = base.titleMedium.scaledBy(textScale.factor),
                titleSmall = base.titleSmall.scaledBy(textScale.factor),
                bodyLarge = base.bodyLarge.scaledBy(textScale.factor),
                bodyMedium = base.bodyMedium.scaledBy(textScale.factor),
                bodySmall = base.bodySmall.scaledBy(textScale.factor),
                labelLarge = base.labelLarge.scaledBy(textScale.factor),
                labelMedium = base.labelMedium.scaledBy(textScale.factor),
                labelSmall = base.labelSmall.scaledBy(textScale.factor)
            )
        }
    }

    CompositionLocalProvider(LocalAppTypography provides AppTypography(scaled)) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = scaled,
            content = content
        )
    }
}

private fun TextStyle.scaledBy(factor: Float): TextStyle =
    if (factor == 1f) this else copy(fontSize = fontSize * factor, lineHeight = lineHeight * factor)

