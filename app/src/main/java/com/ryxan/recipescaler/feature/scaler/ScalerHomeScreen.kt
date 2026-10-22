package com.ryxan.recipescaler.feature.scaler

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
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ryxan.recipescaler.R
import com.ryxan.recipescaler.core.designsystem.Spacing
import com.ryxan.recipescaler.core.designsystem.V2Card
import com.ryxan.recipescaler.navigation.V2BottomNav
import com.ryxan.recipescaler.navigation.V2Tab

/**
 * Scaler tab root (v2) — the empty state shown when no recipe is on the scale:
 * a hero card with the "scale dial" and the two entry actions (browse saved
 * recipes, scan & import).
 */
@Composable
fun ScalerHomeScreen(
    onBrowseRecipes: () -> Unit,
    onScanImport: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            V2BottomNav(
                selected = V2Tab.SCALER,
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
            HeroEmptyCard(
                onBrowseRecipes = onBrowseRecipes,
                onScanImport = onScanImport,
            )
        }
    }
}

/** Hero card: dashed-ring "scale dial", empty copy and the two entry actions. */
@Composable
private fun HeroEmptyCard(
    onBrowseRecipes: () -> Unit,
    onScanImport: () -> Unit,
) {
    V2Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ScaleDial()
            Text(
                text = stringResource(R.string.v2_scaler_empty_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
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
                Button(
                    onClick = onBrowseRecipes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = CircleShape,
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
                    shape = CircleShape,
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

/** Decorative "0.0 g" dial: warm circle with a dashed platter ring. */
@Composable
private fun ScaleDial() {
    val platter = MaterialTheme.colorScheme.surfaceContainer
    val ring = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = Modifier
            .size(128.dp)
            .clip(CircleShape)
            // Decorative illustration — "0.0 g" would be read out as content.
            .clearAndSetSemantics {}
            .drawBehind {
                drawCircle(color = platter)
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .drawBehind {
                    drawCircle(
                        color = ring,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
                            ),
                        ),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.Scale,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(R.string.v2_scale_dial_zero),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
