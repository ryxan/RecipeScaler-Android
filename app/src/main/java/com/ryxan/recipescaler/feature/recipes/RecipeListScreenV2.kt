package com.ryxan.recipescaler.feature.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ryxan.recipescaler.R
import com.ryxan.recipescaler.core.designsystem.Spacing
import com.ryxan.recipescaler.core.designsystem.V2Card
import com.ryxan.recipescaler.core.designsystem.V2ExtendedFab
import com.ryxan.recipescaler.core.designsystem.V2MetaPill
import com.ryxan.recipescaler.core.designsystem.V2SearchField
import com.ryxan.recipescaler.core.designsystem.V2SectionHeader
import com.ryxan.recipescaler.core.designsystem.V2SwipeToDelete
import com.ryxan.recipescaler.core.designsystem.V2TopAppBar
import com.ryxan.recipescaler.core.domain.model.RecipeSummary
import com.ryxan.recipescaler.core.domain.model.formatYield
import com.ryxan.recipescaler.navigation.RescaleBottomNav
import com.ryxan.recipescaler.navigation.RescaleTab
import kotlinx.coroutines.launch

/**
 * Culinary Material (v2) recipe list. Feature-identical to [RecipeListContent] —
 * same search, delete + undo, quick scale and empty states — restyled after the
 * Stitch mock: pill search field, hero recipe cards, persimmon FAB. Deletion is
 * a deliberate two-step: drag left reveals a trash strip, tapping it deletes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListV2Content(
    state: RecipeListUiState,
    onQueryChange: (String) -> Unit,
    onOpenRecipe: (Long) -> Unit,
    onNewRecipe: () -> Unit,
    onQuickScale: () -> Unit,
    onDelete: (Long) -> Unit,
    onUndo: () -> Unit,
    onDismissUndo: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onScalerClick: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val listState = rememberLazyListState()
    val fabExpanded by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scope = rememberCoroutineScope()
    UndoDeleteSnackbar(state, snackbarHostState, onUndo, onDismissUndo)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            V2TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
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
            V2ExtendedFab(
                onClick = onNewRecipe,
                expanded = fabExpanded,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_new_recipe)) },
            )
        },
        bottomBar = {
            RescaleBottomNav(
                selected = RescaleTab.RECIPES,
                onRecipesClick = {
                    scope.launch { listState.animateScrollToItem(0) }
                },
                onScalerClick = onScalerClick,
                onSettingsClick = onNavigateToSettings,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state.recipes.isNotEmpty() || state.query.isNotBlank()) {
                V2SearchField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    placeholder = stringResource(R.string.search_recipes),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.l, vertical = Spacing.s),
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
                )
            }

            when {
                state.recipes.isEmpty() && state.query.isBlank() && !state.isLoading -> {
                    V2EmptyMessage(
                        title = stringResource(R.string.empty_recipes_title),
                        body = stringResource(R.string.empty_recipes_body),
                        actionLabel = stringResource(R.string.quick_scale_title),
                        onAction = onQuickScale,
                    )
                }
                state.recipes.isEmpty() && state.query.isNotBlank() -> {
                    V2EmptyMessage(
                        title = stringResource(R.string.empty_search_results),
                        body = null,
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                    ) {
                        item(key = "header") {
                            V2SectionHeader(text = stringResource(R.string.title_recipes))
                        }
                        items(state.recipes, key = { it.id }) { summary ->
                            V2SwipeToDelete(
                                onDelete = { onDelete(summary.id) },
                                deleteLabel = stringResource(R.string.action_delete),
                                dismissLabel = stringResource(R.string.action_dismiss),
                                modifier = Modifier
                                    .animateItem()
                                    .padding(horizontal = Spacing.l, vertical = 6.dp),
                            ) {
                                RecipeCardV2(
                                    summary = summary,
                                    onOpen = { onOpenRecipe(summary.id) },
                                    onDelete = { onDelete(summary.id) },
                                    modifier = Modifier.fillMaxWidth(),
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
private fun RecipeCardV2(
    summary: RecipeSummary,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val deleteLabel = stringResource(R.string.action_delete)
    V2Card(
        onClick = onOpen,
        modifier = modifier.semantics {
            customActions = listOf(
                CustomAccessibilityAction(label = deleteLabel) {
                    onDelete()
                    true
                },
            )
        },
    ) {
        Column(modifier = Modifier.padding(Spacing.l)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                MonogramTile(title = summary.title)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.title.ifBlank { stringResource(R.string.untitled_recipe) },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (summary.yieldAmount > 0.0) {
                            V2MetaPill(
                                text = "${formatYield(summary.yieldAmount)} ${summary.yieldLabel}",
                                icon = Icons.Filled.Group,
                            )
                        }
                        V2MetaPill(
                            text = pluralStringResource(
                                R.plurals.ingredient_count,
                                summary.ingredientCount,
                                summary.ingredientCount,
                            ),
                            icon = Icons.AutoMirrored.Outlined.MenuBook,
                        )
                    }
                }
            }
        }
    }
}

/** Letter tile standing in for the recipe photo the Stitch mock uses. */
@Composable
private fun MonogramTile(title: String) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primaryFixed),
        contentAlignment = Alignment.Center,
    ) {
        val letter = title.trim()
            .firstOrNull { it.isLetter() }
            ?.uppercaseChar()
        if (letter == null) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryFixedVariant,
            )
        } else {
            Text(
                text = letter.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryFixedVariant,
            )
        }
    }
}

@Composable
private fun V2EmptyMessage(
    title: String,
    body: String?,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
            modifier = Modifier.padding(Spacing.xl),
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryFixed),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryFixedVariant,
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
