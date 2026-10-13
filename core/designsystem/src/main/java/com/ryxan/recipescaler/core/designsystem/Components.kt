package com.ryxan.recipescaler.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Flat top app bar painted with the brand gradient. The gradient is applied to
 * the whole bar, so it extends behind the status-bar inset, and all content is
 * tinted [RescaleExtendedColors.onGradient].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradientTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val extended = RescaleTheme.extendedColors
    TopAppBar(
        title = title,
        modifier = modifier.background(RescaleTheme.brandGradient),
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
            titleContentColor = extended.onGradient,
            navigationIconContentColor = extended.onGradient,
            actionIconContentColor = extended.onGradient,
        ),
        scrollBehavior = scrollBehavior,
    )
}

/**
 * Flat extended FAB painted with the brand gradient. Keeps the semantics and
 * click target of [ExtendedFloatingActionButton].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradientExtendedFab(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
) {
    val shape = FloatingActionButtonDefaults.extendedFabShape
    ExtendedFloatingActionButton(
        text = text,
        icon = icon,
        onClick = onClick,
        modifier = modifier.background(RescaleTheme.brandGradient, shape),
        expanded = expanded,
        shape = shape,
        containerColor = Color.Transparent,
        contentColor = RescaleTheme.extendedColors.onGradient,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
        ),
    )
}

/** Flat pill button painted with the brand gradient; falls back to a tonal surface when disabled. */
@Composable
fun GradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = ButtonDefaults.shape
    Button(
        onClick = onClick,
        modifier = modifier.then(
            if (enabled) Modifier.background(RescaleTheme.brandGradient, shape) else Modifier,
        ),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = RescaleTheme.extendedColors.onGradient,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            disabledElevation = 0.dp,
        ),
        content = content,
    )
}

/**
 * Flat grouping surface: large shape, [ColorScheme]-level container, hairline
 * outline, zero elevation.
 */
@Composable
fun RescaleCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    )
    val border = BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    val shape = MaterialTheme.shapes.large
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            content = content,
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            content = content,
        )
    }
}

/** Label for a grouped section (settings groups, list headers). */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(
            start = Spacing.l,
            end = Spacing.l,
            top = Spacing.l,
            bottom = Spacing.s,
        ),
    )
}

// ── Previews ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "GradientTopAppBar — light", showBackground = true)
@Composable
private fun GradientTopAppBarLightPreview() {
    RescaleTheme(darkTheme = false, dynamicColor = false) {
        GradientTopAppBar(title = { Text("Recipes") })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "GradientTopAppBar — dark", showBackground = true)
@Composable
private fun GradientTopAppBarDarkPreview() {
    RescaleTheme(darkTheme = true, dynamicColor = false) {
        GradientTopAppBar(title = { Text("Recipes") })
    }
}

@Preview(name = "GradientExtendedFab — light", showBackground = true)
@Composable
private fun GradientExtendedFabLightPreview() {
    RescaleTheme(darkTheme = false, dynamicColor = false) {
        GradientExtendedFab(
            text = { Text("New recipe") },
            icon = {},
            onClick = {},
        )
    }
}

@Preview(name = "GradientExtendedFab — dark", showBackground = true)
@Composable
private fun GradientExtendedFabDarkPreview() {
    RescaleTheme(darkTheme = true, dynamicColor = false) {
        GradientExtendedFab(
            text = { Text("New recipe") },
            icon = {},
            onClick = {},
        )
    }
}

@Preview(name = "GradientButton — light", showBackground = true)
@Composable
private fun GradientButtonLightPreview() {
    RescaleTheme(darkTheme = false, dynamicColor = false) {
        GradientButton(onClick = {}) { Text("Subscribe") }
    }
}

@Preview(name = "GradientButton — dark", showBackground = true)
@Composable
private fun GradientButtonDarkPreview() {
    RescaleTheme(darkTheme = true, dynamicColor = false) {
        GradientButton(onClick = {}) { Text("Subscribe") }
    }
}

@Preview(name = "RescaleCard — light", showBackground = true)
@Composable
private fun RescaleCardLightPreview() {
    RescaleTheme(darkTheme = false, dynamicColor = false) {
        RescaleCard { Text("Card content", modifier = Modifier.padding(Spacing.l)) }
    }
}

@Preview(name = "RescaleCard — dark", showBackground = true)
@Composable
private fun RescaleCardDarkPreview() {
    RescaleTheme(darkTheme = true, dynamicColor = false) {
        RescaleCard { Text("Card content", modifier = Modifier.padding(Spacing.l)) }
    }
}

@Preview(name = "SectionHeader — light", showBackground = true)
@Composable
private fun SectionHeaderLightPreview() {
    RescaleTheme(darkTheme = false, dynamicColor = false) {
        SectionHeader("Measurement")
    }
}

@Preview(name = "SectionHeader — dark", showBackground = true)
@Composable
private fun SectionHeaderDarkPreview() {
    RescaleTheme(darkTheme = true, dynamicColor = false) {
        SectionHeader("Measurement")
    }
}
