package com.ryxan.recipescaler.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ryxan.recipescaler.core.designsystem.LocalUiV2
import com.ryxan.recipescaler.core.designsystem.RescaleMotion
import com.ryxan.recipescaler.feature.editor.EditorScreen
import com.ryxan.recipescaler.feature.pro.ProScreen
import com.ryxan.recipescaler.feature.recipes.RecipeListScreen
import com.ryxan.recipescaler.feature.scale.ScaleScreen
import com.ryxan.recipescaler.feature.scaler.ScalerHomeScreen
import com.ryxan.recipescaler.feature.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
object RecipeList

@Serializable
object Settings

/** [recipeId] 0 means a new recipe; [initialTitle] prefills the title (Quick scale). */
@Serializable
data class Editor(val recipeId: Long = 0L, val initialTitle: String? = null)

@Serializable
data class Scale(val recipeId: Long)

/** v2 Scaler tab root — the empty-state hero card. */
@Serializable
object ScalerHome

@Serializable
object Pro

@Composable
fun RescaleNavHost() {
    val navController = rememberNavController()
    // The recipe currently "on the scale" for the v2 Scaler tab. Tracked
    // outside the back stack so popping the Scale entry (e.g. switching to the
    // Recipes tab) doesn't lose it; the tab falls back to the ScalerHome
    // empty state when unset.
    var scalerRecipeId by rememberSaveable { mutableStateOf<Long?>(null) }
    val uiV2 = LocalUiV2.current
    // v2 tab destinations share a bottom nav, so lateral slides between them
    // make the bar look like it's leaving the screen. Cut instantly instead —
    // returning null falls back to the NavHost defaults for pushes and pops
    // (and for v1, where every transition keeps the slide).
    val tabEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? =
        { if (uiV2 && initialState.destination.isV2Tab()) EnterTransition.None else null }
    val tabExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? =
        { if (uiV2 && targetState.destination.isV2Tab()) ExitTransition.None else null }
    val tabPopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? =
        { if (uiV2 && initialState.destination.isV2Tab()) EnterTransition.None else null }
    val tabPopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? =
        { if (uiV2 && targetState.destination.isV2Tab()) ExitTransition.None else null }
    NavHost(
        navController = navController,
        startDestination = RecipeList,
        enterTransition = {
            slideInHorizontally(
                tween(RescaleMotion.DurationEnter, easing = RescaleMotion.EmphasizedDecelerate),
            ) { it / 4 } + fadeIn(
                tween(RescaleMotion.DurationEnter, easing = RescaleMotion.EmphasizedDecelerate),
            )
        },
        exitTransition = {
            slideOutHorizontally(
                tween(RescaleMotion.DurationExit, easing = RescaleMotion.EmphasizedAccelerate),
            ) { -it / 8 } + fadeOut(
                tween(RescaleMotion.DurationExit, easing = RescaleMotion.EmphasizedAccelerate),
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                tween(RescaleMotion.DurationEnter, easing = RescaleMotion.EmphasizedDecelerate),
            ) { -it / 4 } + fadeIn(
                tween(RescaleMotion.DurationEnter, easing = RescaleMotion.EmphasizedDecelerate),
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                tween(RescaleMotion.DurationExit, easing = RescaleMotion.EmphasizedAccelerate),
            ) { it / 8 } + fadeOut(
                tween(RescaleMotion.DurationExit, easing = RescaleMotion.EmphasizedAccelerate),
            )
        },
    ) {
        composable<RecipeList>(
            enterTransition = tabEnter,
            exitTransition = tabExit,
            popEnterTransition = tabPopEnter,
            popExitTransition = tabPopExit,
        ) {
            RecipeListScreen(
                onOpenRecipe = { id ->
                    scalerRecipeId = id
                    navController.navigate(Scale(id))
                },
                onNewRecipe = { navController.navigate(Editor()) },
                onQuickScale = { title -> navController.navigate(Editor(initialTitle = title)) },
                onNavigateToSettings = { navController.navigate(Settings) },
                onScalerClick = { navController.openScalerTab(scalerRecipeId) },
            )
        }
        composable<ScalerHome>(
            enterTransition = tabEnter,
            exitTransition = tabExit,
            popEnterTransition = tabPopEnter,
            popExitTransition = tabPopExit,
        ) {
            ScalerHomeScreen(
                onBrowseRecipes = {
                    navController.popBackStack<RecipeList>(inclusive = false)
                },
                onScanImport = { navController.navigate(Editor()) },
                onNavigateToSettings = { navController.navigate(Settings) },
            )
        }
        composable<Editor> {
            EditorScreen(
                onNavigateUp = { navController.navigateUp() },
                onScale = { id ->
                    scalerRecipeId = id
                    // Returning to the Scale entry below keeps its in-session factor.
                    val previous = navController.previousBackStackEntry
                        ?.let { runCatching { it.toRoute<Scale>() }.getOrNull() }
                    if (previous?.recipeId == id) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Scale(id))
                    }
                },
                onNavigateToPro = { navController.navigate(Pro) },
                onNavigateToSettings = { navController.navigate(Settings) },
            )
        }
        composable<Scale>(
            enterTransition = tabEnter,
            exitTransition = tabExit,
            popEnterTransition = tabPopEnter,
            popExitTransition = tabPopExit,
        ) {
            ScaleScreen(
                onNavigateUp = { navController.navigateUp() },
                onEdit = { id -> navController.navigate(Editor(recipeId = id)) },
                onNavigateToSettings = { navController.navigate(Settings) },
                onNavigateToRecipes = {
                    navController.popBackStack<RecipeList>(inclusive = false)
                },
                onScalerClick = { navController.openScalerTab(scalerRecipeId) },
                onRecipeMissing = { scalerRecipeId = null },
            )
        }
        composable<Settings>(
            enterTransition = tabEnter,
            exitTransition = tabExit,
            popEnterTransition = tabPopEnter,
            popExitTransition = tabPopExit,
        ) {
            SettingsScreen(
                onNavigateUp = { navController.navigateUp() },
                onNavigateToPro = { navController.navigate(Pro) },
                onNavigateToRecipes = {
                    navController.popBackStack<RecipeList>(inclusive = false)
                },
                onScalerClick = { navController.openScalerTab(scalerRecipeId) },
            )
        }
        composable<Pro> {
            ProScreen(
                onNavigateUp = { navController.navigateUp() },
                onTryAiImport = { navController.navigate(Editor()) },
                onNavigateToSettings = { navController.navigate(Settings) },
            )
        }
    }
}

/**
 * True when the destination is one of the v2 top-level tab roots — the routes
 * that render the shared [V2BottomNav]: Recipes, the Scaler tab (either the
 * [ScalerHome] empty state or a loaded [Scale] entry) and Settings.
 */
private fun NavDestination.isV2Tab() =
    hasRoute<RecipeList>() || hasRoute<ScalerHome>() ||
        hasRoute<Scale>() || hasRoute<Settings>()

/**
 * v2 Scaler tab behaviour: the tab shows the recipe on the scale. When a
 * [recipeId] is loaded we pop back to its Scale entry if it's still on the
 * stack (a no-op when already there), otherwise we push it above the recipe
 * list so the back stack reads list → scale. With nothing loaded, the tab's
 * root is the [ScalerHome] empty state instead.
 */
private fun NavController.openScalerTab(recipeId: Long?) {
    if (recipeId != null) {
        if (!popBackStack<Scale>(inclusive = false)) {
            popBackStack<RecipeList>(inclusive = false)
            navigate(Scale(recipeId))
        }
    } else if (!popBackStack<ScalerHome>(inclusive = false)) {
        popBackStack<RecipeList>(inclusive = false)
        navigate(ScalerHome)
    }
}
