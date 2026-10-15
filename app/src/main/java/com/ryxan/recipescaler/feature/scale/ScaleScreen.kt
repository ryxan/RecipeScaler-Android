package com.ryxan.recipescaler.feature.scale

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.WarningAmber
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ryxan.recipescaler.R
import com.ryxan.recipescaler.core.designsystem.GradientTopAppBar
import com.ryxan.recipescaler.core.designsystem.QuantityText
import com.ryxan.recipescaler.core.designsystem.RescaleCard
import com.ryxan.recipescaler.core.designsystem.Spacing
import com.ryxan.recipescaler.core.domain.formatting.MeasurementSystem
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.ScaleStatus
import com.ryxan.recipescaler.core.domain.model.ScaledIngredient
import com.ryxan.recipescaler.core.domain.model.formatYield
import com.ryxan.recipescaler.core.domain.parsing.QuantityParser
import com.ryxan.recipescaler.core.domain.scaling.CopyFormat
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Target-servings chips for the "Scale to" shortcuts row. */
private val servingChips = listOf(1, 2, 3, 4, 5, 6, 8, 10, 12, 16, 20, 24)

@Composable
fun ScaleScreen(
    onNavigateUp: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: ScaleViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val copied = stringResource(R.string.msg_copied)

    ScaleContent(
        state = state,
        snackbarHostState = snackbarHostState,
        callbacks = ScaleCallbacks(
            onNavigateUp = onNavigateUp,
            onEdit = onEdit,
            onServes = viewModel::setServes,
            onScaleTo = viewModel::setScaleTo,
            onServings = viewModel::applyServings,
            onStep = viewModel::stepServings,
            onShowOriginal = viewModel::setShowOriginal,
            onCookMode = viewModel::toggleCookMode,
            onToggleChecked = viewModel::toggleChecked,
            onPromote = viewModel::promote,
            onMeasurementSystem = viewModel::setMeasurementSystem,
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
        ),
    )
}

