package com.ryxan.recipescaler.feature.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ryxan.recipescaler.R
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.UnitFamily
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(
    onNavigateUp: () -> Unit,
    onScale: (Long) -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val nothingImported = stringResource(R.string.msg_nothing_imported)
    val capReached = stringResource(R.string.ingredient_cap_reached)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EditorEvent.NavigateUp -> onNavigateUp()
                EditorEvent.NothingToImport ->
                    snackbarHostState.showSnackbar(nothingImported)
                EditorEvent.IngredientCapReached -> snackbarHostState.showSnackbar(capReached)
            }
        }
    }

    EditorContent(
        state = state,
        snackbarHostState = snackbarHostState,
        callbacks = EditorCallbacks(
            onNavigateUp = onNavigateUp,
            onScale = { scope.launch { viewModel.prepareToScale()?.let(onScale) } },
            onTitleChange = viewModel::onTitleChange,
            onYieldTextChange = viewModel::onYieldTextChange,
            onYieldLabelChange = viewModel::onYieldLabelChange,
            onQtyChange = viewModel::onRowQtyChange,
            onUnitChange = viewModel::onRowUnitChange,
            onNameChange = viewModel::onRowNameChange,
            onAddRow = viewModel::addRow,
            onRemoveRow = viewModel::removeRow,
            onPasteClick = viewModel::openPasteDialog,
            onImportText = viewModel::importText,
            onImportReplace = viewModel::confirmImportReplace,
            onImportAppend = viewModel::confirmImportAppend,
            onClearClick = viewModel::requestClear,
            onClearConfirm = viewModel::confirmClear,
            onDeleteClick = viewModel::requestDelete,
            onDeleteConfirm = viewModel::confirmDelete,
            onDismissDialog = viewModel::dismissDialog,
        ),
    )
}

/** Bundled callbacks so [EditorContent] stays easy to drive from UI tests. */
data class EditorCallbacks(
    val onNavigateUp: () -> Unit = {},
    val onScale: () -> Unit = {},
    val onTitleChange: (String) -> Unit = {},
    val onYieldTextChange: (String) -> Unit = {},
    val onYieldLabelChange: (String) -> Unit = {},
    val onQtyChange: (Long, String) -> Unit = { _, _ -> },
    val onUnitChange: (Long, KitchenUnit) -> Unit = { _, _ -> },
    val onNameChange: (Long, String) -> Unit = { _, _ -> },
    val onAddRow: (Long?) -> Long = { -1L },
    val onRemoveRow: (Long) -> Unit = {},
    val onPasteClick: () -> Unit = {},
    val onImportText: (String) -> Unit = {},
    val onImportReplace: () -> Unit = {},
    val onImportAppend: () -> Unit = {},
    val onClearClick: () -> Unit = {},
    val onClearConfirm: () -> Unit = {},
    val onDeleteClick: () -> Unit = {},
    val onDeleteConfirm: () -> Unit = {},
    val onDismissDialog: () -> Unit = {},
)

private enum class RowField { QTY, NAME }
private data class FocusedField(val rowKey: Long, val field: RowField)

