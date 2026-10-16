package com.ryxan.recipescaler.feature.editor

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ryxan.recipescaler.R
import com.ryxan.recipescaler.core.designsystem.GradientTopAppBar
import com.ryxan.recipescaler.core.designsystem.RescaleCard
import com.ryxan.recipescaler.core.designsystem.RescaleSwipeToDelete
import com.ryxan.recipescaler.core.designsystem.Spacing
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.UnitFamily
import com.ryxan.recipescaler.feature.importer.ScanError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun EditorScreen(
    onNavigateUp: () -> Unit,
    onScale: (Long) -> Unit,
    onNavigateToPro: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val nothingImported = stringResource(R.string.msg_nothing_imported)
    val capReached = stringResource(R.string.ingredient_cap_reached)
    val scannerUnavailable = stringResource(R.string.scan_no_scanner)
    val scanNotRight = stringResource(R.string.scan_snackbar_not_right)
    val tryAiLabel = stringResource(R.string.action_try_ai_import)

    // ── Scan launchers ────────────────────────────────────────────────────────

    // ML Kit Document Scanner: live edge detection, auto-capture, perspective
    // correction, cleanup filters, and built-in gallery import — no CAMERA
    // permission required.
    val docScanner = remember {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        GmsDocumentScanning.getClient(options)
    }
    val docScannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result: ActivityResult ->
        val uri = if (result.resultCode == Activity.RESULT_OK) {
            GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                ?.pages?.firstOrNull()?.imageUri
        } else {
            null
        }
        if (uri != null) {
            viewModel.onImageCaptured(uri, deleteWhenDone = true)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onImageCaptured(uri, deleteWhenDone = false)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EditorEvent.NavigateUp -> onNavigateUp()
                EditorEvent.NothingToImport ->
                    snackbarHostState.showSnackbar(nothingImported)
                EditorEvent.IngredientCapReached -> snackbarHostState.showSnackbar(capReached)
                EditorEvent.ScanLooksPoor -> {
                    val result = snackbarHostState.showSnackbar(
                        message = scanNotRight,
                        actionLabel = tryAiLabel,
                    )
                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                        viewModel.retryAiImport()
                    }
                }
                is EditorEvent.CreditsGranted -> snackbarHostState.showSnackbar(
                    context.resources.getQuantityString(
                        R.plurals.ai_scans_granted,
                        event.count,
                        event.count,
                    ),
                )
                is EditorEvent.PurchaseError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    EditorContent(
        state = state,
        snackbarHostState = snackbarHostState,
        callbacks = EditorCallbacks(
            onNavigateUp = onNavigateUp,
            onSettings = onNavigateToSettings,
            onScale = { scope.launch { viewModel.prepareToScale()?.let(onScale) } },
            onTitleChange = viewModel::onTitleChange,
            onYieldTextChange = viewModel::onYieldTextChange,
            onQtyChange = viewModel::onRowQtyChange,
            onUnitChange = viewModel::onRowUnitChange,
            onNameChange = viewModel::onRowNameChange,
            onAddRow = viewModel::addRow,
            onRemoveRow = viewModel::removeRow,
            onPasteClick = viewModel::openPasteDialog,
            onImportText = viewModel::importText,
            onImportReplace = viewModel::confirmImportReplace,
            onImportAppend = viewModel::confirmImportAppend,
            onClearClick = viewModel::clearIngredients,
            onDelete = viewModel::deleteRecipe,
            onUndoEdit = viewModel::undoPendingEdit,
            onDismissUndo = viewModel::dismissPendingUndo,
            onMoveRow = viewModel::moveRow,
            onInferName = viewModel::inferFromName,
            onDismissDialog = viewModel::dismissDialog,
            onScanClick = viewModel::openScanSheet,
            onUseAiChange = viewModel::setUseAiImport,
            onScanDocument = {
                val activity = context as? Activity
                if (activity == null) {
                    scope.launch { snackbarHostState.showSnackbar(scannerUnavailable) }
                } else {
                    docScanner.getStartScanIntent(activity)
                        .addOnSuccessListener { intentSender ->
                            try {
                                docScannerLauncher.launch(
                                    IntentSenderRequest.Builder(intentSender).build(),
                                )
                            } catch (e: Exception) {
                                scope.launch { snackbarHostState.showSnackbar(scannerUnavailable) }
                            }
                        }
                        .addOnFailureListener {
                            scope.launch { snackbarHostState.showSnackbar(scannerUnavailable) }
                        }
                }
            },
            onChooseGallery = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onRetryAi = viewModel::retryAiImport,
            onUseOnDevice = viewModel::useOnDevice,
            onSeePro = {
                viewModel.dismissDialog()
                onNavigateToPro()
            },
            onBuyProduct = { productId ->
                (context as? Activity)?.let { viewModel.purchaseProduct(it, productId) }
            },
            onWatchAd = {
                (context as? Activity)?.let(viewModel::watchAdForScans)
            },
        ),
    )
}

