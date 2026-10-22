package com.ryxan.recipescaler.feature.pro

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ryxan.recipescaler.R
import com.ryxan.recipescaler.core.data.billing.ProPlan
import com.ryxan.recipescaler.core.data.billing.ProProduct
import com.ryxan.recipescaler.core.data.billing.ProProductIds
import com.ryxan.recipescaler.core.designsystem.Spacing
import com.ryxan.recipescaler.core.designsystem.V2Card
import com.ryxan.recipescaler.core.designsystem.V2TopAppBar
import com.ryxan.recipescaler.core.domain.importer.AiImportConfig
import kotlinx.coroutines.delay

/**
 * Culinary Material (v2) Pro/paywall — same purchase flow as [ProContent]
 * (plan selection, restore, pending/verifying, post-purchase state), restyled
 * with v2 cards, icon badges and pill buttons instead of gradients.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProV2Content(
    state: ProUiState,
    onPurchase: (String) -> Unit = {},
    onRestore: () -> Unit = {},
    onRetry: () -> Unit = {},
    onNavigateUp: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onTryAiImport: () -> Unit = {},
    onBackToRecipes: () -> Unit = {},
    onManageSubscription: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    // Only Rescale's own Pro plans belong on this screen. The AI scan pack is
    // also an in-app product and ends up in `products`, so filter by id.
    // Lifetime is listed (and pre-selected) first.
    val plans = remember(state.products) {
        listOfNotNull(
            state.products.firstOrNull { it.id == ProProductIds.LIFETIME },
            state.products.firstOrNull { it.id == ProProductIds.MONTHLY },
        )
    }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = plans.firstOrNull { it.id == selectedId } ?: plans.firstOrNull()
    val canBuy = !state.isPro && !state.isPending && !state.loading &&
        !state.verifying && selected != null

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            V2TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        stringResource(R.string.title_pro),
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
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.title_settings),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (!state.isPro) {
                PurchaseBarV2(
                    selected = selected.takeIf { canBuy },
                    verifying = state.verifying,
                    onPurchase = onPurchase,
                    onRestore = onRestore,
                )
            }
        },
    ) { innerPadding ->
        if (state.isPro) {
            ProActiveV2Content(
                state = state,
                onTryAiImport = onTryAiImport,
                onBackToRecipes = onBackToRecipes,
                onManageSubscription = onManageSubscription,
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.l),
                verticalArrangement = Arrangement.spacedBy(Spacing.l),
            ) {
                HeroV2(isPro = state.isPro)

                state.freeScansLeft?.let { UsageMeterV2(scansLeft = it) }

                BenefitsCardV2(isPro = state.isPro)

                when {
                    state.isPending -> PendingCardV2()
                    state.loading -> PlanSkeletonV2()
                    plans.isEmpty() -> UnavailableV2(onRetry = onRetry)
                    else -> Column(
                        modifier = Modifier.selectableGroup(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.m),
                    ) {
                        plans.forEach { product ->
                            PlanCardV2(
                                product = product,
                                selected = product.id == selected?.id,
                                onSelect = { selectedId = product.id },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Sections ────────────────────────────────────────────────────────────────

@Composable
private fun HeroV2(isPro: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        V2IconBadge(
            icon = if (isPro) Icons.Filled.WorkspacePremium else Icons.Filled.AutoAwesome,
            containerColor = MaterialTheme.colorScheme.primaryFixed,
            contentColor = MaterialTheme.colorScheme.onPrimaryFixedVariant,
            size = 64.dp,
            iconSize = 32.dp,
        )
        Text(
            text = stringResource(
                if (isPro) R.string.pro_hero_title_active else R.string.title_pro,
            ),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(
                if (isPro) R.string.pro_hero_subtitle_active else R.string.pro_hero_subtitle,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun UsageMeterV2(scansLeft: Int) {
    V2Card(
        Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            Text(
                text = if (scansLeft > 0) {
                    pluralStringResource(R.plurals.pro_scans_left, scansLeft, scansLeft)
                } else {
                    stringResource(R.string.pro_scans_none)
                },
                style = MaterialTheme.typography.titleSmall,
            )
            LinearProgressIndicator(
                progress = {
                    (scansLeft.toFloat() / AiImportConfig.FREE_LIFETIME_LIMIT).coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BenefitsCardV2(isPro: Boolean) {
    V2Card(
        Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            BenefitRowV2(Icons.Filled.AutoAwesome, R.string.pro_benefit_ai, isPro)
            BenefitRowV2(Icons.Filled.Draw, R.string.pro_benefit_handwriting, isPro)
        }
    }
}

@Composable
private fun BenefitRowV2(icon: ImageVector, textRes: Int, checked: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (checked) Icons.Filled.CheckCircle else icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = Spacing.m),
        )
    }
}

@Composable
private fun PlanCardV2(
    product: ProProduct,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (selected) {
            scheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            scheme.surfaceContainerLow
        },
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) scheme.primary else scheme.outlineVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.m),
            ) {
                Text(
                    text = stringResource(
                        if (product.isSubscription) R.string.pro_plan_monthly else R.string.pro_plan_lifetime,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(
                        if (product.isSubscription) {
                            R.string.pro_plan_monthly_detail
                        } else {
                            R.string.pro_plan_lifetime_detail
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (product.isSubscription) {
                        stringResource(R.string.pro_price_monthly, product.price)
                    } else {
                        product.price
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                if (!product.isSubscription) {
                    Text(
                        text = stringResource(R.string.ai_paywall_best_value),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingCardV2() {
    V2Card(
        Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.pro_pending),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.pro_pending_detail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlanSkeletonV2() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        repeat(2) {
            V2Card(
                Modifier.fillMaxWidth().height(72.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {}
        }
    }
}

@Composable
private fun UnavailableV2(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.pro_unavailable),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}

/** Pinned bottom bar: one primary action, its terms, and a quiet Restore. */
@Composable
private fun PurchaseBarV2(
    selected: ProProduct?,
    verifying: Boolean,
    onPurchase: (String) -> Unit,
    onRestore: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.l, vertical = Spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (verifying) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp),
                    )
                    Text(
                        text = stringResource(R.string.pro_verifying),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (selected != null) {
                Button(
                    onClick = { onPurchase(selected.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                ) {
                    Text(
                        text = if (selected.isSubscription) {
                            stringResource(R.string.pro_cta_monthly, selected.price)
                        } else {
                            stringResource(R.string.pro_cta_lifetime, selected.price)
                        },
                    )
                }
                Text(
                    text = stringResource(
                        if (selected.isSubscription) R.string.pro_fine_monthly else R.string.pro_fine_lifetime,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
            TextButton(onClick = onRestore) {
                Text(stringResource(R.string.action_restore_purchases))
            }
        }
    }
}

// ── Post-purchase ───────────────────────────────────────────────────────────

@Composable
private fun ProActiveV2Content(
    state: ProUiState,
    onTryAiImport: () -> Unit,
    onBackToRecipes: () -> Unit,
    onManageSubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        RevealV2(delayMillis = 0) {
            ProHeroV2()
        }
        RevealV2(delayMillis = 90) {
            OwnedPlanCardV2(plan = state.plan, onManageSubscription = onManageSubscription)
        }
        RevealV2(delayMillis = 180) {
            UnlockedCardV2()
        }
        RevealV2(delayMillis = 270) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Button(
                    onClick = onTryAiImport,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = Icons.Filled.DocumentScanner,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(Spacing.s))
                    Text(text = stringResource(R.string.pro_cta_scan_first))
                }
                OutlinedButton(
                    onClick = onBackToRecipes,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                ) {
                    Text(text = stringResource(R.string.pro_back_to_recipes))
                }
            }
        }
    }
}

