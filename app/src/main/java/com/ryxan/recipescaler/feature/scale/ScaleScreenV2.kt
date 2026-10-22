package com.ryxan.recipescaler.feature.scale

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ryxan.recipescaler.R
import com.ryxan.recipescaler.core.designsystem.QuantityText
import com.ryxan.recipescaler.core.designsystem.Spacing
import com.ryxan.recipescaler.core.designsystem.V2Card
import com.ryxan.recipescaler.core.designsystem.V2TopAppBar
import com.ryxan.recipescaler.core.domain.formatting.MeasurementSystem
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.ScaleStatus
import com.ryxan.recipescaler.core.domain.model.ScaledIngredient
import com.ryxan.recipescaler.core.domain.model.formatYield
import com.ryxan.recipescaler.core.domain.parsing.QuantityParser
import com.ryxan.recipescaler.core.domain.scaling.CopyFormat
import com.ryxan.recipescaler.navigation.RescaleBottomNav
import com.ryxan.recipescaler.navigation.RescaleTab
import kotlin.math.abs

/**
 * Culinary Material (v2) scale/cook screen — same state and callbacks as
 * [ScaleContent], laid out after the Stitch mock: a "TARGET YIELD" stepper
 * card with quick-scale chips, per-row ingredient cards with a right-aligned
 * quantity, and a bottom Export / Cook action bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleV2Content(
    state: ScaleUiState,
    callbacks: ScaleCallbacks,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var overflowExpanded by remember { mutableStateOf(false) }
    var warningDismissed by rememberSaveable { mutableStateOf(false) }
    var warningExpanded by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val canEdit = !state.cookMode && !state.isLoading && !state.recipeMissing
    val showActions = !state.isLoading && !state.recipeMissing
    val view = LocalView.current
    DisposableEffect(state.cookMode) {
        view.keepScreenOn = state.cookMode
        onDispose { view.keepScreenOn = false }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            V2TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Column {
                        Text(
                            text = state.title.ifBlank {
                                stringResource(R.string.untitled_recipe)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (state.yieldAmount > 0.0) {
                            Text(
                                text = stringResource(
                                    R.string.scale_makes,
                                    formatYield(state.yieldAmount),
                                    state.yieldLabel,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = callbacks.onNavigateUp) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (canEdit) {
                        IconButton(onClick = { callbacks.onEdit(state.recipeId) }) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = stringResource(R.string.action_edit),
                            )
                        }
                    }
                    IconButton(onClick = callbacks.onSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.title_settings),
                        )
                    }
                    if (!state.cookMode) {
                        Box {
                            IconButton(onClick = { overflowExpanded = true }) {
                                Icon(
                                    Icons.Filled.MoreVert,
                                    contentDescription = stringResource(R.string.action_more),
                                )
                            }
                            DropdownMenu(
                                expanded = overflowExpanded,
                                onDismissRequest = { overflowExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(stringResource(R.string.action_share_send))
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.Share,
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        overflowExpanded = false
                                        callbacks.onShare()
                                    },
                                )
                                listOf(
                                    CopyFormat.PLAIN to R.string.copy_format_plain,
                                    CopyFormat.MARKDOWN to R.string.copy_format_markdown,
                                    CopyFormat.SHOPPING_LIST to R.string.copy_format_shopping,
                                ).forEach { (format, labelRes) ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(labelRes)) },
                                        onClick = {
                                            overflowExpanded = false
                                            callbacks.onCopy(format)
                                        },
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Text(stringResource(R.string.scale_show_original))
                                    },
                                    trailingIcon = {
                                        if (state.showOriginal) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    onClick = {
                                        overflowExpanded = false
                                        callbacks.onShowOriginal(!state.showOriginal)
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (showActions) {
                Column {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 3.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.l, vertical = Spacing.m),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilledTonalButton(onClick = callbacks.onShare) {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.size(Spacing.s))
                                Text(stringResource(R.string.action_share))
                            }
                            Button(
                                onClick = callbacks.onCookMode,
                                modifier = Modifier.weight(1f),
                                shape = CircleShape,
                            ) {
                                Icon(
                                    Icons.Outlined.Checklist,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.size(Spacing.s))
                                Text(
                                    stringResource(
                                        if (state.cookMode) R.string.action_cook_done
                                        else R.string.action_cook_mode,
                                    ),
                                )
                            }
                        }
                    }
                    RescaleBottomNav(
                        selected = RescaleTab.SCALER,
                        onRecipesClick = callbacks.onRecipes,
                        onScalerClick = callbacks.onScaler,
                        onSettingsClick = callbacks.onSettings,
                    )
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .testTag("scale_list"),
            contentPadding = PaddingValues(
                start = Spacing.l,
                end = Spacing.l,
                top = Spacing.s,
                bottom = Spacing.l,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            if (state.recipeMissing) {
                item(key = "missing") {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.scale_recipe_missing),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                if (!state.cookMode) {
                    item(key = "controls") {
                        ScaleControlsV2(state = state, callbacks = callbacks)
                    }
                } else {
                    item(key = "cook_hint") {
                        Text(
                            stringResource(R.string.cook_mode_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item(key = "summary") {
                        CookSummaryV2(state)
                    }
                }

                item(key = "warning") {
                    AnimatedVisibility(
                        visible = state.showScaleWarning && !warningDismissed,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            ),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    start = 12.dp,
                                    top = 4.dp,
                                    bottom = 4.dp,
                                    end = 4.dp,
                                ),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.WarningAmber,
                                    contentDescription = null,
                                )
                                Text(
                                    text = stringResource(
                                        if (warningExpanded) {
                                            if (state.factor > 4.0) R.string.scale_warning_big
                                            else R.string.scale_warning_small
                                        } else {
                                            if (state.factor > 4.0) R.string.scale_warning_big_short
                                            else R.string.scale_warning_small_short
                                        },
                                        formatYield(state.factor),
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = if (warningExpanded) Int.MAX_VALUE else 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { warningExpanded = !warningExpanded },
                                )
                                IconButton(onClick = { warningDismissed = true }) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = stringResource(
                                            R.string.action_dismiss,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "rows_header") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.v2_scaled_ingredients),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (state.cookMode && state.rows.isNotEmpty()) {
                            Text(
                                text = stringResource(
                                    R.string.v2_cook_progress,
                                    state.checkedIds.size,
                                    state.rows.size,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                items(state.rows, key = { it.id }) { row ->
                    ScaledRowV2(
                        row = row,
                        showOriginal = state.showOriginal && !state.cookMode,
                        checked = if (state.cookMode) row.id in state.checkedIds else null,
                        onToggle = { callbacks.onToggleChecked(row.id) },
                        promoteEnabled = state.unitsSelectable && !state.cookMode,
                        onPromote = { unit -> callbacks.onPromote(row.id, unit) },
                    )
                }
            }
        }
    }
}

/**
 * "TARGET YIELD" hero card: a "From N servings" pill and the unit-system chip
 * on the meta row, a centred label over big circular − / + steppers, a
 * multiplier badge and quick-scale chips under a divider. Tapping the value
 * or the "from" pill opens the free-form input dialog — the stepper and chips
 * only cover whole-number targets.
 */
