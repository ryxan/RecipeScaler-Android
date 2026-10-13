package com.ryxan.recipescaler.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ryxan.recipescaler.core.designsystem.RescaleMotion
import com.ryxan.recipescaler.feature.editor.EditorScreen
import com.ryxan.recipescaler.feature.pro.ProScreen
import com.ryxan.recipescaler.feature.recipes.RecipeListScreen
import com.ryxan.recipescaler.feature.scale.ScaleScreen
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

@Serializable
object Pro

@Composable
fun RescaleNavHost() {
    val navController = rememberNavController()
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
        composable<RecipeList> {
            RecipeListScreen(
                onOpenRecipe = { id -> navController.navigate(Editor(recipeId = id)) },
                onNewRecipe = { navController.navigate(Editor()) },
                onQuickScale = { title -> navController.navigate(Editor(initialTitle = title)) },
                onNavigateToSettings = { navController.navigate(Settings) },
            )
        }
        composable<Editor> {
            EditorScreen(
                onNavigateUp = { navController.navigateUp() },
                onScale = { id -> navController.navigate(Scale(id)) },
                onNavigateToPro = { navController.navigate(Pro) },
            )
        }
        composable<Scale> {
            ScaleScreen(onNavigateUp = { navController.navigateUp() })
        }
        composable<Settings> {
            SettingsScreen(
                onNavigateUp = { navController.navigateUp() },
                onNavigateToPro = { navController.navigate(Pro) },
            )
        }
        composable<Pro> {
            ProScreen(onNavigateUp = { navController.navigateUp() })
        }
    }
}