data class ScaleCallbacks(
    val onNavigateUp: () -> Unit = {},
    val onEdit: (Long) -> Unit = {},
    val onServes: (String) -> Unit = {},
    val onScaleTo: (String) -> Unit = {},
    val onServings: (Int) -> Unit = {},
    val onStep: (Int) -> Unit = {},
    val onShowOriginal: (Boolean) -> Unit = {},
    val onCookMode: () -> Unit = {},
    val onToggleChecked: (String) -> Unit = {},
    val onPromote: (String, KitchenUnit?) -> Unit = { _, _ -> },
    val onCopy: (CopyFormat) -> Unit = {},
    val onShare: () -> Unit = {},
    val onMeasurementSystem: (MeasurementSystem) -> Unit = {},
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
                item(key = "inputs") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val message = state.message
                        OutlinedTextField(
                            value = state.inputs.servesText,
                            onValueChange = callbacks.onServes,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                            ),
                            label = { Text(stringResource(R.string.scale_label_serves)) },
                            isError = message?.field == ScaleField.SERVES,
                            supportingText = {
                                if (message?.field == ScaleField.SERVES) {
                                    Text(messageText(message))
                                }
                            },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = state.inputs.scaleToText,
                            onValueChange = callbacks.onScaleTo,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                            ),
                            label = { Text(stringResource(R.string.scale_label_scale_to)) },
                            isError = message?.field == ScaleField.SCALE_TO,
                            supportingText = {
                                if (message?.field == ScaleField.SCALE_TO) {
                                    Text(messageText(message))
                                }
                            },
                            singleLine = true,
                        )
                    }
                }

                item(key = "controls") {
                    val targetServings = state.derivedYieldAmount
                        ?: QuantityParser.parse(state.inputs.scaleToText)
                    val targetText = targetServings?.let { formatYield(it) } ?: "–"
                    val currentTarget = stringResource(
                        R.string.cd_target_servings,
                        targetText,
                        state.yieldLabel,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .selectableGroup(),
                        ) {
                            items(servingChips, key = { it }) { n ->
                                FilterChip(
                                    selected = state.message == null &&
                                        QuantityParser.parse(state.inputs.scaleToText)
                                            ?.let { abs(it - n.toDouble()) < 1e-9 } == true,
                                    onClick = { callbacks.onServings(n) },
                                    label = { Text("$n") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor =
                                            MaterialTheme.colorScheme.primaryContainer,
                                    ),
                                )
                            }
                        }
                        IconButton(
                            onClick = { callbacks.onStep(-1) },
                            enabled = targetServings == null || targetServings >
                                ScaleViewModel.MIN_SERVINGS + 1e-9,
                        ) {
                            Icon(
                                Icons.Filled.Remove,
                                contentDescription = stringResource(
                                    R.string.action_decrease_servings,
                                ),
                            )
                        }
                        Text(
                            text = targetText,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .widthIn(min = 40.dp)
                                .clearAndSetSemantics {
                                    contentDescription = currentTarget
                                },
                        )
                        IconButton(
                            onClick = { callbacks.onStep(1) },
                            enabled = targetServings == null || targetServings <
                                ScaleViewModel.MAX_SERVINGS - 1e-9,
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = stringResource(
                                    R.string.action_increase_servings,
                                ),
                            )
                        }
                    }
                }

            } else {
                item(key = "cook_hint") {
                    Text(
                        stringResource(R.string.cook_mode_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item(key = "summary") {
                SummaryLine(state)
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
                    Column(
                        modifier = Modifier.padding(Spacing.m),
                        verticalArrangement = Arrangement.spacedBy(Spacing.m),
                    ) {
                        state.rows.forEach { row ->
                            ScaledRow(
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
    showOriginal: Boolean,
    checked: Boolean?,
    onToggle: () -> Unit,
    promoteEnabled: Boolean,
    onPromote: (KitchenUnit?) -> Unit,
) {
    val struck = checked == true
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
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedContent(
            targetState = row.displayText,
            modifier = Modifier.animateContentSize(),
            label = "qty",
        ) { text ->
            QuantityText(
                text = text,
                style = MaterialTheme.typography.titleMedium.merge(
                    color = if (struck) MaterialTheme.colorScheme.onSurfaceVariant
                    else Color.Unspecified,
                    textDecoration = if (struck) TextDecoration.LineThrough else null,
                ),
            )
        }
        Column {
            Text(
                row.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (struck) MaterialTheme.colorScheme.onSurfaceVariant
                else Color.Unspecified,
                textDecoration = if (struck) TextDecoration.LineThrough else null,
            )
            val captions = buildList {
                when (row.status) {
                    ScaleStatus.RANGE_LOW_END -> {
                        if (showOriginal) {
                            add(stringResource(R.string.scale_was, row.originalDisplayText))
                        }
                        add(stringResource(R.string.scale_from_range))
                    }
                    ScaleStatus.UNPARSED -> add(stringResource(R.string.scale_not_scaled))
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
                    caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (promoteEnabled && row.promoteOptions.isNotEmpty()) {
            Spacer(Modifier.weight(1f))
            PromoteMenu(row = row, onPromote = onPromote)
        }
    }
}

@Composable
private fun PromoteMenu(
    row: ScaledIngredient,
    onPromote: (KitchenUnit?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Outlined.SwapVert,
                contentDescription = stringResource(R.string.cd_promote_unit),
                tint = if (row.promotedUnit != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
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
private fun promoteUnitName(unit: KitchenUnit): String = stringResource(
    when (unit) {
        KitchenUnit.QUART -> R.string.promote_unit_quarts
        KitchenUnit.GALLON -> R.string.promote_unit_gallons
        KitchenUnit.ML -> R.string.promote_unit_ml
        KitchenUnit.L -> R.string.promote_unit_litres
        else -> R.string.promote_to
    },
)

@Composable
private fun messageText(message: ScaleMessage): String = when (message) {
    is ScaleMessage.CantRead -> stringResource(R.string.msg_cant_read, message.text)
    is ScaleMessage.EnterAmountAboveZero -> stringResource(R.string.err_amount_above_zero)
}
