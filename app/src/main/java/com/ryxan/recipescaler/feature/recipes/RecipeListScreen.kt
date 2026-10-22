package com.ryxan.recipescaler.feature.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ryxan.recipescaler.R
import com.ryxan.recipescaler.core.designsystem.GradientExtendedFab
import com.ryxan.recipescaler.core.designsystem.GradientTopAppBar
import com.ryxan.recipescaler.core.designsystem.RescaleCard
import com.ryxan.recipescaler.core.designsystem.RescaleSwipeToDelete
import com.ryxan.recipescaler.core.designsystem.RescaleTheme
import com.ryxan.recipescaler.core.designsystem.Spacing
import com.ryxan.recipescaler.core.domain.model.RecipeSummary
import com.ryxan.recipescaler.core.domain.model.formatYield

@Composable
fun RecipeListScreen(
    onOpenRecipe: (Long) -> Unit,
    onNewRecipe: () -> Unit,
    onQuickScale: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onScalerClick: () -> Unit = {},
    viewModel: RecipeListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val quickScaleTitle = stringResource(R.string.quick_scale_title)
    if (RescaleTheme.uiV2) {
        RecipeListV2Content(
            state = state,
            onQueryChange = viewModel::onQueryChange,
            onOpenRecipe = onOpenRecipe,
            onNewRecipe = onNewRecipe,
            onQuickScale = { onQuickScale(quickScaleTitle) },
            onDelete = viewModel::delete,
            onUndo = viewModel::undoDelete,
            onDismissUndo = viewModel::dismissUndo,
            onNavigateToSettings = onNavigateToSettings,
            onScalerClick = onScalerClick,
        )
        return
    }
    RecipeListContent(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onOpenRecipe = onOpenRecipe,
        onNewRecipe = onNewRecipe,
        onQuickScale = { onQuickScale(quickScaleTitle) },
        onDelete = viewModel::delete,
        onUndo = viewModel::undoDelete,
        onDismissUndo = viewModel::dismissUndo,
        onNavigateToSettings = onNavigateToSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListContent(
    state: RecipeListUiState,
    onQueryChange: (String) -> Unit,
    onOpenRecipe: (Long) -> Unit,
    onNewRecipe: () -> Unit,
    onQuickScale: () -> Unit,
    onDelete: (Long) -> Unit,
    onUndo: () -> Unit,
    onDismissUndo: () -> Unit,
    onNavigateToSettings: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val listState = rememberLazyListState()
    val fabExpanded by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }
    UndoDeleteSnackbar(state, snackbarHostState, onUndo, onDismissUndo)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.title_recipes)) },
                actions = {
                    IconButton(onClick = onQuickScale) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = stringResource(R.string.quick_scale_title),
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.title_settings),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            GradientExtendedFab(
                onClick = onNewRecipe,
                expanded = fabExpanded,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_new_recipe)) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state.recipes.isNotEmpty() || state.query.isNotBlank()) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.l, vertical = Spacing.s),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                    placeholder = { Text(stringResource(R.string.search_recipes)) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    Icons.Filled.Clear,
                                    contentDescription = stringResource(R.string.action_clear_search),
                                )
                            }
                        }
                    },
                    singleLine = true,
                )
            }

            when {
                state.recipes.isEmpty() && state.query.isBlank() && !state.isLoading -> {
                    EmptyMessage(
                        title = stringResource(R.string.empty_recipes_title),
                        body = stringResource(R.string.empty_recipes_body),
                        actionLabel = stringResource(R.string.quick_scale_title),
                        onAction = onQuickScale,
                    )
                }
                state.recipes.isEmpty() && state.query.isNotBlank() -> {
                    EmptyMessage(
                        title = stringResource(R.string.empty_search_results),
                        body = null,
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp),
                    ) {
                        items(state.recipes, key = { it.id }) { summary ->
                            RescaleSwipeToDelete(
                                onDelete = { onDelete(summary.id) },
                                modifier = Modifier
                                    .animateItem()
                                    .padding(horizontal = Spacing.l, vertical = 6.dp),
                            ) {
                                RescaleCard(Modifier.fillMaxWidth()) {
                                    RecipeRow(
                                        summary = summary,
                                        onClick = { onOpenRecipe(summary.id) },
                                        onDelete = { onDelete(summary.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Shows the undo-delete snackbar whenever [state.pendingUndo] is set. Shared by the v1 and v2 list content. */
@Composable
internal fun UndoDeleteSnackbar(
    state: RecipeListUiState,
    snackbarHostState: SnackbarHostState,
    onUndo: () -> Unit,
    onDismissUndo: () -> Unit,
) {
    val undoLabel = stringResource(R.string.action_undo)
    val deletedMessage = state.pendingUndo?.let {
        if (it.title.isBlank()) stringResource(R.string.snackbar_recipe_deleted)
        else stringResource(R.string.snackbar_deleted_titled, it.title)
    }
    LaunchedEffect(state.pendingUndo?.id) {
        if (deletedMessage != null) {
            when (
                snackbarHostState.showSnackbar(
                    message = deletedMessage,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Long,
                )
            ) {
                SnackbarResult.ActionPerformed -> onUndo()
                SnackbarResult.Dismissed -> onDismissUndo()
            }
        }
    }
}

@Composable
private fun RecipeRow(
    summary: RecipeSummary,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val deleteLabel = stringResource(R.string.action_delete)
    ListItem(
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(label = deleteLabel) {
                        onDelete()
                        true
                    },
                )
            },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                val letter = summary.title.trim()
                    .firstOrNull { it.isLetter() }
                    ?.uppercaseChar()
                if (letter == null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    Text(
                        text = letter.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        },
        headlineContent = {
            Text(
                text = summary.title.ifBlank { stringResource(R.string.untitled_recipe) },
            )
        },
        supportingContent = {
            Text(supportingText(summary))
        },
    )
}

@Composable
private fun supportingText(summary: RecipeSummary): String {
    val ingredients = pluralStringResource(
        R.plurals.ingredient_count,
        summary.ingredientCount,
        summary.ingredientCount,
    )
    return if (summary.yieldAmount > 0.0) {
        val yield = formatYield(summary.yieldAmount)
        "${yield} ${summary.yieldLabel} · $ingredients"
    } else {
        ingredients
    }
}

@Composable
private fun EmptyMessage(
    title: String,
    body: String?,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(RescaleTheme.brandGradient),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = RescaleTheme.extendedColors.onGradient,
                )
            }
            Text(title, style = MaterialTheme.typography.headlineSmall)
            if (body != null) {
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (actionLabel != null && onAction != null) {
                FilledTonalButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}
