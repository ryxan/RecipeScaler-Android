package com.ryxan.recipescaler.feature.editor

import android.app.Activity
import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
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
import com.ryxan.recipescaler.core.designsystem.GradientExtendedFab
import com.ryxan.recipescaler.core.designsystem.GradientTopAppBar
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.UnitFamily
import com.ryxan.recipescaler.feature.importer.ScanError
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun EditorScreen(
    onNavigateUp: () -> Unit,
    onScale: (Long) -> Unit,
    onNavigateToPro: () -> Unit = {},
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val nothingImported = stringResource(R.string.msg_nothing_imported)
    val capReached = stringResource(R.string.ingredient_cap_reached)
    val scannerUnavailable = stringResource(R.string.scan_no_scanner)
    val noCamera = stringResource(R.string.scan_no_camera)
    val scanNotRight = stringResource(R.string.scan_snackbar_not_right)
    val tryAiLabel = stringResource(R.string.action_try_ai_import)

    // ── Scan launchers ────────────────────────────────────────────────────────

    // TakePicture grants the camera app a URI permission, so no CAMERA runtime
    // permission is needed.
    var pendingCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success: Boolean ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) {
            viewModel.onImageCaptured(uri, deleteWhenDone = true)
        }
    }

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
            onClearClick = viewModel::requestClear,
            onClearConfirm = viewModel::confirmClear,
            onDelete = viewModel::deleteRecipe,
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
            onTakePhoto = {
                val uri = viewModel.newCameraUri()
                if (uri == null) {
                    scope.launch { snackbarHostState.showSnackbar(noCamera) }
                } else {
                    pendingCameraUri = uri
                    try {
                        cameraLauncher.launch(uri)
                    } catch (e: ActivityNotFoundException) {
                        pendingCameraUri = null
                        scope.launch { snackbarHostState.showSnackbar(noCamera) }
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
    val onClearConfirm: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onMoveRow: (Long, Long) -> Unit = { _, _ -> },
    val onInferName: (Long) -> Unit = {},
    val onDismissDialog: () -> Unit = {},
    val onScanClick: () -> Unit = {},
    val onUseAiChange: (Boolean) -> Unit = {},
    val onScanDocument: () -> Unit = {},
    val onTakePhoto: () -> Unit = {},
    val onChooseGallery: () -> Unit = {},
    val onRetryAi: () -> Unit = {},
    val onUseOnDevice: () -> Unit = {},
    val onSeePro: () -> Unit = {},
    val onBuyProduct: (String) -> Unit = {},
    val onWatchAd: () -> Unit = {},
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
    var autoFocusHandled by rememberSaveable { mutableStateOf(false) }
    var editMode by rememberSaveable { mutableStateOf(false) }
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
    val titleCd = stringResource(R.string.cd_recipe_title)

    LaunchedEffect(state.autoFocusKey) {
        if (!autoFocusHandled && state.autoFocusKey != null) {
            pendingFocusKey = state.autoFocusKey
            autoFocusHandled = true
        }
    }

    LaunchedEffect(state.rows.isEmpty()) {
        if (state.rows.isEmpty()) editMode = false
    }

    fun goNext() {
        val f = focused ?: return
        if (f.field == RowField.NAME) callbacks.onInferName(f.rowKey)
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
                    if (state.rows.isNotEmpty()) {
                        IconButton(onClick = { editMode = !editMode }) {
                            if (editMode) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = stringResource(R.string.action_done_editing),
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = stringResource(R.string.action_edit_ingredients),
                                )
                            }
                        }
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
            GradientExtendedFab(
                onClick = { if (canScale) callbacks.onScale() },
                expanded = fabExpanded,
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
                item(key = "title_header") {
                    Text(
                        stringResource(R.string.header_name),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                item(key = "title") {
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = callbacks.onTitleChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = titleCd
                            },
                        singleLine = true,
                    )
                }
                item(key = "yield_header") {
                    Text(
                        stringResource(R.string.scale_label_serves),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                item(key = "yield") {
                    val yieldError = state.yieldError
                    OutlinedTextField(
                        value = state.yieldText,
                        onValueChange = callbacks.onYieldTextChange,
                        modifier = Modifier.width(120.dp),
                        placeholder = { Text("1") },
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
                }
                item(key = "ingredients_header") {
                    Text(
                        stringResource(R.string.header_ingredients),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                items(state.rows, key = { it.key }) { row ->
                    ReorderableItem(reorderableState, key = row.key) {
                        val idx = state.rows.indexOfFirst { it.key == row.key }
                        val prev = state.rows.getOrNull(idx - 1)
                        val next = state.rows.getOrNull(idx + 1)
                        val moveUp = stringResource(R.string.action_move_up)
                        val moveDown = stringResource(R.string.action_move_down)
                        IngredientRow(
                            row = row,
                            position = idx + 1,
                            modifier = Modifier.semantics {
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
                            dragModifier = if (editMode) Modifier.draggableHandle() else Modifier,
                            editing = editMode,
                            requestFocus = pendingFocusKey == row.key,
                            onFocusedHandled = { pendingFocusKey = null },
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
                            onRemove = { callbacks.onRemoveRow(row.key) },
                        )
                    }
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
    position: Int,
    qtyValue: androidx.compose.runtime.MutableState<TextFieldValue>,
    modifier: Modifier = Modifier,
    dragModifier: Modifier = Modifier,
    editing: Boolean = false,
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
    val nameFocusRequester = remember { FocusRequester() }
    val qtyCd = stringResource(R.string.cd_quantity_field, position)
    val nameCd = stringResource(R.string.cd_ingredient_name_field, position)
    if (requestFocus) {
        LaunchedEffect(Unit) {
            qtyFocusRequester.requestFocus()
            onFocusedHandled()
        }
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AnimatedVisibility(
            visible = editing,
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut(),
        ) {
            Icon(
                Icons.Filled.DragHandle,
                contentDescription = stringResource(R.string.action_reorder),
                modifier = Modifier.padding(top = 16.dp).then(dragModifier),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = qtyValue.value,
            onValueChange = { v ->
                qtyValue.value = v
                onQtyChange(v.text)
            },
            modifier = Modifier
                .width(88.dp)
                .focusRequester(qtyFocusRequester)
                .onFocusChanged { onFocusChanged(it.hasFocus, RowField.QTY) }
                .semantics {
                    contentDescription = qtyCd
                },
            placeholder = { Text(stringResource(R.string.hint_quantity)) },
            isError = row.qtyError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { nameFocusRequester.requestFocus() },
            ),
            singleLine = true,
        )
        OutlinedButton(
            onClick = onUnitClick,
            modifier = Modifier.height(56.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text(row.unit.label)
        }
        OutlinedTextField(
            value = row.name,
            onValueChange = onNameChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(nameFocusRequester)
                .onFocusChanged { onFocusChanged(it.hasFocus, RowField.NAME) }
                .semantics {
                    contentDescription = nameCd
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
            singleLine = true,
        )
        AnimatedVisibility(
            visible = editing,
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut(),
        ) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_remove_ingredient),
                )
            }
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
                headlineContent = { Text(stringResource(R.string.action_scan_document)) },
                modifier = Modifier.clickableEntry { callbacks.onScanDocument() },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.action_take_photo)) },
                modifier = Modifier.clickableEntry { callbacks.onTakePhoto() },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.action_choose_gallery)) },
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
