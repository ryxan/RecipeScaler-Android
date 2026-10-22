package com.ryxan.recipescaler.feature.scale

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ryxan.recipescaler.R
import com.ryxan.recipescaler.core.designsystem.GradientTopAppBar
import com.ryxan.recipescaler.core.designsystem.QuantityText
import com.ryxan.recipescaler.core.designsystem.RescaleCard
import com.ryxan.recipescaler.core.designsystem.RescaleTheme
import com.ryxan.recipescaler.core.designsystem.Spacing
import com.ryxan.recipescaler.core.domain.formatting.MeasurementSystem
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.ScaleStatus
import com.ryxan.recipescaler.core.domain.model.ScaledIngredient
import com.ryxan.recipescaler.core.domain.model.formatYield
import com.ryxan.recipescaler.core.domain.parsing.QuantityParser
import com.ryxan.recipescaler.core.domain.scaling.CopyFormat
import com.ryxan.recipescaler.navigation.RescaleBottomNav
import com.ryxan.recipescaler.navigation.RescaleTab
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Multiplier chips — each writes serves × n into "Scale to". */
internal val factorChips = listOf(1, 2, 3, 5)

/** Upper bound for the shared quantity column; longer quantities wrap. */
private val MAX_QTY_WIDTH = 136.dp

@Composable
fun ScaleScreen(
    onNavigateUp: () -> Unit,
    onEdit: (Long) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToRecipes: () -> Unit = {},
    onScalerClick: () -> Unit = {},
    /** Fired when the recipe is gone (e.g. deleted) so callers can drop it. */
    onRecipeMissing: () -> Unit = {},
    viewModel: ScaleViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.recipeMissing) {
        if (state.recipeMissing) onRecipeMissing()
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val copied = stringResource(R.string.msg_copied)

    val callbacks = ScaleCallbacks(
        onNavigateUp = onNavigateUp,
        onEdit = onEdit,
        onSettings = onNavigateToSettings,
        onServes = viewModel::setServes,
        onScaleTo = viewModel::setScaleTo,
        onFactor = viewModel::applyFactor,
        onStep = viewModel::stepServings,
        onShowOriginal = viewModel::setShowOriginal,
        onCookMode = viewModel::toggleCookMode,
        onToggleChecked = viewModel::toggleChecked,
        onPromote = viewModel::promote,
        onMeasurementSystem = viewModel::setMeasurementSystem,
        onRecipes = onNavigateToRecipes,
        onScaler = onScalerClick,
        onCopy = { format ->
            scope.launch {
                clipboard.setClipEntry(
                    ClipEntry(ClipData.newPlainText(null, viewModel.copyText(format))),
                )
                snackbarHostState.showSnackbar(copied)
            }
        },
        onShare = {
            val send = Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, viewModel.copyText(CopyFormat.PLAIN))
            context.startActivity(Intent.createChooser(send, null))
        },
    )

    if (RescaleTheme.uiV2) {
        ScaleV2Content(state = state, callbacks = callbacks, snackbarHostState = snackbarHostState)
    } else {
        ScaleContent(state = state, callbacks = callbacks, snackbarHostState = snackbarHostState)
    }
}