@Composable
private fun ScaleControlsV2(
    state: ScaleUiState,
    callbacks: ScaleCallbacks,
) {
    var editingField by remember { mutableStateOf<ScaleField?>(null) }
    val message = state.message
    val targetServings = state.derivedYieldAmount
        ?: QuantityParser.parse(state.inputs.scaleToText)
    val targetText = targetServings?.let { formatYield(it) } ?: "–"
    val servesText = state.inputs.servesText.ifBlank { "–" }
    val currentTarget = stringResource(
        R.string.cd_target_servings,
        targetText,
        state.yieldLabel,
    )
    val editServes = stringResource(R.string.cd_edit_serves)
    val editTarget = stringResource(R.string.cd_edit_scale_to)
    val canStepDown = targetServings == null ||
        targetServings > ScaleViewModel.MIN_SERVINGS + 1e-9
    val canStepUp = targetServings == null ||
        targetServings < ScaleViewModel.MAX_SERVINGS - 1e-9

    V2Card(
        Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Compact pill; the 48dp hit area lives on the invisible box.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(CircleShape)
                        .clickable(onClickLabel = editServes) {
                            editingField = ScaleField.SERVES
                        }
                        .semantics(mergeDescendants = true) {
                            contentDescription = "$servesText, $editServes"
                        },
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                start = Spacing.s,
                                end = Spacing.m,
                                top = Spacing.xs,
                                bottom = Spacing.xs,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) {
                            Icon(
                                Icons.Outlined.Restaurant,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = stringResource(
                                    R.string.v2_scaled_from,
                                    servesText,
                                    state.yieldLabel,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                if (state.unitsSelectable) {
                    UnitSystemChipV2(
                        system = state.measurementSystem,
                        onSelect = callbacks.onMeasurementSystem,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.l),
            ) {
                StepperCircle(
                    onClick = { callbacks.onStep(-1) },
                    enabled = canStepDown,
                    description = stringResource(R.string.action_decrease_servings),
                    filled = false,
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = null)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(onClickLabel = editTarget) {
                            editingField = ScaleField.SCALE_TO
                        }
                        .semantics(mergeDescendants = true) {
                            contentDescription = currentTarget
                        }
                        .padding(vertical = Spacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.v2_target_yield),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    AnimatedContent(
                        targetState = targetText to state.yieldLabel,
                        label = "target",
                    ) { (value, unit) ->
                        Row {
                            Text(
                                text = value,
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.alignByBaseline(),
                            )
                            Text(
                                text = unit,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(start = Spacing.xs)
                                    .alignByBaseline(),
                            )
                        }
                    }
                }
                StepperCircle(
                    onClick = { callbacks.onStep(1) },
                    enabled = canStepUp,
                    description = stringResource(R.string.action_increase_servings),
                    filled = true,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryFixed,
                contentColor = MaterialTheme.colorScheme.onPrimaryFixedVariant,
            ) {
                Text(
                    text = stringResource(
                        R.string.v2_multiplier,
                        formatYield(state.factor),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .padding(horizontal = Spacing.m, vertical = Spacing.xs)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                modifier = Modifier.selectableGroup(),
            ) {
                factorChips.forEach { n ->
                    FilterChip(
                        selected = message == null &&
                            abs(state.factor - n.toDouble()) < 1e-9,
                        onClick = { callbacks.onFactor(n) },
                        label = { Text("${n}×") },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor =
                                MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor =
                                MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    )
                }
            }

            if (message != null) {
                Text(
                    text = messageText(message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    val field = editingField
    if (field != null) {
        AlertDialog(
            onDismissRequest = { editingField = null },
            confirmButton = {
                TextButton(onClick = { editingField = null }) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            text = {
                OutlinedTextField(
                    value = when (field) {
                        ScaleField.SERVES -> state.inputs.servesText
                        ScaleField.SCALE_TO -> state.inputs.scaleToText
                    },
                    onValueChange = when (field) {
                        ScaleField.SERVES -> callbacks.onServes
                        ScaleField.SCALE_TO -> callbacks.onScaleTo
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                    ),
                    label = {
                        Text(
                            stringResource(
                                if (field == ScaleField.SERVES) {
                                    R.string.scale_label_serves
                                } else {
                                    R.string.scale_label_scale_to
                                },
                            ),
                        )
                    },
                    isError = message?.field == field,
                    supportingText = {
                        if (message?.field == field) {
                            Text(messageText(message))
                        }
                    },
                    singleLine = true,
                )
            },
        )
    }
}

/** Big circular stepper: light when unfilled, solid persimmon when filled. */
@Composable
private fun StepperCircle(
    onClick: () -> Unit,
    enabled: Boolean,
    description: String,
    filled: Boolean,
    icon: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(56.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        shape = CircleShape,
        color = when {
            !enabled -> MaterialTheme.colorScheme.surfaceContainerHigh
            filled -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceContainerLowest
        },
        contentColor = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            filled -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.primary
        },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
    }
}

/** Outlined pill showing the active measurement system; opens a picker menu. */
@Composable
private fun UnitSystemChipV2(
    system: MeasurementSystem,
    onSelect: (MeasurementSystem) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val systems = listOf(
        MeasurementSystem.MATCH_RECIPE to R.string.settings_system_match,
        MeasurementSystem.US to R.string.settings_system_us,
        MeasurementSystem.METRIC to R.string.settings_system_metric,
    )
    Box {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.heightIn(min = 48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Row(
                modifier = Modifier.padding(
                    start = Spacing.m,
                    end = Spacing.s,
                    top = Spacing.xs,
                    bottom = Spacing.xs,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = stringResource(
                        systems.first { it.first == system }.second,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            systems.forEach { (option, labelRes) ->
                DropdownMenuItem(
                    text = { Text(stringResource(labelRes)) },
                    trailingIcon = {
                        if (system == option) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun CookSummaryV2(state: ScaleUiState) {
    val factorText = stringResource(R.string.scale_factor_prefix, formatYield(state.factor))
    val derived = state.derivedYieldAmount?.let {
        stringResource(R.string.scale_makes, formatYield(it), state.yieldLabel)
    }
    Row(
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = listOfNotNull(factorText, derived).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.message != null) {
            Text(
                text = stringResource(R.string.scale_input_not_read),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * One ingredient per card: optional cook-mode checkbox, name + captions on
 * the start edge, right-aligned quantity in persimmon with the promote chip
 * below it.
 */
@Composable
private fun ScaledRowV2(
    row: ScaledIngredient,
    showOriginal: Boolean,
    checked: Boolean?,
    onToggle: () -> Unit,
    promoteEnabled: Boolean,
    onPromote: (KitchenUnit?) -> Unit,
) {
    val isChecked = checked == true
    val contentAlpha = if (isChecked) 0.55f else 1f
    val hasPromoteChip = promoteEnabled && row.promoteOptions.isNotEmpty()

    V2Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (checked != null) {
                    Modifier.toggleable(
                        value = checked,
                        role = Role.Checkbox,
                        onValueChange = { onToggle() },
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            if (checked != null) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = null, // handled by the row toggleable
                    modifier = Modifier.size(24.dp),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(contentAlpha),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = row.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isChecked) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (isChecked) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                val captions = buildList {
                    when (row.status) {
                        ScaleStatus.RANGE_LOW_END -> {
                            if (showOriginal) {
                                add(stringResource(R.string.scale_was, row.originalDisplayText))
                            }
                            add(stringResource(R.string.scale_from_range))
                        }
                        ScaleStatus.UNPARSED -> {
                            add(stringResource(R.string.scale_not_scaled))
                        }
                        ScaleStatus.SCALED -> {
                            if (showOriginal) {
                                add(stringResource(R.string.scale_was, row.originalDisplayText))
                            }
                        }
                        else -> Unit
                    }
                }
                captions.forEach { caption ->
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                AnimatedContent(
                    targetState = row.displayText,
                    modifier = Modifier.alpha(contentAlpha),
                    label = "qty",
                ) { text ->
                    QuantityText(
                        text = text,
                        style = MaterialTheme.typography.titleLarge.merge(
                            color = when {
                                isChecked -> MaterialTheme.colorScheme.onSurfaceVariant
                                row.status == ScaleStatus.UNPARSED ->
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.primary
                            },
                            textDecoration = if (isChecked) {
                                TextDecoration.LineThrough
                            } else {
                                null
                            },
                        ),
                    )
                }
                if (hasPromoteChip) {
                    PromoteChipV2(row = row, onPromote = onPromote)
                }
            }
        }
    }
}

@Composable
private fun PromoteChipV2(
    row: ScaledIngredient,
    onPromote: (KitchenUnit?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val promoteCd = stringResource(R.string.cd_promote_unit)

    Box {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier
                .heightIn(min = 48.dp)
                .semantics {
                    contentDescription = promoteCd
                },
            shape = CircleShape,
            color = if (row.promotedUnit != null) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            contentColor = if (row.promotedUnit != null) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            tonalElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.SwapVert,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = row.promotedUnit?.let { promoteUnitName(it) }
                        ?: stringResource(R.string.promote_to),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            row.promoteOptions.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                R.string.promote_option,
                                promoteUnitName(option.unit),
                                option.previewText,
                            ),
                        )
                    },
                    trailingIcon = {
                        if (row.promotedUnit == option.unit) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        }
                    },
                    onClick = {
                        expanded = false
                        onPromote(option.unit)
                    },
                )
            }
            if (row.promotedUnit != null) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.promote_auto)) },
                    onClick = {
                        expanded = false
                        onPromote(null)
                    },
                )
            }
        }
    }
}