/** Bundled callbacks so [EditorContent] stays easy to drive from UI tests. */
data class EditorCallbacks(
    val onNavigateUp: () -> Unit = {},
    val onSettings: () -> Unit = {},
    val onScale: () -> Unit = {},
    val onTitleChange: (String) -> Unit = {},
    val onYieldTextChange: (String) -> Unit = {},
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
    val onDelete: () -> Unit = {},
    val onUndoEdit: () -> Unit = {},
    val onDismissUndo: () -> Unit = {},
    val onMoveRow: (Long, Long) -> Unit = { _, _ -> },
    val onInferName: (Long) -> Unit = {},
    val onDismissDialog: () -> Unit = {},
    val onScanClick: () -> Unit = {},
    val onUseAiChange: (Boolean) -> Unit = {},
    val onScanDocument: () -> Unit = {},
    val onChooseGallery: () -> Unit = {},
    val onRetryAi: () -> Unit = {},
    val onUseOnDevice: () -> Unit = {},
    val onSeePro: () -> Unit = {},
    val onBuyProduct: (String) -> Unit = {},
    val onWatchAd: () -> Unit = {},
)

private enum class RowField { QTY, NAME }
private data class FocusedField(val rowKey: Long, val field: RowField)

/** LazyColumn sections above the ingredient rows, in display order. */
private enum class EditorHeader { TITLE_HEADER, TITLE, YIELD, INGREDIENTS_HEADER, EMPTY_STATE }

