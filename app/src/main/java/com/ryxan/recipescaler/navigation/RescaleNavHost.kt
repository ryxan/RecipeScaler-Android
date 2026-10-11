package com.ryxan.recipescaler.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ryxan.recipescaler.feature.editor.EditorScreen
import com.ryxan.recipescaler.feature.recipes.RecipeListScreen
import com.ryxan.recipescaler.feature.scale.ScaleScreen
import com.ryxan.recipescaler.feature.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
object RecipeList

@Serializable
object Settings

/** [recipeId] 0 means a new recipe. */
@Serializable
data class Editor(val recipeId: Long = 0L)

@Serializable
data class Scale(val recipeId: Long)

@Composable
fun RescaleNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = RecipeList,
    ) {
        composable<RecipeList> {
            RecipeListScreen(
                onOpenRecipe = { id -> navController.navigate(Editor(id)) },
                onNewRecipe = { navController.navigate(Editor()) },
                onNavigateToSettings = { navController.navigate(Settings) },
            )
        }
        composable<Editor> {
            EditorScreen(onNavigateUp = { navController.navigateUp() })
        }
        composable<Scale> {
            ScaleScreen(onNavigateUp = { navController.navigateUp() })
        }
        composable<Settings> {
            SettingsScreen(onNavigateUp = { navController.navigateUp() })
        }
    }
}
