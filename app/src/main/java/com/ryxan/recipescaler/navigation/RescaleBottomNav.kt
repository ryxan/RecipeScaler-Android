package com.ryxan.recipescaler.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ryxan.recipescaler.R

/** The three top-level destinations reachable from the bottom bar. */
internal enum class RescaleTab { RECIPES, SCALER, SETTINGS }

/**
 * Shared bottom nav. Shown on the Recipes, Scaler (scale screen) and Settings
 * destinations — the bar persists across tabs rather than living only on the
 * list screen.
 */
@Composable
internal fun RescaleBottomNav(
    selected: RescaleTab,
    onRecipesClick: () -> Unit,
    onScalerClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
    ) {
        NavigationBarItem(
            selected = selected == RescaleTab.RECIPES,
            onClick = onRecipesClick,
            icon = {
                Icon(
                    if (selected == RescaleTab.RECIPES) {
                        Icons.AutoMirrored.Filled.MenuBook
                    } else {
                        Icons.AutoMirrored.Outlined.MenuBook
                    },
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.title_recipes)) },
        )
        NavigationBarItem(
            selected = selected == RescaleTab.SCALER,
            onClick = onScalerClick,
            icon = {
                Icon(
                    if (selected == RescaleTab.SCALER) {
                        Icons.Filled.Calculate
                    } else {
                        Icons.Outlined.Calculate
                    },
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.nav_scaler)) },
        )
        NavigationBarItem(
            selected = selected == RescaleTab.SETTINGS,
            onClick = onSettingsClick,
            icon = {
                Icon(
                    if (selected == RescaleTab.SETTINGS) {
                        Icons.Filled.Settings
                    } else {
                        Icons.Outlined.Settings
                    },
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.title_settings)) },
        )
    }
}