private val FRACTION_INSERTS = listOf("½", "⅓", "¼", "¾", "⅔", "⅛", "/")
private val BAR_UNITS = listOf(
    KitchenUnit.CUP, KitchenUnit.TBSP, KitchenUnit.TSP, KitchenUnit.G, KitchenUnit.OZ,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorContent(
    state: EditorUiState,
    callbacks: EditorCallbacks,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var focused by remember { mutableStateOf<FocusedField?>(null) }
    var pendingFocusKey by remember { mutableStateOf<Long?>(null) }
    var unitPickerRowKey by remember { mutableStateOf<Long?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    val qtyValues = remember { mutableStateMapOf<Long, androidx.compose.runtime.MutableState<TextFieldValue>>() }

    fun goNext() {
        val f = focused ?: return
        val idx = state.rows.indexOfFirst { it.key == f.rowKey }
        val next = state.rows.getOrNull(idx + 1)
        if (next != null) {
            pendingFocusKey = next.key
        } else if (idx >= 0 && state.rows.size < MAX_INGREDIENTS) {
            val key = callbacks.onAddRow(null)
            if (key >= 0) pendingFocusKey = key
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.recipeId == 0L) R.string.title_new_recipe
                            else R.string.title_edit_recipe,
                        ),
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
                    Box {
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
                                        callbacks.onDeleteClick()
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            val canScale = state.canScale
            ExtendedFloatingActionButton(
                onClick = { if (canScale) callbacks.onScale() },
                modifier = Modifier
                    .semantics { if (!canScale) disabled() }
                    .alpha(if (canScale) 1f else 0.5f),
                icon = { Icon(Icons.Filled.Scale, contentDescription = null) },
                text = { Text(stringResource(R.string.action_scale)) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "title") {
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = callbacks.onTitleChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.hint_recipe_title)) },
                        singleLine = true,
                    )
                }
                item(key = "yield") {
                    val yieldError = state.yieldError
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        OutlinedTextField(
                            value = state.yieldText,
                            onValueChange = callbacks.onYieldTextChange,
                            modifier = Modifier.width(120.dp),
                            placeholder = { Text("4") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = yieldError,
                            supportingText = {
                                if (yieldError) {
                                    Text(
                                        stringResource(
                                            R.string.msg_cant_read,
                                            state.yieldText,
                                        ),
                                    )
                                }
                            },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = state.yieldLabel,
                            onValueChange = callbacks.onYieldLabelChange,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.hint_yield_label)) },
                            singleLine = true,
                        )
                    }
                }
                item(key = "ingredients_header") {
                    Text(
                        stringResource(R.string.header_ingredients),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                items(state.rows, key = { it.key }) { row ->
                    IngredientRow(
                        row = row,
                        qtyValue = qtyValues.getOrPut(row.key) {
                            mutableStateOf(
                                TextFieldValue(
                                    row.rawQuantity,
                                    TextRange(row.rawQuantity.length),
                                ),
                            )
                        },
                        requestFocus = pendingFocusKey == row.key,
                        onFocusedHandled = { pendingFocusKey = null },
                        onFocusChanged = { hasFocus, field ->
                            if (hasFocus) {
                                focused = FocusedField(row.key, field)
                            } else if (focused?.rowKey == row.key && focused?.field == field) {
                                focused = null
                            }
                        },
                        onQtyChange = { callbacks.onQtyChange(row.key, it) },
                        onUnitClick = { unitPickerRowKey = row.key },
                        onNameChange = { callbacks.onNameChange(row.key, it) },
                        onNext = ::goNext,
                        onRemove = { callbacks.onRemoveRow(row.key) },
                    )
                }
                item(key = "add_row") {
                    TextButton(
                        onClick = { callbacks.onAddRow(null) },
                        enabled = state.rows.size < MAX_INGREDIENTS,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(stringResource(R.string.action_add_ingredient))
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
                },
                onDismiss = { unitPickerRowKey = null },
            )
        }
    }

    EditorDialogs(state = state, callbacks = callbacks)
}

@Composable
private fun IngredientRow(
    row: EditorRow,
    qtyValue: androidx.compose.runtime.MutableState<TextFieldValue>,
    requestFocus: Boolean,
    onFocusedHandled: () -> Unit,
    onFocusChanged: (hasFocus: Boolean, field: RowField) -> Unit,
    onQtyChange: (String) -> Unit,
    onUnitClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onNext: () -> Unit,
    onRemove: () -> Unit,
) {
    val qtyFocusRequester = remember { FocusRequester() }
    if (requestFocus) {
        LaunchedEffect(Unit) {
            qtyFocusRequester.requestFocus()
            onFocusedHandled()
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedTextField(
            value = qtyValue.value,
            onValueChange = { v ->
                qtyValue.value = v
                onQtyChange(v.text)
            },
            modifier = Modifier
                .width(88.dp)
                .focusRequester(qtyFocusRequester)
                .onFocusChanged { onFocusChanged(it.hasFocus, RowField.QTY) },
            placeholder = { Text(stringResource(R.string.hint_quantity)) },
            isError = row.qtyError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
        OutlinedButton(
            onClick = onUnitClick,
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text(row.unit.label)
        }
        OutlinedTextField(
            value = row.name,
            onValueChange = onNameChange,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { onFocusChanged(it.hasFocus, RowField.NAME) },
            placeholder = { Text(stringResource(R.string.hint_ingredient_name)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onNext() }),
            singleLine = true,
        )
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.action_remove_ingredient),
            )
        }
    }
}

