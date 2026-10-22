package com.ryxan.recipescaler.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// "Culinary Material" (v2) building blocks. These assume the v2 color scheme,
// type scale and shapes are active (RescaleTheme(uiV2 = true)) — they read
// everything from MaterialTheme so they also follow dynamic color.

/**
 * Flat top app bar on `surface` that morphs into `surfaceContainer` on scroll.
 * Unlike [GradientTopAppBar] there is no gradient — separation comes from the
 * warm tonal step.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V2TopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        scrollBehavior = scrollBehavior,
    )
}

/**
 * Hero-card surface: 28dp corners, `surfaceContainerLow`, a whisper of warm
 * elevation. Used for recipe cards and grouped panels.
 */
@Composable
fun V2Card(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable () -> Unit,
) {
    val shape = MaterialTheme.shapes.extraLarge
    val colors = CardDefaults.cardColors(containerColor = containerColor)
    val elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, elevation = elevation) {
            content()
        }
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, elevation = elevation) {
            content()
        }
    }
}

/** All-caps group label ("KITCHEN PERSONA") used to head grouped sections. */
@Composable
fun V2SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Spacing.l, end = Spacing.l, top = Spacing.l, bottom = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        if (trailing != null) {
            Spacer(Modifier.weight(1f))
            trailing()
        }
    }
}

/** Rounded meta badge ("4 servings") with a leading icon, `surfaceContainer` fill. */
@Composable
fun V2MetaPill(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Row(
        modifier = modifier
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Pill search field on `surfaceContainerLow`: leading search icon and a
 * trailing slot (clear/tune). Mirrors the Stitch "Search & Expressive Input".
 */
@Composable
fun V2SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        shape = CircleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
        ),
        placeholder = {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
            )
        },
        leadingIcon = {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        },
        trailingIcon = trailingIcon,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

/**
 * Deep-persimmon extended FAB (`primaryContainer` fill, `onPrimaryContainer`
 * content) — the v2 accent action. Collapses to a circle on scroll via
 * [expanded].
 */
@Composable
fun V2ExtendedFab(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
) {
    ExtendedFloatingActionButton(
        text = text,
        icon = icon,
        onClick = onClick,
        modifier = modifier,
        expanded = expanded,
        shape = FloatingActionButtonDefaults.extendedFabShape,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 3.dp,
        ),
    )
}

/** Fling velocity (px/s) past which a reveal swipe always opens (or closes). */
private const val REVEAL_FLING_VELOCITY = 800f

/**
 * Swipe-reveal delete row (v2): dragging the content end-to-start slides it
 * aside to uncover a destructive trash strip on `errorContainer`. The row
 * settles open once past half the action width (or on a hard fling) and
 * [onDelete] fires only when the revealed strip is tapped — the swipe alone
 * never deletes. Tapping the row while it's open, or swiping it back, closes
 * the reveal without deleting.
 *
 * Unlike v1's [RescaleSwipeToDelete] this needs no Undo snackbar: the delete
 * is a deliberate tap. Keep a custom accessibility action on the content so
 * deletion isn't gesture-only.
 */
@Composable
fun V2SwipeToDelete(
    onDelete: () -> Unit,
    deleteLabel: String,
    modifier: Modifier = Modifier,
    actionWidth: Dp = 96.dp,
    dismissLabel: String? = null,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val actionPx = with(density) { actionWidth.toPx() }
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    var open by remember { mutableStateOf(false) }
    val revealed = offset.value != 0f

    Box(modifier) {
        // Revealed layer, painted card-wide so the exposed edge follows the
        // card shape; only the trailing actionWidth slice is ever uncovered.
        // matchParentSize (not fillMaxSize): in a LazyColumn the item's max
        // height is infinite, so fillMax* would collapse to the icon's height.
        // Only composed while displaced — at rest the pink would bleed through
        // the card's anti-aliased corners.
        if (revealed) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.errorContainer),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(actionWidth)
                        .clickable {
                            open = false
                            scope.launch { offset.animateTo(0f) }
                            onDelete()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = deleteLabel,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
        // Draggable foreground, clamped to [-actionPx, 0] so it only opens
        // end-to-start.
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            offset.snapTo((offset.value + delta).coerceIn(-actionPx, 0f))
                        }
                    },
                    onDragStopped = { velocity ->
                        scope.launch {
                            val stayOpen = when {
                                velocity < -REVEAL_FLING_VELOCITY -> true
                                velocity > REVEAL_FLING_VELOCITY -> false
                                else -> offset.value < -actionPx / 2f
                            }
                            offset.animateTo(if (stayOpen) -actionPx else 0f)
                            open = stayOpen
                        }
                    },
                ),
        ) {
            content()
            if (open) {
                // While open, a tap on the row closes the reveal instead of
                // reaching the content's own click handler.
                Box(
                    Modifier
                        .matchParentSize()
                        .semantics {
                            if (dismissLabel != null) contentDescription = dismissLabel
                        }
                        .clickable {
                            open = false
                            scope.launch { offset.animateTo(0f) }
                        },
                )
            }
        }
    }
}

// ── Previews ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "V2 components — light", showBackground = true)
@Composable
private fun V2ComponentsLightPreview() {
    RescaleTheme(darkTheme = false, dynamicColor = false, uiV2 = true) {
        androidx.compose.foundation.layout.Column {
            V2TopAppBar(title = { Text("Rescale") })
            V2SectionHeader("Your recipes")
            V2SearchField(
                value = "",
                onValueChange = {},
                placeholder = "Search recipes",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.l),
            )
            V2Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.l),
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.l),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                ) {
                    V2MetaPill(text = "4 servings", icon = Icons.Filled.Star)
                }
            }
            V2ExtendedFab(
                text = { Text("New recipe") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = {},
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.CenterHorizontally)
                    .padding(Spacing.l),
            )
            Spacer(Modifier.height(Spacing.l))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "V2 components — dark", showBackground = true)
@Composable
private fun V2ComponentsDarkPreview() {
    RescaleTheme(darkTheme = true, dynamicColor = false, uiV2 = true) {
        V2TopAppBar(title = { Text("Rescale") })
    }
}
