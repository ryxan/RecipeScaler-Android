package com.ryxan.recipescaler.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = HerbPrimary,
    onPrimary = OnHerbPrimary,
    primaryContainer = HerbPrimaryContainer,
    onPrimaryContainer = OnHerbPrimaryContainer,
    secondary = HerbSecondary,
    onSecondary = OnHerbSecondary,
    secondaryContainer = HerbSecondaryContainer,
    onSecondaryContainer = OnHerbSecondaryContainer,
    tertiary = CitrusTertiary,
    onTertiary = OnCitrusTertiary,
    tertiaryContainer = CitrusTertiaryContainer,
    onTertiaryContainer = OnCitrusTertiaryContainer,
    background = HerbBackground,
    onBackground = OnHerbBackground,
    surface = HerbBackground,
    onSurface = OnHerbBackground,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    outlineVariant = OutlineVariantLight,
)

private val DarkColors = darkColorScheme(
    primary = HerbPrimaryDark,
    onPrimary = OnHerbPrimaryDark,
    primaryContainer = HerbPrimaryContainerDark,
    onPrimaryContainer = OnHerbPrimaryContainerDark,
    secondary = HerbSecondaryDark,
    onSecondary = OnHerbSecondaryDark,
    secondaryContainer = HerbSecondaryContainerDark,
    onSecondaryContainer = OnHerbSecondaryContainerDark,
    tertiary = CitrusTertiaryDark,
    onTertiary = OnCitrusTertiaryDark,
    tertiaryContainer = CitrusTertiaryContainerDark,
    onTertiaryContainer = OnCitrusTertiaryContainerDark,
    background = HerbBackgroundDark,
    onBackground = OnHerbBackgroundDark,
    surface = HerbBackgroundDark,
    onSurface = OnHerbBackgroundDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    outlineVariant = OutlineVariantDark,
)

/** Brand colors outside the M3 [ColorScheme]: the accent gradient and its content color. */
@Immutable
data class RescaleExtendedColors(
    val gradientStart: Color,
    val gradientEnd: Color,
    val onGradient: Color,
)

val LocalRescaleExtendedColors = staticCompositionLocalOf {
    RescaleExtendedColors(
        gradientStart = BrandGradientStartLight,
        gradientEnd = BrandGradientEndLight,
        onGradient = OnBrandGradient,
    )
}

object RescaleTheme {
    val extendedColors: RescaleExtendedColors
        @Composable get() = LocalRescaleExtendedColors.current

    /** Top-start → bottom-end brand gradient. Always dark enough for [RescaleExtendedColors.onGradient] content. */
    val brandGradient: Brush
        @Composable get() {
            val colors = extendedColors
            return Brush.linearGradient(
                colors = listOf(colors.gradientStart, colors.gradientEnd),
                start = Offset.Zero,
                end = Offset.Infinite,
            )
        }
}

@Composable
fun RescaleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dynamicActive = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        dynamicActive -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val extendedColors = when {
        dynamicActive && darkTheme -> RescaleExtendedColors(
            gradientStart = colorScheme.primaryContainer,
            gradientEnd = colorScheme.secondaryContainer,
            onGradient = colorScheme.onPrimaryContainer,
        )
        dynamicActive -> RescaleExtendedColors(
            gradientStart = colorScheme.primary,
            gradientEnd = colorScheme.tertiary,
            onGradient = colorScheme.onPrimary,
        )
        darkTheme -> RescaleExtendedColors(
            gradientStart = BrandGradientStartDark,
            gradientEnd = BrandGradientEndDark,
            onGradient = OnBrandGradient,
        )
        else -> RescaleExtendedColors(
            gradientStart = BrandGradientStartLight,
            gradientEnd = BrandGradientEndLight,
            onGradient = OnBrandGradient,
        )
    }

    CompositionLocalProvider(LocalRescaleExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = RescaleShapes,
            typography = Typography,
            content = content,
        )
    }
}