@Composable
private fun ProHeroV2() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(Spacing.s))
        V2IconBadge(
            icon = Icons.Filled.Check,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            size = 88.dp,
            iconSize = 48.dp,
        )
        Spacer(modifier = Modifier.height(Spacing.l))
        Text(
            text = stringResource(R.string.pro_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

/** Which plan was bought, and — for monthly — where to manage it. */
@Composable
private fun OwnedPlanCardV2(
    plan: ProPlan?,
    onManageSubscription: () -> Unit,
) {
    if (plan == null) return
    V2Card(
        Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            V2IconBadge(
                icon = Icons.Filled.WorkspacePremium,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Text(
                    text = stringResource(
                        if (plan == ProPlan.Monthly) {
                            R.string.pro_plan_monthly
                        } else {
                            R.string.pro_plan_lifetime
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(
                        if (plan == ProPlan.Monthly) {
                            R.string.pro_plan_monthly_detail
                        } else {
                            R.string.pro_plan_lifetime_detail
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (plan == ProPlan.Monthly) {
                TextButton(onClick = onManageSubscription) {
                    Text(text = stringResource(R.string.pro_manage_subscription))
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun UnlockedCardV2() {
    V2Card(
        Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Text(
                text = stringResource(R.string.pro_unlocked_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            UnlockedBenefitRowV2(
                icon = Icons.Filled.AutoAwesome,
                title = stringResource(R.string.pro_benefit_ai),
            )
            UnlockedBenefitRowV2(
                icon = Icons.Filled.Draw,
                title = stringResource(R.string.pro_benefit_handwriting),
            )
            UnlockedBenefitRowV2(
                icon = Icons.Filled.Block,
                title = stringResource(R.string.pro_benefit_no_ads),
            )
        }
    }
}

@Composable
private fun UnlockedBenefitRowV2(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        V2IconBadge(
            icon = icon,
            containerColor = MaterialTheme.colorScheme.primaryFixed,
            contentColor = MaterialTheme.colorScheme.onPrimaryFixedVariant,
        )
        Text(
            text = title,
            modifier = Modifier.padding(start = 14.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun V2IconBadge(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    iconSize: androidx.compose.ui.unit.Dp = 22.dp,
) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

/** Staggered entrance: fades + slides content in after [delayMillis]. */
@Composable
private fun RevealV2(delayMillis: Int, content: @Composable () -> Unit) {
    var visible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!visible) {
            delay(delayMillis.toLong())
            visible = true
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "pro_reveal_v2",
    )
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 24.dp.toPx()
        },
    ) {
        content()
    }
}
