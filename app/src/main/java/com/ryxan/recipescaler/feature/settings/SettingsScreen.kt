package com.ryxan.recipescaler.feature.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ryxan.recipescaler.BuildConfig
import com.ryxan.recipescaler.R
import com.ryxan.recipescaler.core.domain.formatting.DisplayMode
import com.ryxan.recipescaler.core.domain.formatting.MeasurementSystem
import com.ryxan.recipescaler.core.domain.model.AppSettings
import com.ryxan.recipescaler.core.domain.model.ThemeMode

@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToPro: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val debugPro by viewModel.debugPro.collectAsStateWithLifecycle()
    SettingsContent(
        settings = settings,
        onSystem = viewModel::setMeasurementSystem,
        onDisplayMode = viewModel::setDisplayMode,
        onThemeMode = viewModel::setThemeMode,
        onDynamicColor = viewModel::setDynamicColor,
        showDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        versionName = BuildConfig.VERSION_NAME,
        onNavigateUp = onNavigateUp,
        isPro = isPro,
        onProClick = onNavigateToPro,
        showDebug = BuildConfig.DEBUG,
        debugPro = debugPro,
        onDebugPro = viewModel::setDebugPro,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    settings: AppSettings,
    onSystem: (MeasurementSystem) -> Unit,
    onDisplayMode: (DisplayMode) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    showDynamicColor: Boolean,
    versionName: String,
    onNavigateUp: () -> Unit = {},
    isPro: Boolean = false,
    onProClick: () -> Unit = {},
    showDebug: Boolean = false,
    debugPro: Boolean = false,
    onDebugPro: (Boolean) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable(onClick = onProClick)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.title_pro),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (isPro) {
                        Text(
                            text = stringResource(R.string.pro_state_active),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            SectionHeader(stringResource(R.string.settings_section_measurement))
            Column(Modifier.selectableGroup()) {
                RadioRow(
                    text = stringResource(R.string.settings_system_us),
                    selected = settings.display.system == MeasurementSystem.US,
                    onClick = { onSystem(MeasurementSystem.US) },
                )
                RadioRow(
                    text = stringResource(R.string.settings_system_metric),
                    selected = settings.display.system == MeasurementSystem.METRIC,
                    onClick = { onSystem(MeasurementSystem.METRIC) },
                )
                RadioRow(
                    text = stringResource(R.string.settings_system_match),
                    selected = settings.display.system == MeasurementSystem.MATCH_RECIPE,
                    onClick = { onSystem(MeasurementSystem.MATCH_RECIPE) },
                )
            }

            SectionHeader(stringResource(R.string.settings_section_display))
            Column(Modifier.selectableGroup()) {
                RadioRow(
                    text = stringResource(R.string.settings_display_kitchen),
                    selected = settings.display.mode == DisplayMode.KITCHEN,
                    onClick = { onDisplayMode(DisplayMode.KITCHEN) },
                )
                RadioRow(
                    text = stringResource(R.string.settings_display_keep_units),
                    selected = settings.display.mode == DisplayMode.KEEP_UNITS,
                    onClick = { onDisplayMode(DisplayMode.KEEP_UNITS) },
                )
                RadioRow(
                    text = stringResource(R.string.settings_display_decimal),
                    selected = settings.display.mode == DisplayMode.DECIMAL,
                    onClick = { onDisplayMode(DisplayMode.DECIMAL) },
                )
            }

            SectionHeader(stringResource(R.string.settings_section_theme))
            Column(Modifier.selectableGroup()) {
                RadioRow(
                    text = stringResource(R.string.settings_theme_system),
                    selected = settings.themeMode == ThemeMode.SYSTEM,
                    onClick = { onThemeMode(ThemeMode.SYSTEM) },
                )
                RadioRow(
                    text = stringResource(R.string.settings_theme_light),
                    selected = settings.themeMode == ThemeMode.LIGHT,
                    onClick = { onThemeMode(ThemeMode.LIGHT) },
                )
                RadioRow(
                    text = stringResource(R.string.settings_theme_dark),
                    selected = settings.themeMode == ThemeMode.DARK,
                    onClick = { onThemeMode(ThemeMode.DARK) },
                )
            }

            if (showDynamicColor) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { onDynamicColor(!settings.dynamicColor) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_dynamic_color),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = settings.dynamicColor,
                        onCheckedChange = onDynamicColor,
                    )
                }
            }

            if (showDebug) {
                SectionHeader(stringResource(R.string.settings_section_debug))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { onDebugPro(!debugPro) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_debug_pro),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = debugPro,
                        onCheckedChange = onDebugPro,
                    )
                }
            }

            SectionHeader(stringResource(R.string.settings_section_about))
            Text(
                text = stringResource(R.string.settings_version, versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun RadioRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
