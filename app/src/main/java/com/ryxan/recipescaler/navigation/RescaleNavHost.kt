package com.ryxan.recipescaler.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ryxan.recipescaler.feature.recipes.RecipeListScreen
import com.ryxan.recipescaler.feature.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
object RecipeList

@Serializable
object Settings

@Composable
fun RescaleNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = RecipeList,
    ) {
        composable<RecipeList> {
            RecipeListScreen(
                onNavigateToSettings = { navController.navigate(Settings) },
            )
        }
        composable<Settings> {
            SettingsScreen()
        }
    }
}
