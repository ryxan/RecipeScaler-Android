package com.ryxan.recipescaler.feature.scale

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ryxan.recipescaler.R
import com.ryxan.recipescaler.core.designsystem.QuantityText
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.ScaleStatus
import com.ryxan.recipescaler.core.domain.model.ScaledIngredient
import com.ryxan.recipescaler.core.domain.model.formatYield
import com.ryxan.recipescaler.core.domain.parsing.QuantityParser
import com.ryxan.recipescaler.core.domain.scaling.CopyFormat
import kotlinx.coroutines.launch

@Composable
fun ScaleScreen(
    onNavigateUp: () -> Unit,
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
            onTab = viewModel::setTab,
            onTargetYield = viewModel::setTargetYield,
            onSelectIngredient = viewModel::selectIngredient,
            onIngredientAmount = viewModel::setIngredientAmount,
            onIngredientUnit = viewModel::setIngredientUnit,
            onFactorText = viewModel::setFactorText,
            onShowOriginal = viewModel::setShowOriginal,
            onCookMode = viewModel::toggleCookMode,
            onToggleChecked = viewModel::toggleChecked,
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
    val onTab: (ScaleTab) -> Unit = {},
    val onTargetYield: (String) -> Unit = {},
    val onSelectIngredient: (String) -> Unit = {},
    val onIngredientAmount: (String) -> Unit = {},
    val onIngredientUnit: (KitchenUnit) -> Unit = {},
    val onFactorText: (String) -> Unit = {},
    val onShowOriginal: (Boolean) -> Unit = {},
    val onCookMode: () -> Unit = {},
    val onToggleChecked: (String) -> Unit = {},
    val onCopy: (CopyFormat) -> Unit = {},
    val onShare: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleContent(
    state: ScaleUiState,
    callbacks: ScaleCallbacks,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var copyMenuExpanded by remember { mutableStateOf(false) }
    val view = LocalView.current
    DisposableEffect(state.cookMode) {
        view.keepScreenOn = state.cookMode
        onDispose { view.keepScreenOn = false }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
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
                    IconButton(onClick = callbacks.onCookMode) {
                        Icon(
                            Icons.Outlined.Checklist,
                            contentDescription = stringResource(
                                if (state.cookMode) R.string.action_exit_cook_mode
                                else R.string.action_cook_mode,
                            ),
                        )
                    }
                    Box {
                        IconButton(onClick = { copyMenuExpanded = true }) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = stringResource(R.string.action_copy),
                            )
                        }
                        DropdownMenu(
                            expanded = copyMenuExpanded,
                            onDismissRequest = { copyMenuExpanded = false },
                        ) {
                            listOf(
                                CopyFormat.PLAIN to R.string.copy_format_plain,
                                CopyFormat.MARKDOWN to R.string.copy_format_markdown,
                                CopyFormat.SHOPPING_LIST to R.string.copy_format_shopping,
                            ).forEach { (format, labelRes) ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(labelRes)) },
                                    onClick = {
                                        copyMenuExpanded = false
                                        callbacks.onCopy(format)
                                    },
                                )
                            }
                        }
                    }
                    IconButton(onClick = callbacks.onShare) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = stringResource(R.string.action_share),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
                item(key = "tabs") {
                    val tabs = listOf(
                        ScaleTab.SERVINGS to R.string.scale_tab_servings,
                        ScaleTab.INGREDIENT to R.string.scale_tab_ingredient,
                        ScaleTab.FACTOR to R.string.scale_tab_factor,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        tabs.forEachIndexed { index, (tab, labelRes) ->
                            SegmentedButton(
                                selected = state.inputs.tab == tab,
                                onClick = { callbacks.onTab(tab) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = tabs.size,
                                ),
                            ) {
                                Text(stringResource(labelRes))
                            }
                        }
                    }
                }

                item(key = "input") {
                    when (state.inputs.tab) {
                        ScaleTab.SERVINGS -> ServingsInput(state, callbacks)
                        ScaleTab.INGREDIENT -> IngredientInput(state, callbacks)
                        ScaleTab.FACTOR -> FactorInput(state, callbacks)
                    }
                }

                state.message?.let { message ->
                    item(key = "message") {
                        Text(
                            text = messageText(message),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
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

            if (state.showScaleWarning) {
                item(key = "warning") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                Icons.Outlined.WarningAmber,
                                contentDescription = null,
                            )
                            Text(
                                text = stringResource(
                                    if (state.factor > 4.0) R.string.scale_warning_big
                                    else R.string.scale_warning_small,
                                    formatYield(state.factor),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            if (!state.cookMode) {
                item(key = "show_original") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.scale_show_original),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = state.showOriginal,
                            onCheckedChange = callbacks.onShowOriginal,
                        )
                    }
                }
            }

            items(state.rows, key = { it.id }) { row ->
                ScaledRow(
                    row = row,
                    showOriginal = state.showOriginal && !state.cookMode,
                    checked = if (state.cookMode) row.id in state.checkedIds else null,
                    onToggle = { callbacks.onToggleChecked(row.id) },
                )
            }
            }
        }
    }
}

