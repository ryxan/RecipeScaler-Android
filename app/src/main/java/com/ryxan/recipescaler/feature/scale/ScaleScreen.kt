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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateContentSize
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
import com.ryxan.recipescaler.core.designsystem.GradientTopAppBar
import com.ryxan.recipescaler.core.designsystem.QuantityText
import com.ryxan.recipescaler.core.designsystem.RescaleCard
import com.ryxan.recipescaler.core.designsystem.Spacing
import com.ryxan.recipescaler.core.domain.model.ScaleStatus
import com.ryxan.recipescaler.core.domain.model.ScaledIngredient
import com.ryxan.recipescaler.core.domain.model.formatYield
import com.ryxan.recipescaler.core.domain.scaling.CopyFormat
import kotlinx.coroutines.launch
import kotlin.math.abs

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
            onServes = viewModel::setServes,
            onScaleTo = viewModel::setScaleTo,
            onMultiplier = viewModel::applyMultiplier,
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
    val onServes: (String) -> Unit = {},
    val onScaleTo: (String) -> Unit = {},
    val onMultiplier: (Int) -> Unit = {},
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
                item(key = "inputs") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.inputs.servesText,
                            onValueChange = callbacks.onServes,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                            ),
                            label = { Text(stringResource(R.string.scale_label_serves)) },
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
                            singleLine = true,
                        )
                    }
                }

                item(key = "multipliers") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.selectableGroup(),
                    ) {
                        items(10) { index ->
                            val n = index + 1
                            FilterChip(
                                selected = state.message == null &&
                                    abs(state.factor - n) < 1e-9,
                                onClick = { callbacks.onMultiplier(n) },
                                label = { Text("${n}×") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor =
                                        MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                        }
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

            item(key = "warning") {
                AnimatedVisibility(
                    visible = state.showScaleWarning,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
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
    }
}

@Composable
private fun messageText(message: ScaleMessage): String = when (message) {
    is ScaleMessage.CantRead -> stringResource(R.string.msg_cant_read, message.text)
    ScaleMessage.EnterAmountAboveZero -> stringResource(R.string.err_amount_above_zero)
}
