package com.ryxan.recipescaler.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ryxan.recipescaler.R
import com.ryxan.recipescaler.core.designsystem.Spacing
import com.ryxan.recipescaler.core.designsystem.V2Card
import com.ryxan.recipescaler.core.designsystem.V2SectionHeader
import com.ryxan.recipescaler.core.designsystem.V2TopAppBar
import com.ryxan.recipescaler.core.domain.formatting.DisplayMode
import com.ryxan.recipescaler.core.domain.importer.AiImportConfig
import com.ryxan.recipescaler.core.domain.formatting.MeasurementSystem
import com.ryxan.recipescaler.core.domain.model.AppSettings
import com.ryxan.recipescaler.core.domain.model.ThemeMode
import com.ryxan.recipescaler.navigation.V2BottomNav
import com.ryxan.recipescaler.navigation.V2Tab

/**
 * Culinary Material (v2) settings — same preferences as [SettingsContent],
 * restyled after the Stitch mock: icon-led rows and full-width option rows
 * (selected option filled with the primary colour) grouped in v2 cards under
 * uppercase section labels.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsV2Content(
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
    onNavigateToRecipes: () -> Unit = {},
    onScalerClick: () -> Unit = {},
    isPro: Boolean = false,
    onProClick: () -> Unit = {},
    showDebug: Boolean = false,
    debugPro: Boolean = false,
    onDebugPro: (Boolean) -> Unit = {},
    aiScansRemaining: Int = 0,
    onSetAiScans: (Int) -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            V2TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        stringResource(R.string.title_settings),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
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
        bottomBar = {
            V2BottomNav(
                selected = V2Tab.SETTINGS,
                onRecipesClick = onNavigateToRecipes,
                onScalerClick = onScalerClick,
                onSettingsClick = {},
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.xl),
        ) {
            V2SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .clickable(onClick = onProClick)
                        .padding(horizontal = Spacing.l, vertical = Spacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconTile {
                        Icon(
                            imageVector = Icons.Filled.WorkspacePremium,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = Spacing.m),
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

            V2SectionHeader(stringResource(R.string.settings_section_measurement))
            V2SettingsCard {
                Column(
                    Modifier
                        .selectableGroup()
                        .padding(Spacing.m),
                ) {
                    V2OptionRow(
                        text = stringResource(R.string.settings_system_us),
                        selected = settings.display.system == MeasurementSystem.US,
                        onClick = { onSystem(MeasurementSystem.US) },
                    )
                    V2OptionRow(
                        text = stringResource(R.string.settings_system_metric),
                        selected = settings.display.system == MeasurementSystem.METRIC,
                        onClick = { onSystem(MeasurementSystem.METRIC) },
                    )
                    V2OptionRow(
                        text = stringResource(R.string.settings_system_match),
                        selected = settings.display.system == MeasurementSystem.MATCH_RECIPE,
                        onClick = { onSystem(MeasurementSystem.MATCH_RECIPE) },
                    )
                }
            }

            V2SectionHeader(stringResource(R.string.settings_section_display))
            V2SettingsCard {
                Column(
                    Modifier
                        .selectableGroup()
                        .padding(Spacing.m),
                ) {
                    V2OptionRow(
                        text = stringResource(R.string.settings_display_kitchen),
                        selected = settings.display.mode == DisplayMode.KITCHEN,
                        onClick = { onDisplayMode(DisplayMode.KITCHEN) },
                    )
                    V2OptionRow(
                        text = stringResource(R.string.settings_display_keep_units),
                        selected = settings.display.mode == DisplayMode.KEEP_UNITS,
                        onClick = { onDisplayMode(DisplayMode.KEEP_UNITS) },
                    )
                    V2OptionRow(
                        text = stringResource(R.string.settings_display_decimal),
                        selected = settings.display.mode == DisplayMode.DECIMAL,
                        onClick = { onDisplayMode(DisplayMode.DECIMAL) },
                    )
                }
                V2SwitchRow(
                    text = stringResource(R.string.settings_scale_warning),
                    checked = settings.showScaleWarning,
                    onCheckedChange = onShowScaleWarning,
                )
            }

            V2SectionHeader(stringResource(R.string.settings_section_theme))
            V2SettingsCard {
                Column(
                    Modifier
                        .selectableGroup()
                        .padding(Spacing.m),
                ) {
                    V2OptionRow(
                        text = stringResource(R.string.settings_theme_system),
                        selected = settings.themeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeMode(ThemeMode.SYSTEM) },
                    )
                    V2OptionRow(
                        text = stringResource(R.string.settings_theme_light),
                        selected = settings.themeMode == ThemeMode.LIGHT,
                        onClick = { onThemeMode(ThemeMode.LIGHT) },
                    )
                    V2OptionRow(
                        text = stringResource(R.string.settings_theme_dark),
                        selected = settings.themeMode == ThemeMode.DARK,
                        onClick = { onThemeMode(ThemeMode.DARK) },
                    )
                }
                if (showDynamicColor) {
                    V2SwitchRow(
                        text = stringResource(R.string.settings_dynamic_color),
                        checked = settings.dynamicColor,
                        onCheckedChange = onDynamicColor,
                    )
                }
                V2SwitchRow(
                    text = stringResource(R.string.settings_ui_v2),
                    checked = settings.uiV2,
                    onCheckedChange = onUiV2,
                )
            }

            if (showDebug) {
                V2SectionHeader(stringResource(R.string.settings_section_debug))
                V2SettingsCard {
                    V2SwitchRow(
                        text = stringResource(R.string.settings_debug_pro),
                        checked = debugPro,
                        onCheckedChange = onDebugPro,
                    )
                    Text(
                        text = stringResource(
                            R.string.settings_debug_ai_scans,
                            aiScansRemaining,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = Spacing.l),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.l, vertical = Spacing.xs),
                    ) {
                        listOf(0, 1, 5, 10).forEach { preset ->
                            TextButton(onClick = { onSetAiScans(preset) }) {
                                Text("$preset")
                            }
                        }
                        TextButton(
                            onClick = { onSetAiScans(AiImportConfig.FREE_LIFETIME_LIMIT) },
                        ) {
                            Text(stringResource(R.string.settings_debug_ai_scans_reset))
                        }
                    }
                }
            }

            V2SectionHeader(stringResource(R.string.settings_section_about))
            V2SettingsCard {
                val uriHandler = LocalUriHandler.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable { uriHandler.openUri(PRIVACY_POLICY_URL) }
                        .padding(horizontal = Spacing.l, vertical = Spacing.m),
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
                    modifier = Modifier.padding(
                        horizontal = Spacing.l,
                        vertical = Spacing.s,
                    ),
                )
            }
        }
    }
}

/** Tinted square tile used as the leading affordance on v2 settings rows. */
@Composable
private fun IconTile(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryFixed,
        contentColor = MaterialTheme.colorScheme.onPrimaryFixedVariant,
        modifier = Modifier.size(40.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}

/**
 * Full-width selectable row — the mock's "filled = selected" option list.
 * Selected rows use the primary fill; unselected rows are hairline-outlined.
 */
@Composable
private fun V2OptionRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs)
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        shape = CircleShape,
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        border = if (selected) {
            null
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(Spacing.s))
            }
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** Label + switch row matching the mock's toggle rows. */
@Composable
private fun V2SwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = Spacing.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun V2SettingsCard(content: @Composable () -> Unit) {
    V2Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.l),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        content()
    }
}