data class ScaleCallbacks(
    val onNavigateUp: () -> Unit = {},
    val onEdit: (Long) -> Unit = {},
    val onSettings: () -> Unit = {},
    val onServes: (String) -> Unit = {},
    val onScaleTo: (String) -> Unit = {},
    val onFactor: (Int) -> Unit = {},
    val onStep: (Int) -> Unit = {},
    val onShowOriginal: (Boolean) -> Unit = {},
    val onCookMode: () -> Unit = {},
    val onToggleChecked: (String) -> Unit = {},
    val onPromote: (String, KitchenUnit?) -> Unit = { _, _ -> },
    val onCopy: (CopyFormat) -> Unit = {},
    val onShare: () -> Unit = {},
    val onMeasurementSystem: (MeasurementSystem) -> Unit = {},
    /** Bottom nav: Recipes tab → pop back to the recipe list root. */
    val onRecipes: () -> Unit = {},
    /** Bottom nav: Scaler tab → back to the scaler home (tab root). */
    val onScaler: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleContent(
    state: ScaleUiState,
    callbacks: ScaleCallbacks,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var overflowExpanded by remember { mutableStateOf(false) }
    var warningDismissed by rememberSaveable { mutableStateOf(false) }
    var warningExpanded by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val fabExpanded by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }
    val canEdit = !state.cookMode && !state.isLoading && !state.recipeMissing
    val view = LocalView.current
    DisposableEffect(state.cookMode) {
        view.keepScreenOn = state.cookMode
        onDispose { view.keepScreenOn = false }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GradientTopAppBar(
                title = {
                    Text(
                        state.title.ifBlank {
                            stringResource(R.string.untitled_recipe)
                        },
                    )
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(onClick = callbacks.onCookMode)
                            .padding(horizontal = 12.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Checklist,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(
                                if (state.cookMode) R.string.action_cook_done
                                else R.string.action_cook,
                            ),
                        )
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
                                if (state.unitsSelectable) {
                                    HorizontalDivider()
                                    listOf(
                                        MeasurementSystem.MATCH_RECIPE to
                                            R.string.settings_system_match,
                                        MeasurementSystem.US to R.string.settings_system_us,
                                        MeasurementSystem.METRIC to
                                            R.string.settings_system_metric,
                                    ).forEach { (system, labelRes) ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(stringResource(labelRes))
                                            },
                                            trailingIcon = {
                                                if (state.measurementSystem == system) {
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = null,
                                                    )
                                                }
                                            },
                                            onClick = {
                                                overflowExpanded = false
                                                callbacks.onMeasurementSystem(system)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (canEdit) {
                ExtendedFloatingActionButton(
                    onClick = { callbacks.onEdit(state.recipeId) },
                    expanded = fabExpanded,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = {
                        Icon(Icons.Outlined.Edit, contentDescription = null)
                    },
                    text = { Text(stringResource(R.string.action_edit)) },
                )
            }
        },
        bottomBar = {
            RescaleBottomNav(
                selected = RescaleTab.SCALER,
                onRecipesClick = callbacks.onRecipes,
                onScalerClick = callbacks.onScaler,
                onSettingsClick = callbacks.onSettings,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = if (canEdit) 88.dp else 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    ScaleControls(state = state, callbacks = callbacks)
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
                    SummaryLine(state)
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

            item(key = "results") {
                RescaleCard(Modifier.fillMaxWidth()) {
                    // Every row shares one quantity-column width — the widest
                    // displayText in the list — so name columns start on a
                    // single vertical line. Capped so an unusually long
                    // quantity wraps instead of crushing the name.
                    val textMeasurer = rememberTextMeasurer()
                    val qtyStyle = MaterialTheme.typography.titleMedium.merge(
                        fontFeatureSettings = "tnum",
                    )
                    val density = LocalDensity.current
                    val qtyWidth = remember(state.rows) {
                        state.rows.maxOfOrNull { row ->
                            textMeasurer.measure(row.displayText, qtyStyle).size.width
                        }?.let { with(density) { it.toDp() } }
                            ?.coerceAtMost(MAX_QTY_WIDTH) ?: 0.dp
                    }
                    Column {
                        state.rows.forEachIndexed { index, row ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = Spacing.m),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                    thickness = 0.5.dp,
                                )
                            }
                            ScaledRow(
                                row = row,
                                qtyWidth = qtyWidth,
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
        }
    }
}

/**
 * Servings control card: a segmented − / target / + stepper, "MAKING: n"
 * caption, ×-multiplier chips, and the "Scaled from original" line. Tapping
 * the stepper value or the original-servings badge opens a free-form input
 * dialog, since the stepper and chips only cover whole-number targets.
 */
@Composable
private fun ScaleControls(
    state: ScaleUiState,
    callbacks: ScaleCallbacks,
) {
    var editingField by remember { mutableStateOf<ScaleField?>(null) }
    val message = state.message
    val targetServings = state.derivedYieldAmount
        ?: QuantityParser.parse(state.inputs.scaleToText)
    val targetText = targetServings?.let { formatYield(it) } ?: "–"
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

    RescaleCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(MaterialTheme.shapes.large)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        MaterialTheme.shapes.large,
                    ),
            ) {
                StepperButton(
                    onClick = { callbacks.onStep(-1) },
                    enabled = canStepDown,
                    description = stringResource(R.string.action_decrease_servings),
                    icon = { Icon(Icons.Filled.Remove, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(onClickLabel = editTarget) {
                            editingField = ScaleField.SCALE_TO
                        }
                        .semantics(mergeDescendants = true) {
                            contentDescription = currentTarget
                        },
                ) {
                    AnimatedContent(targetState = targetText, label = "target") { text ->
                        Text(text, style = MaterialTheme.typography.displaySmall)
                    }
                }
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                StepperButton(
                    onClick = { callbacks.onStep(1) },
                    enabled = canStepUp,
                    description = stringResource(R.string.action_increase_servings),
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = stringResource(
                    R.string.scale_making,
                    targetText,
                    state.yieldLabel,
                ).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )

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
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor =
                                MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val servesText = state.inputs.servesText.ifBlank { "–" }
                Text(
                    stringResource(R.string.scale_scaled_from_prefix),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(Spacing.xs))
                // Small visual badge, full-size touch target around it.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClickLabel = editServes) {
                            editingField = ScaleField.SERVES
                        }
                        .semantics(mergeDescendants = true) {
                            contentDescription = "$servesText, $editServes"
                        },
                ) {
                    Text(
                        servesText,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(6.dp),
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    stringResource(
                        R.string.scale_scaled_from_suffix,
                        state.yieldLabel,
                        formatYield(state.factor),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

/** One tappable segment of the stepper, greyed out when disabled. */
@Composable
private fun StepperButton(
    onClick: () -> Unit,
    enabled: Boolean,
    description: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .background(
                if (enabled) {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            )
            .semantics(mergeDescendants = true) { contentDescription = description }
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
        ) {
            icon()
        }
    }
}

@Composable
private fun SummaryLine(state: ScaleUiState) {
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

@Composable
private fun ScaledRow(
    row: ScaledIngredient,
    qtyWidth: Dp,
    showOriginal: Boolean,
    checked: Boolean?,
    onToggle: () -> Unit,
    promoteEnabled: Boolean,
    onPromote: (KitchenUnit?) -> Unit,
) {
    val isChecked = checked == true
    val contentAlpha = if (isChecked) 0.55f else 1f
    val hasPromoteChip = promoteEnabled && row.promoteOptions.isNotEmpty()

    Row(
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
            )
            // Chip rows get no vertical padding: Surface(onClick) already
            // enforces a 48dp touch target, and padding on top would make
            // them taller than plain rows.
            .padding(
                horizontal = Spacing.m,
                vertical = if (hasPromoteChip) 0.dp else Spacing.m,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        // Cook-mode checkbox
        if (checked != null) {
            Checkbox(
                checked = checked,
                onCheckedChange = null, // handled by parent toggleable
                modifier = Modifier.size(24.dp),
            )
        }

        // Quantity — a shared fixed width (the widest in the list) keeps the
        // name columns of every row on the same vertical line.
        AnimatedContent(
            targetState = row.displayText,
            modifier = Modifier
                .width(qtyWidth)
                .alpha(contentAlpha)
                .animateContentSize(),
            label = "qty",
        ) { text ->
            QuantityText(
                text = text,
                style = MaterialTheme.typography.titleMedium.merge(
                    color = when {
                        isChecked -> MaterialTheme.colorScheme.onSurfaceVariant
                        row.status == ScaleStatus.UNPARSED ->
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (isChecked) TextDecoration.LineThrough else null,
                ),
            )
        }

        // Name + secondary captions
        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(contentAlpha),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = row.name,
                style = MaterialTheme.typography.bodyLarge,
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

        // Promote control (only when options exist)
        if (hasPromoteChip) {
            PromoteChip(
                row = row,
                onPromote = onPromote,
            )
        }
    }
}

@Composable
private fun PromoteChip(
    row: ScaledIngredient,
    onPromote: (KitchenUnit?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = MaterialTheme.shapes.small,
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
                    contentDescription = stringResource(R.string.cd_promote_unit),
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

@Composable
internal fun promoteUnitName(unit: KitchenUnit): String = stringResource(
    when (unit) {
        KitchenUnit.QUART -> R.string.promote_unit_quarts
        KitchenUnit.GALLON -> R.string.promote_unit_gallons
        KitchenUnit.L -> R.string.promote_unit_litres
        else -> R.string.promote_to
    },
)

@Composable
internal fun messageText(message: ScaleMessage): String = when (message) {
    is ScaleMessage.CantRead -> stringResource(R.string.msg_cant_read, message.text)
    is ScaleMessage.EnterAmountAboveZero -> stringResource(R.string.err_amount_above_zero)
}