@Composable
private fun ServingsInput(state: ScaleUiState, callbacks: ScaleCallbacks) {
    Column {
        OutlinedTextField(
            value = state.inputs.targetYieldText,
            onValueChange = callbacks.onTargetYield,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            label = { Text(stringResource(R.string.scale_label_target)) },
            suffix = { Text(state.yieldLabel) },
            singleLine = true,
        )
        if (state.yieldAmount > 0) {
            Text(
                stringResource(
                    R.string.scale_original_yield,
                    formatYield(state.yieldAmount),
                    state.yieldLabel,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientInput(state: ScaleUiState, callbacks: ScaleCallbacks) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        var ingredientExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = ingredientExpanded,
            onExpandedChange = { ingredientExpanded = it },
        ) {
            val selected = state.ingredientChoices.find { it.id == state.inputs.ingredientId }
            OutlinedTextField(
                value = selected?.label.orEmpty(),
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                readOnly = true,
                label = { Text(stringResource(R.string.scale_label_ingredient)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(ingredientExpanded) },
            )
            ExposedDropdownMenu(
                expanded = ingredientExpanded,
                onDismissRequest = { ingredientExpanded = false },
            ) {
                state.ingredientChoices.forEach { choice ->
                    DropdownMenuItem(
                        text = { Text(choice.label) },
                        onClick = {
                            ingredientExpanded = false
                            callbacks.onSelectIngredient(choice.id)
                        },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.inputs.ingredientAmountText,
                onValueChange = callbacks.onIngredientAmount,
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.scale_label_amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
            var unitExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = unitExpanded,
                onExpandedChange = { unitExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = (state.inputs.ingredientUnit
                        ?: state.unitChoices.firstOrNull())?.label.orEmpty(),
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    readOnly = true,
                    label = { Text(stringResource(R.string.scale_label_unit)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitExpanded) },
                )
                ExposedDropdownMenu(
                    expanded = unitExpanded,
                    onDismissRequest = { unitExpanded = false },
                ) {
                    state.unitChoices.forEach { unit ->
                        DropdownMenuItem(
                            text = { Text(unit.label) },
                            onClick = {
                                unitExpanded = false
                                callbacks.onIngredientUnit(unit)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FactorInput(state: ScaleUiState, callbacks: ScaleCallbacks) {
    val presets = listOf("0.5" to "½×", "1" to "1×", "2" to "2×", "3" to "3×")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.selectableGroup(),
        ) {
            val current = QuantityParser.parse(state.inputs.factorText)
            presets.forEach { (value, label) ->
                FilterChip(
                    selected = current?.let { it == value.toDouble() } == true,
                    onClick = { callbacks.onFactorText(value) },
                    label = { Text(label) },
                )
            }
        }
        OutlinedTextField(
            value = state.inputs.factorText,
            onValueChange = callbacks.onFactorText,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.scale_label_custom_factor)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
    }
}

@Composable
private fun SummaryLine(state: ScaleUiState) {
    val factorText = stringResource(R.string.scale_factor_prefix, formatYield(state.factor))
    val derived = state.derivedYieldAmount?.let {
        stringResource(R.string.scale_makes, formatYield(it), state.yieldLabel)
    }
    Text(
        text = listOfNotNull(factorText, derived).joinToString(" · "),
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun ScaledRow(
    row: ScaledIngredient,
    showOriginal: Boolean,
    checked: Boolean?,
    onToggle: () -> Unit,
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
    }
}

@Composable
private fun messageText(message: ScaleMessage): String = when (message) {
    is ScaleMessage.CantRead -> stringResource(R.string.msg_cant_read, message.text)
    ScaleMessage.EnterAmountAboveZero -> stringResource(R.string.err_amount_above_zero)
    ScaleMessage.IngredientNoQuantity -> stringResource(R.string.err_ingredient_no_qty)
    is ScaleMessage.IncompatibleUnits ->
        stringResource(R.string.err_incompatible_units, message.from, message.to)
    ScaleMessage.SetYieldInEditor -> stringResource(R.string.scale_set_yield)
}
