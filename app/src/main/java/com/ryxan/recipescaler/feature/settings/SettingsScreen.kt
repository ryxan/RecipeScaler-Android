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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ryxan.recipescaler.BuildConfig
import com.ryxan.recipescaler.R
import com.ryxan.recipescaler.core.designsystem.GradientTopAppBar
import com.ryxan.recipescaler.core.designsystem.RescaleCard
import com.ryxan.recipescaler.core.designsystem.RescaleTheme
import com.ryxan.recipescaler.core.designsystem.SectionHeader
import com.ryxan.recipescaler.core.designsystem.Spacing
import com.ryxan.recipescaler.core.domain.formatting.DisplayMode
import com.ryxan.recipescaler.core.domain.importer.AiImportConfig
import com.ryxan.recipescaler.core.domain.formatting.MeasurementSystem
import com.ryxan.recipescaler.core.domain.model.AppSettings
import com.ryxan.recipescaler.core.domain.model.ThemeMode

@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToPro: () -> Unit = {},
    onNavigateToRecipes: () -> Unit = {},
    onScalerClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val debugPro by viewModel.debugPro.collectAsStateWithLifecycle()
    val aiScansRemaining by viewModel.aiScansRemaining.collectAsStateWithLifecycle()
    if (RescaleTheme.uiV2) {
        SettingsV2Content(
            settings = settings,
            onSystem = viewModel::setMeasurementSystem,
            onDisplayMode = viewModel::setDisplayMode,
            onThemeMode = viewModel::setThemeMode,
            onDynamicColor = viewModel::setDynamicColor,
            onUiV2 = viewModel::setUiV2,
            onShowScaleWarning = viewModel::setShowScaleWarning,
            showDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            versionName = BuildConfig.VERSION_NAME,
            onNavigateUp = onNavigateUp,
            isPro = isPro,
            onProClick = onNavigateToPro,
            onNavigateToRecipes = onNavigateToRecipes,
            onScalerClick = onScalerClick,
            showDebug = true, // TODO: restore BuildConfig.DEBUG before closed testing / production
            debugPro = debugPro,
            onDebugPro = viewModel::setDebugPro,
            aiScansRemaining = aiScansRemaining,
            onSetAiScans = viewModel::setAiScans,
        )
        return
    }
    SettingsContent(
        settings = settings,
        onSystem = viewModel::setMeasurementSystem,
        onDisplayMode = viewModel::setDisplayMode,
        onThemeMode = viewModel::setThemeMode,
        onDynamicColor = viewModel::setDynamicColor,
        onUiV2 = viewModel::setUiV2,
        onShowScaleWarning = viewModel::setShowScaleWarning,
        showDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        versionName = BuildConfig.VERSION_NAME,
        onNavigateUp = onNavigateUp,
        isPro = isPro,
        onProClick = onNavigateToPro,
        showDebug = true, // TODO: restore BuildConfig.DEBUG before closed testing / production
        debugPro = debugPro,
        onDebugPro = viewModel::setDebugPro,
        aiScansRemaining = aiScansRemaining,
        onSetAiScans = viewModel::setAiScans,
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
    onUiV2: (Boolean) -> Unit = {},
    onShowScaleWarning: (Boolean) -> Unit = {},
    showDynamicColor: Boolean,
    versionName: String,
    onNavigateUp: () -> Unit = {},
    isPro: Boolean = false,
    onProClick: () -> Unit = {},
    showDebug: Boolean = false,
    debugPro: Boolean = false,
    onDebugPro: (Boolean) -> Unit = {},
    aiScansRemaining: Int = 0,
    onSetAiScans: (Int) -> Unit = {},
) {
    Scaffold(
        topBar = {
            GradientTopAppBar(
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
            RescaleCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.l),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable(onClick = onProClick)
                        .padding(horizontal = Spacing.l),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.WorkspacePremium,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = Spacing.l),
                    ) {
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
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SectionHeader(stringResource(R.string.settings_section_measurement))
            SettingsCard {
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
            }

            SectionHeader(stringResource(R.string.settings_section_display))
            SettingsCard {
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { onShowScaleWarning(!settings.showScaleWarning) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_scale_warning),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = settings.showScaleWarning,
                        onCheckedChange = onShowScaleWarning,
                    )
                }
            }

            SectionHeader(stringResource(R.string.settings_section_theme))
            SettingsCard {
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { onUiV2(!settings.uiV2) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_ui_v2),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = settings.uiV2,
                        onCheckedChange = onUiV2,
                    )
                }
            }

            if (showDebug) {
                SectionHeader(stringResource(R.string.settings_section_debug))
                SettingsCard {
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
                    Text(
                        text = stringResource(R.string.settings_debug_ai_scans, aiScansRemaining),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        listOf(0, 1, 5, 10).forEach { preset ->
                            TextButton(onClick = { onSetAiScans(preset) }) {
                                Text("$preset")
                            }
                        }
                        TextButton(onClick = { onSetAiScans(AiImportConfig.FREE_LIFETIME_LIMIT) }) {
                            Text(stringResource(R.string.settings_debug_ai_scans_reset))
                        }
                    }
                }
            }

            SectionHeader(stringResource(R.string.settings_section_about))
            SettingsCard {
                val uriHandler = LocalUriHandler.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { uriHandler.openUri(PRIVACY_POLICY_URL) }
                        .padding(horizontal = Spacing.l),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_privacy),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.settings_version, versionName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s),
                )
            }
        }
    }
}

internal const val PRIVACY_POLICY_URL = "https://ryxan.ca/rescale-privacy.html"

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    RescaleCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.l),
    ) {
        content()
    }
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
