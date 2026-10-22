package com.ryxan.recipescaler.feature.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ryxan.recipescaler.R
import com.ryxan.recipescaler.core.designsystem.RescaleSwipeToDelete
import com.ryxan.recipescaler.core.designsystem.Spacing
import com.ryxan.recipescaler.core.designsystem.V2Card
import com.ryxan.recipescaler.core.designsystem.V2SectionHeader
import com.ryxan.recipescaler.core.designsystem.V2TopAppBar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** LazyColumn sections above the ingredient rows, in display order (v2). */
private enum class V2Header { EXTRACTION, TITLE_HEADER, TITLE, YIELD, INGREDIENTS_HEADER, EMPTY_STATE }

private fun v2Headers(rows: List<EditorRow>): List<V2Header> = buildList {
    val empty = rows.all { it.rawQuantity.isBlank() && it.name.isBlank() }
    // The empty-state card carries its own scan/paste CTAs — showing the
    // extraction card too would duplicate them.
    if (!empty) add(V2Header.EXTRACTION)
    add(V2Header.TITLE_HEADER)
    add(V2Header.TITLE)
    add(V2Header.YIELD)
    add(V2Header.INGREDIENTS_HEADER)
    if (empty) add(V2Header.EMPTY_STATE)
}

/**
 * Culinary Material (v2) editor — same state, callbacks and editing machinery
 * as [EditorContent] (autosave, reorder, unit inference, scan/paste/AI import),
 * restyled after the Stitch mock: a "Smart recipe extraction" card, per-row
 * remove affordance, and a bottom action bar with the Scale action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorV2Content(
    state: EditorUiState,
    callbacks: EditorCallbacks,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var focused by remember { mutableStateOf<FocusedField?>(null) }
    var pendingFocus by remember { mutableStateOf<FocusedField?>(null) }
    var unitPickerRowKey by remember { mutableStateOf<Long?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var autoFocusHandled by rememberSaveable { mutableStateOf(false) }
    val qtyValues = remember { mutableStateMapOf<Long, MutableState<TextFieldValue>>() }
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromKey = from.key
        val toKey = to.key
        if (fromKey is Long && toKey is Long) callbacks.onMoveRow(fromKey, toKey)
    }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val titleCd = stringResource(R.string.cd_recipe_title)
    val undoLabel = stringResource(R.string.action_undo)
    val fabNeedIngredient = stringResource(R.string.fab_need_ingredient)
    val ingredientRemovedMsg = stringResource(R.string.snackbar_ingredient_removed)
    val ingredientsClearedMsg = stringResource(R.string.snackbar_ingredients_cleared)
    val yieldAmountCd = stringResource(R.string.cd_yield_amount)

    LaunchedEffect(state.autoFocusKey) {
        val key = state.autoFocusKey
        if (!autoFocusHandled && key != null) {
            pendingFocus = FocusedField(key, RowField.QTY)
            autoFocusHandled = true
        }
    }

    // A newly added row can land below the fold, where it isn't composed yet —
    // scroll it into view so its pendingFocus requester can run.
    val latestState by rememberUpdatedState(state)
    LaunchedEffect(pendingFocus) {
        val target = pendingFocus ?: return@LaunchedEffect
        val index = snapshotFlow {
            val s = latestState
            val idx = s.rows.indexOfFirst { it.key == target.rowKey }
            // Header sections precede the rows; their count derives from the
            // same list the LazyColumn emits, so it can't drift out of sync.
            if (idx < 0) -1 else idx + v2Headers(s.rows).size
        }.first { it >= 0 }
        if (lazyListState.layoutInfo.visibleItemsInfo.none { it.key == target.rowKey }) {
            lazyListState.animateScrollToItem(index)
        }
    }

    LaunchedEffect(state.undoToken) {
        val undo = state.pendingUndo ?: return@LaunchedEffect
        val message = when (undo) {
            is EditorUndo.RowRemoved -> ingredientRemovedMsg
            is EditorUndo.RowsCleared -> ingredientsClearedMsg
        }
        when (
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Long,
            )
        ) {
            SnackbarResult.ActionPerformed -> callbacks.onUndoEdit()
            SnackbarResult.Dismissed -> callbacks.onDismissUndo()
        }
    }

    fun goNext() {
        val f = focused ?: return
        when (f.field) {
            RowField.QTY -> {
                unitPickerRowKey = f.rowKey
                return
            }
            RowField.NAME -> callbacks.onInferName(f.rowKey)
        }
        val idx = state.rows.indexOfFirst { it.key == f.rowKey }
        val next = state.rows.getOrNull(idx + 1)
        if (next != null) {
            pendingFocus = FocusedField(next.key, RowField.QTY)
        } else if (idx >= 0 && state.rows.size < MAX_INGREDIENTS) {
            val key = callbacks.onAddRow(null)
            if (key >= 0) pendingFocus = FocusedField(key, RowField.QTY)
        }
    }

    fun insertFraction(ch: String) {
        val f = focused ?: return
        val field = qtyValues[f.rowKey] ?: return
        val cur = field.value
        val sel = cur.selection
        val newText = cur.text.replaceRange(sel.min, sel.max, ch)
        val pos = sel.min + ch.length
        field.value = TextFieldValue(newText, TextRange(pos))
        callbacks.onQtyChange(f.rowKey, newText)
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            V2TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        stringResource(
                            if (state.recipeId == 0L) R.string.title_new_recipe
                            else R.string.title_edit_recipe,
                        ),
                        style = MaterialTheme.typography.titleLarge,
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
                    IconButton(onClick = callbacks.onSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.title_settings),
                        )
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.action_more),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_paste_text)) },
                            onClick = {
                                menuExpanded = false
                                callbacks.onPasteClick()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_clear_ingredients)) },
                            onClick = {
                                menuExpanded = false
                                callbacks.onClearClick()
                            },
                        )
                        if (state.recipeId != 0L) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete_recipe)) },
                                onClick = {
                                    menuExpanded = false
                                    callbacks.onDelete()
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = Spacing.l, vertical = Spacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                ) {
                    Text(
                        text = if (state.rows.isEmpty()) {
                            fabNeedIngredient
                        } else {
                            pluralStringResource(
                                R.plurals.ingredient_count,
                                state.rows.size,
                                state.rows.size,
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    val canScale = state.canScale
                    if (canScale) {
                        Button(onClick = callbacks.onScale, shape = CircleShape) {
                            Icon(
                                Icons.Filled.Scale,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(Spacing.s))
                            Text(stringResource(R.string.action_scale))
                        }
                    } else {
                        Button(
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(fabNeedIngredient)
                                }
                            },
                            modifier = Modifier.semantics { disabled() },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface
                                    .copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                                    .copy(alpha = 0.38f),
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                        ) {
                            Icon(
                                Icons.Filled.Scale,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(Spacing.s))
                            Text(stringResource(R.string.action_scale))
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            state.importBusy?.let { busy ->
                Column(Modifier.padding(horizontal = Spacing.l)) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(
                        text = stringResource(
                            if (busy == ImportBusy.AI) R.string.scan_reading_ai
                            else R.string.scan_reading,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = Spacing.l,
                    end = Spacing.l,
                    top = Spacing.m,
                    bottom = Spacing.l,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                items(v2Headers(state.rows), key = { it.name }) { section ->
                    when (section) {
                        V2Header.EXTRACTION -> ExtractionCard(
                            onScan = callbacks.onScanClick,
                            onPaste = callbacks.onPasteClick,
                        )
                        V2Header.TITLE_HEADER -> V2SectionHeader(
                            stringResource(R.string.header_name),
                        )
                        V2Header.TITLE -> OutlinedTextField(
                            value = state.title,
                            onValueChange = callbacks.onTitleChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = titleCd },
                            textStyle = MaterialTheme.typography.titleLarge,
                            placeholder = {
                                Text(
                                    stringResource(R.string.editor_title_placeholder),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            },
                            shape = MaterialTheme.shapes.medium,
                            colors = editorFieldColors(),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next,
                            ),
                            singleLine = true,
                        )
                        V2Header.YIELD -> {
                            val yieldError = state.yieldError
                            V2Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier.padding(
                                        horizontal = Spacing.l,
                                        vertical = Spacing.m,
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            stringResource(R.string.editor_makes),
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        OutlinedTextField(
                                            value = state.yieldText,
                                            onValueChange = callbacks.onYieldTextChange,
                                            modifier = Modifier
                                                .width(104.dp)
                                                .semantics {
                                                    contentDescription = yieldAmountCd
                                                },
                                            placeholder = { Text("4") },
                                            shape = MaterialTheme.shapes.small,
                                            colors = editorFieldColors(),
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Decimal,
                                            ),
                                            isError = yieldError,
                                            singleLine = true,
                                        )
                                    }
                                    if (yieldError) {
                                        Text(
                                            stringResource(
                                                R.string.msg_cant_read_example,
                                                state.yieldText,
                                            ),
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(top = Spacing.xs),
                                        )
                                    }
                                }
                            }
                        }
                        V2Header.INGREDIENTS_HEADER -> V2SectionHeader(
                            text = stringResource(R.string.header_ingredients),
                            trailing = {
                                TextButton(
                                    onClick = {
                                        val key = callbacks.onAddRow(null)
                                        if (key >= 0) {
                                            pendingFocus = FocusedField(key, RowField.QTY)
                                        }
                                    },
                                    enabled = state.rows.size < MAX_INGREDIENTS,
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(Spacing.xs))
                                    Text(stringResource(R.string.v2_add_item))
                                }
                            },
                        )
                        V2Header.EMPTY_STATE -> EmptyStateCard(
                            onScan = callbacks.onScanClick,
                            onPaste = callbacks.onPasteClick,
                            onType = {
                                val key = state.rows.firstOrNull()?.key
                                    ?: callbacks.onAddRow(null)
                                if (key >= 0) pendingFocus = FocusedField(key, RowField.QTY)
                            },
                        )
                    }
                }
                items(state.rows, key = { it.key }) { row ->
                    ReorderableItem(reorderableState, key = row.key) { isDragging ->
                        val idx = state.rows.indexOfFirst { it.key == row.key }
                        val prev = state.rows.getOrNull(idx - 1)
                        val next = state.rows.getOrNull(idx + 1)
                        val moveUp = stringResource(R.string.action_move_up)
                        val moveDown = stringResource(R.string.action_move_down)
                        val removeLabel = stringResource(R.string.action_remove_ingredient)
                        RescaleSwipeToDelete(onDelete = { callbacks.onRemoveRow(row.key) }) {
                            IngredientRowV2(
                                row = row,
                                position = idx + 1,
                                isDragging = isDragging,
                                dragHandle = Modifier.draggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                ),
                                modifier = Modifier
                                    .longPressDraggableHandle(
                                        onDragStarted = {
                                            haptic.performHapticFeedback(
                                                HapticFeedbackType.LongPress,
                                            )
                                        },
                                    )
                                    .semantics {
                                        customActions = buildList {
                                            if (prev != null) {
                                                add(
                                                    CustomAccessibilityAction(moveUp) {
                                                        callbacks.onMoveRow(row.key, prev.key)
                                                        true
                                                    },
                                                )
                                            }
                                            if (next != null) {
                                                add(
                                                    CustomAccessibilityAction(moveDown) {
                                                        callbacks.onMoveRow(row.key, next.key)
                                                        true
                                                    },
                                                )
                                            }
                                            add(
                                                CustomAccessibilityAction(removeLabel) {
                                                    callbacks.onRemoveRow(row.key)
                                                    true
                                                },
                                            )
                                        }
                                    },
                                qtyValue = qtyValues.getOrPut(row.key) {
                                    mutableStateOf(
                                        TextFieldValue(
                                            row.rawQuantity,
                                            TextRange(row.rawQuantity.length),
                                        ),
                                    )
                                },
                                requestFocus = pendingFocus
                                    ?.takeIf { it.rowKey == row.key }
                                    ?.field,
                                onFocusedHandled = { pendingFocus = null },
                                onFocusChanged = { hasFocus, field ->
                                    if (hasFocus) {
                                        focused = FocusedField(row.key, field)
                                    } else {
                                        if (field == RowField.NAME) callbacks.onInferName(row.key)
                                        if (focused?.rowKey == row.key && focused?.field == field) {
                                            focused = null
                                        }
                                    }
                                },
                                onQtyChange = { callbacks.onQtyChange(row.key, it) },
                                onUnitClick = { unitPickerRowKey = row.key },
                                onNameChange = { callbacks.onNameChange(row.key, it) },
                                onRemove = { callbacks.onRemoveRow(row.key) },
                                onNext = ::goNext,
                            )
                        }
                    }
                }
            }

            AccessoryBar(
                visible = focused != null,
                qtyFocused = focused?.field == RowField.QTY,
                onFraction = ::insertFraction,
                onUnit = { unit ->
                    focused?.let { callbacks.onUnitChange(it.rowKey, unit) }
                },
                onNext = ::goNext,
            )
        }
    }

    unitPickerRowKey?.let { key ->
        val row = state.rows.firstOrNull { it.key == key }
        if (row != null) {
            UnitPickerSheet(
                selected = row.unit,
                onSelect = {
                    callbacks.onUnitChange(key, it)
                    unitPickerRowKey = null
                    pendingFocus = FocusedField(key, RowField.NAME)
                },
                onDismiss = {
                    unitPickerRowKey = null
                    pendingFocus = FocusedField(key, RowField.NAME)
                },
            )
        }
    }

    EditorDialogs(state = state, callbacks = callbacks)
}

/** "Smart recipe extraction" card — scan and paste entry points. */
@Composable
private fun ExtractionCard(
    onScan: () -> Unit,
    onPaste: () -> Unit,
) {
    V2Card(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(Spacing.l)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryFixed,
                    contentColor = MaterialTheme.colorScheme.onPrimaryFixedVariant,
                ) {
                    Icon(
                        Icons.Outlined.DocumentScanner,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(Spacing.s)
                            .size(24.dp),
                    )
                }
                Column {
                    Text(
                        stringResource(R.string.v2_import_card_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.v2_import_card_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.m),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                FilledTonalButton(
                    onClick = onScan,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Outlined.DocumentScanner,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(Spacing.s))
                    Text(stringResource(R.string.action_scan_recipe))
                }
                OutlinedButton(
                    onClick = onPaste,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Outlined.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(Spacing.s))
                    Text(stringResource(R.string.action_paste))
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    onScan: () -> Unit,
    onPaste: () -> Unit,
    onType: () -> Unit,
) {
    V2Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryFixed,
                contentColor = MaterialTheme.colorScheme.onPrimaryFixedVariant,
            ) {
                Icon(
                    Icons.Outlined.Restaurant,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(Spacing.m)
                        .size(28.dp),
                )
            }
            Text(
                stringResource(R.string.editor_empty_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = Spacing.m),
            )
            Text(
                stringResource(R.string.editor_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.l),
            )
            Button(
                onClick = onScan,
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
            ) {
                Icon(
                    Icons.Outlined.DocumentScanner,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(Spacing.s))
                Text(stringResource(R.string.action_scan))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.s),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                FilledTonalButton(
                    onClick = onPaste,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Outlined.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(Spacing.s))
                    Text(stringResource(R.string.action_paste))
                }
                FilledTonalButton(
                    onClick = onType,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Outlined.Keyboard,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(Spacing.s))
                    Text(stringResource(R.string.action_type))
                }
            }
        }
    }
}

