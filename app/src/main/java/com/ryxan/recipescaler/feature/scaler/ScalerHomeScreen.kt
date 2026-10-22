package com.ryxan.recipescaler.feature.scaler

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ryxan.recipescaler.R
import com.ryxan.recipescaler.core.designsystem.GradientButton
import com.ryxan.recipescaler.core.designsystem.GradientTopAppBar
import com.ryxan.recipescaler.core.designsystem.RescaleCard
import com.ryxan.recipescaler.core.designsystem.RescaleTheme
import com.ryxan.recipescaler.core.designsystem.Spacing
import com.ryxan.recipescaler.navigation.RescaleBottomNav
import com.ryxan.recipescaler.navigation.RescaleTab

/**
 * Scaler tab root — the empty state shown when no recipe is on the scale:
 * a hero card with the "scale dial" and the two entry actions (browse saved
 * recipes, scan & import).
 */
@Composable
fun ScalerHomeScreen(
    onBrowseRecipes: () -> Unit,
    onScanImport: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    if (RescaleTheme.uiV2) {
        ScalerHomeV2Content(
            onBrowseRecipes = onBrowseRecipes,
            onScanImport = onScanImport,
            onNavigateToSettings = onNavigateToSettings,
        )
        return
    }
    ScalerHomeContent(
        onBrowseRecipes = onBrowseRecipes,
        onScanImport = onScanImport,
        onNavigateToSettings = onNavigateToSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScalerHomeContent(
    onBrowseRecipes: () -> Unit,
    onScanImport: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.nav_scaler)) },
            )
        },
        bottomBar = {
            RescaleBottomNav(
                selected = RescaleTab.SCALER,
                onRecipesClick = onBrowseRecipes,
                onScalerClick = {},
                onSettingsClick = onNavigateToSettings,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Spacing.l),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RescaleCard(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ScaleDial()
                    Text(
                        text = stringResource(R.string.v2_scaler_empty_title),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.v2_scaler_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = Spacing.s),
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.l),
                        verticalArrangement = Arrangement.spacedBy(Spacing.s),
                    ) {
                        GradientButton(
                            onClick = onBrowseRecipes,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(Spacing.s))
                            Text(
                                stringResource(R.string.v2_scaler_browse),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        FilledTonalButton(
                            onClick = onScanImport,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                        ) {
                            Icon(
                                Icons.Outlined.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(Spacing.s))
                            Text(
                                stringResource(R.string.v2_scaler_scan),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.size(Spacing.s))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.secondary,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(
                                        horizontal = 6.dp,
                                        vertical = 2.dp,
                                    ),
                                ) {
                                    Icon(
                                        Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp),
                                    )
                                    Text(
                                        text = stringResource(R.string.v2_scaler_ocr_badge),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(start = 2.dp),
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

/** Decorative "0.0 g" dial: brand-gradient circle, matching the v1 empty-state icon. */
@Composable
private fun ScaleDial() {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            // Decorative illustration — "0.0 g" would be read out as content.
            .clearAndSetSemantics {}
            .background(RescaleTheme.brandGradient),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Scale,
                contentDescription = null,
                tint = RescaleTheme.extendedColors.onGradient,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = stringResource(R.string.v2_scale_dial_zero),
                style = MaterialTheme.typography.headlineSmall,
                color = RescaleTheme.extendedColors.onGradient,
            )
        }
    }
}