@Composable
private fun AccessoryBar(
    visible: Boolean,
    qtyFocused: Boolean,
    onFraction: (String) -> Unit,
    onUnit: (KitchenUnit) -> Unit,
    onNext: () -> Unit,
) {
    AnimatedVisibility(visible = visible) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FRACTION_INSERTS.forEach { ch ->
                FilterChip(
                    selected = false,
                    enabled = qtyFocused,
                    onClick = { onFraction(ch) },
                    label = { Text(ch) },
                )
            }
            BAR_UNITS.forEach { unit ->
                FilterChip(
                    selected = false,
                    onClick = { onUnit(unit) },
                    label = { Text(unit.label) },
                )
            }
            TextButton(onClick = onNext) {
                Text(stringResource(R.string.action_next))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun UnitPickerSheet(
    selected: KitchenUnit,
    onSelect: (KitchenUnit) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                stringResource(R.string.unit_picker_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            listOf(
                UnitFamily.VOLUME to R.string.unit_group_volume,
                UnitFamily.WEIGHT to R.string.unit_group_weight,
                UnitFamily.COUNT to R.string.unit_group_other,
            ).forEach { (family, labelRes) ->
                Text(
                    stringResource(labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KitchenUnit.entries
                        .filter { it.family == family }
                        .forEach { unit ->
                            FilterChip(
                                selected = unit == selected,
                                onClick = { onSelect(unit) },
                                label = { Text(unit.label) },
                            )
                        }
                }
            }
        }
    }
}

@Composable
private fun EditorDialogs(state: EditorUiState, callbacks: EditorCallbacks) {
    when (val dialog = state.dialog) {
        EditorDialog.PasteText -> {
            val clipboard = LocalClipboard.current
            var text by remember { mutableStateOf("") }
            LaunchedEffect(Unit) {
                text = clipboard.getClipEntry()
                    ?.clipData?.getItemAt(0)?.text?.toString().orEmpty()
            }
            AlertDialog(
                onDismissRequest = callbacks.onDismissDialog,
                title = { Text(stringResource(R.string.paste_dialog_title)) },
                text = {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        placeholder = { Text(stringResource(R.string.paste_dialog_hint)) },
                    )
                },
                confirmButton = {
                    TextButton(onClick = { callbacks.onImportText(text) }) {
                        Text(stringResource(R.string.action_import))
                    }
                },
                dismissButton = {
                    TextButton(onClick = callbacks.onDismissDialog) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }
        is EditorDialog.ConfirmImport -> AlertDialog(
            onDismissRequest = callbacks.onDismissDialog,
            title = { Text(stringResource(R.string.confirm_import_title)) },
            text = { Text(stringResource(R.string.confirm_import_message)) },
            confirmButton = {
                Row {
                    TextButton(onClick = callbacks.onImportReplace) {
                        Text(stringResource(R.string.action_replace))
                    }
                    TextButton(onClick = callbacks.onImportAppend) {
                        Text(stringResource(R.string.action_append))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = callbacks.onDismissDialog) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
        EditorDialog.ConfirmDelete -> AlertDialog(
            onDismissRequest = callbacks.onDismissDialog,
            title = { Text(stringResource(R.string.confirm_delete_title)) },
            text = { Text(stringResource(R.string.confirm_delete_message)) },
            confirmButton = {
                TextButton(onClick = callbacks.onDeleteConfirm) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = callbacks.onDismissDialog) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
        EditorDialog.ConfirmClear -> AlertDialog(
            onDismissRequest = callbacks.onDismissDialog,
            title = { Text(stringResource(R.string.confirm_clear_title)) },
            text = { Text(stringResource(R.string.confirm_clear_message)) },
            confirmButton = {
                TextButton(onClick = callbacks.onClearConfirm) {
                    Text(stringResource(R.string.action_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = callbacks.onDismissDialog) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
        null -> Unit
    }
}
