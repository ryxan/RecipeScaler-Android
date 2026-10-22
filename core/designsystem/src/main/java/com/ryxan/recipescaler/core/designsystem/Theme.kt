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

// "Culinary Material" (v2) schemes — active only when the experimental
// "New look" flag is on. Light values from the Stitch DESIGN.md spec;
// dark is the standard M3 tonal inversion.
private val V2LightColors = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = OnTerracottaPrimary,
    primaryContainer = TerracottaPrimaryContainer,
    onPrimaryContainer = OnTerracottaPrimaryContainer,
    secondary = UmberSecondary,
    onSecondary = OnUmberSecondary,
    secondaryContainer = UmberSecondaryContainer,
    onSecondaryContainer = OnUmberSecondaryContainer,
    tertiary = RosemaryTertiary,
    onTertiary = OnRosemaryTertiary,
    tertiaryContainer = RosemaryTertiaryContainer,
    onTertiaryContainer = OnRosemaryTertiaryContainer,
    error = WarmError,
    onError = OnWarmError,
    errorContainer = WarmErrorContainer,
    onErrorContainer = OnWarmErrorContainer,
    background = CulinaryBackground,
    onBackground = OnCulinaryBackground,
    surface = CulinaryBackground,
    onSurface = OnCulinaryBackground,
    surfaceVariant = SurfaceVariantV2Light,
    onSurfaceVariant = OnSurfaceVariantV2Light,
    surfaceContainerLowest = SurfaceContainerLowestV2Light,
    surfaceContainerLow = SurfaceContainerLowV2Light,
    surfaceContainer = SurfaceContainerV2Light,
    surfaceContainerHigh = SurfaceContainerHighV2Light,
    surfaceContainerHighest = SurfaceContainerHighestV2Light,
    outline = OutlineV2Light,
    outlineVariant = OutlineVariantV2Light,
    inverseSurface = InverseSurfaceV2Light,
    inverseOnSurface = InverseOnSurfaceV2Light,
    inversePrimary = InversePrimaryV2Light,
    surfaceTint = SurfaceTintV2Light,
    primaryFixed = PrimaryFixedV2,
    primaryFixedDim = PrimaryFixedDimV2,
    onPrimaryFixed = OnPrimaryFixedV2,
    onPrimaryFixedVariant = OnPrimaryFixedVariantV2,
    secondaryFixed = SecondaryFixedV2,
    secondaryFixedDim = SecondaryFixedDimV2,
    onSecondaryFixed = OnSecondaryFixedV2,
    onSecondaryFixedVariant = OnSecondaryFixedVariantV2,
    tertiaryFixed = TertiaryFixedV2,
    tertiaryFixedDim = TertiaryFixedDimV2,
    onTertiaryFixed = OnTertiaryFixedV2,
    onTertiaryFixedVariant = OnTertiaryFixedVariantV2,
)

private val V2DarkColors = darkColorScheme(
    primary = TerracottaPrimaryDark,
    onPrimary = OnTerracottaPrimaryDark,
    primaryContainer = TerracottaPrimaryContainerDark,
    onPrimaryContainer = OnTerracottaPrimaryContainerDark,
    secondary = UmberSecondaryDark,
    onSecondary = OnUmberSecondaryDark,
    secondaryContainer = UmberSecondaryContainerDark,
    onSecondaryContainer = OnUmberSecondaryContainerDark,
    tertiary = RosemaryTertiaryDark,
    onTertiary = OnRosemaryTertiaryDark,
    tertiaryContainer = RosemaryTertiaryContainerDark,
    onTertiaryContainer = OnRosemaryTertiaryContainerDark,
    error = WarmErrorDark,
    onError = OnWarmErrorDark,
    errorContainer = WarmErrorContainerDark,
    onErrorContainer = OnWarmErrorContainerDark,
    background = CulinaryBackgroundDark,
    onBackground = OnCulinaryBackgroundDark,
    surface = CulinaryBackgroundDark,
    onSurface = OnCulinaryBackgroundDark,
    surfaceVariant = SurfaceVariantV2Dark,
    onSurfaceVariant = OnSurfaceVariantV2Dark,
    surfaceContainerLowest = SurfaceContainerLowestV2Dark,
    surfaceContainerLow = SurfaceContainerLowV2Dark,
    surfaceContainer = SurfaceContainerV2Dark,
    surfaceContainerHigh = SurfaceContainerHighV2Dark,
    surfaceContainerHighest = SurfaceContainerHighestV2Dark,
    outline = OutlineV2Dark,
    outlineVariant = OutlineVariantV2Dark,
    inverseSurface = InverseSurfaceV2Dark,
    inverseOnSurface = InverseOnSurfaceV2Dark,
    inversePrimary = InversePrimaryV2Dark,
    surfaceTint = SurfaceTintV2Dark,
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

/**
 * Whether the experimental "v2" UI is enabled (Settings → Theme). v2
 * composables/styles live alongside v1 and read this to swap in; flipping the
 * flag recomposes the whole tree since this is a static local.
 */
val LocalUiV2 = staticCompositionLocalOf { false }

object RescaleTheme {
    val extendedColors: RescaleExtendedColors
        @Composable get() = LocalRescaleExtendedColors.current

    val uiV2: Boolean
        @Composable get() = LocalUiV2.current

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
    uiV2: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dynamicActive = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        dynamicActive -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        uiV2 && darkTheme -> V2DarkColors
        uiV2 -> V2LightColors
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
        uiV2 && darkTheme -> RescaleExtendedColors(
            gradientStart = BrandGradientStartV2Dark,
            gradientEnd = BrandGradientEndV2Dark,
            onGradient = OnBrandGradientV2,
        )
        uiV2 -> RescaleExtendedColors(
            gradientStart = BrandGradientStartV2Light,
            gradientEnd = BrandGradientEndV2Light,
            onGradient = OnBrandGradientV2,
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

    CompositionLocalProvider(
        LocalRescaleExtendedColors provides extendedColors,
        LocalUiV2 provides uiV2,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = if (uiV2) RescaleShapesV2 else RescaleShapes,
            typography = if (uiV2) TypographyV2 else Typography,
            content = content,
        )
    }
}