private fun editorHeaders(rows: List<EditorRow>): List<EditorHeader> = buildList {
    add(EditorHeader.TITLE_HEADER)
    add(EditorHeader.TITLE)
    add(EditorHeader.YIELD)
    add(EditorHeader.INGREDIENTS_HEADER)
    if (rows.all { it.rawQuantity.isBlank() && it.name.isBlank() }) {
        add(EditorHeader.EMPTY_STATE)
    }
}

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
    var pendingFocus by remember { mutableStateOf<FocusedField?>(null) }
    var unitPickerRowKey by remember { mutableStateOf<Long?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var autoFocusHandled by rememberSaveable { mutableStateOf(false) }
    val qtyValues = remember { mutableStateMapOf<Long, androidx.compose.runtime.MutableState<TextFieldValue>>() }
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromKey = from.key
        val toKey = to.key
        if (fromKey is Long && toKey is Long) callbacks.onMoveRow(fromKey, toKey)
    }
    val fabExpanded by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex == 0 }
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
            if (idx < 0) -1 else idx + editorHeaders(s.rows).size
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GradientTopAppBar(
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
                    IconButton(onClick = callbacks.onScanClick) {
                        Icon(
                            Icons.Outlined.DocumentScanner,
                            contentDescription = stringResource(R.string.action_scan_recipe),
                        )
                    }
                    IconButton(onClick = callbacks.onSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.title_settings),
                        )
                    }
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
                                        callbacks.onDelete()
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
                onClick = {
                    if (canScale) {
                        callbacks.onScale()
                    } else {
                        scope.launch { snackbarHostState.showSnackbar(fabNeedIngredient) }
                    }
                },
                expanded = fabExpanded,
                modifier = Modifier.semantics { if (!canScale) disabled() },
                containerColor = if (canScale) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                },
                contentColor = if (canScale) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
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
            state.importBusy?.let { busy ->
                Column(Modifier.padding(horizontal = 16.dp)) {
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
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(editorHeaders(state.rows), key = { it.name }) { section ->
                    when (section) {
                        EditorHeader.TITLE_HEADER -> Text(
                            stringResource(R.string.header_name),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        EditorHeader.TITLE -> OutlinedTextField(
                            value = state.title,
                            onValueChange = callbacks.onTitleChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription = titleCd
                                },
                            singleLine = true,
                        )
                        EditorHeader.YIELD -> {
                            val yieldError = state.yieldError
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        stringResource(R.string.editor_makes),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    OutlinedTextField(
                                        value = state.yieldText,
                                        onValueChange = callbacks.onYieldTextChange,
                                        modifier = Modifier
                                            .width(96.dp)
                                            .semantics { contentDescription = yieldAmountCd },
                                        placeholder = { Text("4") },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal,
                                        ),
                                        isError = yieldError,
                                        singleLine = true,
                                    )
                                }
                                if (yieldError) {
                                    Text(
                                        stringResource(R.string.msg_cant_read_example, state.yieldText),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                                    )
                                }
                            }
                        }
                        EditorHeader.INGREDIENTS_HEADER -> Text(
                            stringResource(R.string.header_ingredients),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        EditorHeader.EMPTY_STATE -> Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilledTonalButton(
                                onClick = callbacks.onScanClick,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    Icons.Outlined.DocumentScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(stringResource(R.string.action_scan))
                            }
                            FilledTonalButton(
                                onClick = callbacks.onPasteClick,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    Icons.Outlined.ContentPaste,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(stringResource(R.string.action_paste))
                            }
                            FilledTonalButton(
                                onClick = {
                                    val key = state.rows.firstOrNull()?.key
                                        ?: callbacks.onAddRow(null)
                                    if (key >= 0) pendingFocus = FocusedField(key, RowField.QTY)
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    Icons.Outlined.Keyboard,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(stringResource(R.string.action_type))
                            }
                        }
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
                            IngredientRow(
                                row = row,
                                position = idx + 1,
                                isDragging = isDragging,
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
                                onNext = ::goNext,
                            )
                        }
                    }
                }
                item(key = "add_row") {
                    TextButton(
                        onClick = {
                            val key = callbacks.onAddRow(null)
                            if (key >= 0) pendingFocus = FocusedField(key, RowField.QTY)
                        },
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

@Composable
private fun IngredientRow(
    row: EditorRow,
    position: Int,
    isDragging: Boolean,
    qtyValue: androidx.compose.runtime.MutableState<TextFieldValue>,
    modifier: Modifier = Modifier,
    requestFocus: RowField?,
    onFocusedHandled: () -> Unit,
    onFocusChanged: (hasFocus: Boolean, field: RowField) -> Unit,
    onQtyChange: (String) -> Unit,
    onUnitClick: () -> Unit,
    onNameChange: (String) -> Unit,
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
    RescaleCard(
        modifier = modifier.shadow(
            elevation = if (isDragging) 6.dp else 0.dp,
            shape = MaterialTheme.shapes.large,
        ),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .padding(Spacing.m),
        ) {
            // Stack only when genuinely cramped: large font scale, or a row so
            // narrow (split-screen, foldable cover) the name field couldn't fit
            // beside qty+unit. ~260dp content ≈ <340dp screen.
            val stacked = fontScale > 1.3f || maxWidth < 260.dp
            if (stacked) {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Top,
                ) {
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
    }
}

@Composable
private fun QtyField(
    value: androidx.compose.runtime.MutableState<TextFieldValue>,
    isError: Boolean,
    contentDesc: String,
    modifier: Modifier,
    focusRequester: FocusRequester,
    onNext: () -> Unit,
    onFocusChanged: (hasFocus: Boolean, field: RowField) -> Unit,
    onQtyChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value.value,
        onValueChange = { v ->
            value.value = v
            onQtyChange(v.text)
        },
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { onFocusChanged(it.hasFocus, RowField.QTY) }
            .semantics {
                contentDescription = contentDesc
            },
        placeholder = { Text(stringResource(R.string.hint_quantity)) },
        isError = isError,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(
            onNext = { onNext() },
        ),
        singleLine = true,
    )
}

@Composable
private fun UnitPickerButton(
    unit: KitchenUnit,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(56.dp),
        shape = OutlinedTextFieldDefaults.shape,
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        Text(unit.label)
        Icon(
            Icons.Filled.ArrowDropDown,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun NameField(
    row: EditorRow,
    contentDesc: String,
    modifier: Modifier,
    focusRequester: FocusRequester,
    onFocusChanged: (hasFocus: Boolean, field: RowField) -> Unit,
    onNameChange: (String) -> Unit,
    onNext: () -> Unit,
) {
    OutlinedTextField(
        value = row.name,
        onValueChange = onNameChange,
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { onFocusChanged(it.hasFocus, RowField.NAME) }
            .semantics {
                contentDescription = contentDesc
            },
        placeholder = { Text(stringResource(R.string.hint_ingredient_name)) },
        supportingText = {
            if (row.needsReview) {
                Row(
                    modifier = Modifier.semantics(mergeDescendants = true) {},
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        text = stringResource(R.string.row_needs_review),
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        maxLines = 3,
    )
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
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (qtyFocused) {
                    FRACTION_INSERTS.forEach { ch ->
                        FilterChip(
                            selected = false,
                            onClick = { onFraction(ch) },
                            label = { Text(ch) },
                        )
                    }
                }
                BAR_UNITS.forEach { unit ->
                    FilterChip(
                        selected = false,
                        onClick = { onUnit(unit) },
                        label = { Text(unit.label) },
                    )
                }
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

@OptIn(ExperimentalMaterial3Api::class)
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
        is EditorDialog.ConfirmImport -> ModalBottomSheet(
            onDismissRequest = callbacks.onDismissDialog,
        ) {
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    stringResource(R.string.confirm_import_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.confirm_import_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )
                Button(
                    onClick = callbacks.onImportReplace,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.action_replace_ingredients))
                }
                OutlinedButton(
                    onClick = callbacks.onImportAppend,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.action_append_ingredients))
                }
            }
        }
        EditorDialog.ScanSource -> ScanSourceSheet(
            state = state,
            callbacks = callbacks,
        )
        is EditorDialog.ScanFailed -> AlertDialog(
            onDismissRequest = callbacks.onDismissDialog,
            title = { Text(stringResource(R.string.scan_failed_title)) },
            text = { Text(stringResource(scanErrorText(dialog.error))) },
            confirmButton = {
                Row {
                    if (dialog.canRetryWithAi) {
                        TextButton(onClick = callbacks.onRetryAi) {
                            Text(stringResource(R.string.action_try_ai_import))
                        }
                    }
                    if (dialog.canUseOnDevice) {
                        TextButton(onClick = callbacks.onUseOnDevice) {
                            Text(stringResource(R.string.action_scan_on_device))
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = callbacks.onDismissDialog) {
                    Text(stringResource(R.string.action_ok))
                }
            },
        )
        EditorDialog.AiPaywall -> AiPaywallDialog(
            products = state.paywallProducts,
            rewardedAdAvailable = state.rewardedAdAvailable,
            isAdLoading = state.isAdLoading,
            onDismiss = callbacks.onDismissDialog,
            onBuyProduct = { productId ->
                callbacks.onDismissDialog()
                callbacks.onBuyProduct(productId)
            },
            onWatchAd = callbacks.onWatchAd,
            onSeePro = callbacks.onSeePro,
        )
        null -> Unit
    }
}

private fun scanErrorText(error: ScanError): Int = when (error) {
    ScanError.NO_INGREDIENTS_FOUND -> R.string.scan_error_no_ingredients
    ScanError.RECOGNITION_FAILED -> R.string.scan_error_recognition
    ScanError.AI_NETWORK_ERROR -> R.string.scan_error_network
    ScanError.AI_BLOCKED -> R.string.scan_error_blocked
    ScanError.AI_FAILED -> R.string.scan_error_failed
    ScanError.AI_LIMIT_REACHED -> R.string.scan_error_limit
    ScanError.AI_RATE_LIMITED -> R.string.scan_error_rate_limited
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScanSourceSheet(
    state: EditorUiState,
    callbacks: EditorCallbacks,
) {
    ModalBottomSheet(onDismissRequest = callbacks.onDismissDialog) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                stringResource(R.string.scan_source_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.action_scan)) },
                modifier = Modifier.clickableEntry { callbacks.onScanDocument() },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.action_choose_photo)) },
                modifier = Modifier.clickableEntry { callbacks.onChooseGallery() },
            )
            if (state.aiAvailable) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.scan_use_ai)) },
                    supportingContent = {
                        Text(
                            when {
                                state.isPro -> stringResource(R.string.scan_ai_pro)
                                state.aiScansRemaining > 0 -> stringResource(
                                    R.string.scan_ai_free_left,
                                    state.aiScansRemaining,
                                )
                                else -> stringResource(R.string.scan_ai_pro_feature)
                            },
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = state.useAiImport,
                            onCheckedChange = callbacks.onUseAiChange,
                        )
                    },
                )
            }
        }
    }
}

private fun Modifier.clickableEntry(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))