/**
 * v2 ingredient row: drag handle, qty / unit / name fields (shared with v1)
 * and the mock's explicit remove "×" alongside swipe-to-delete and the same
 * accessibility actions.
 */
@Composable
private fun IngredientRowV2(
    row: EditorRow,
    position: Int,
    isDragging: Boolean,
    dragHandle: Modifier,
    qtyValue: MutableState<TextFieldValue>,
    modifier: Modifier = Modifier,
    requestFocus: RowField?,
    onFocusedHandled: () -> Unit,
    onFocusChanged: (hasFocus: Boolean, field: RowField) -> Unit,
    onQtyChange: (String) -> Unit,
    onUnitClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onRemove: () -> Unit,
    onNext: () -> Unit,
) {
    val qtyFocusRequester = remember { FocusRequester() }
    val nameFocusRequester = remember { FocusRequester() }
    val qtyCd = stringResource(R.string.cd_quantity_field, position)
    val nameCd = stringResource(R.string.cd_ingredient_name_field, position)
    if (requestFocus != null) {
        LaunchedEffect(Unit) {
            when (requestFocus) {
                RowField.NAME -> nameFocusRequester.requestFocus()
                else -> qtyFocusRequester.requestFocus()
            }
            onFocusedHandled()
        }
    }
    val fontScale = LocalDensity.current.fontScale
    V2Card(
        modifier = modifier.shadow(
            elevation = if (isDragging) 6.dp else 0.dp,
            shape = MaterialTheme.shapes.extraLarge,
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.xs, end = Spacing.xs, top = Spacing.xs, bottom = Spacing.xs),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = dragHandle
                    .width(28.dp)
                    .height(56.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.DragIndicator,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
            BoxWithConstraints(Modifier.weight(1f)) {
                val stacked = fontScale > 1.3f || maxWidth < 236.dp
                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            QtyField(
                                value = qtyValue,
                                isError = row.qtyError,
                                contentDesc = qtyCd,
                                modifier = Modifier.width(88.dp),
                                focusRequester = qtyFocusRequester,
                                onNext = onUnitClick,
                                onFocusChanged = onFocusChanged,
                                onQtyChange = onQtyChange,
                            )
                            UnitPickerButton(unit = row.unit, onClick = onUnitClick)
                        }
                        NameField(
                            row = row,
                            contentDesc = nameCd,
                            modifier = Modifier.fillMaxWidth(),
                            focusRequester = nameFocusRequester,
                            onFocusChanged = onFocusChanged,
                            onNameChange = onNameChange,
                            onNext = onNext,
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                        verticalAlignment = Alignment.Top,
                    ) {
                        QtyField(
                            value = qtyValue,
                            isError = row.qtyError,
                            contentDesc = qtyCd,
                            modifier = Modifier.width(80.dp),
                            focusRequester = qtyFocusRequester,
                            onNext = onUnitClick,
                            onFocusChanged = onFocusChanged,
                            onQtyChange = onQtyChange,
                        )
                        UnitPickerButton(unit = row.unit, onClick = onUnitClick)
                        NameField(
                            row = row,
                            contentDesc = nameCd,
                            modifier = Modifier.weight(1f),
                            focusRequester = nameFocusRequester,
                            onFocusChanged = onFocusChanged,
                            onNameChange = onNameChange,
                            onNext = onNext,
                        )
                    }
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_remove_ingredient),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
