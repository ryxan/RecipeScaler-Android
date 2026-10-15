package com.ryxan.recipescaler.feature.pro

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ryxan.recipescaler.R
import com.ryxan.recipescaler.core.data.billing.ProProduct
import com.ryxan.recipescaler.core.data.billing.ProProductIds
import com.ryxan.recipescaler.core.designsystem.GradientButton
import com.ryxan.recipescaler.core.designsystem.GradientTopAppBar
import com.ryxan.recipescaler.core.designsystem.RescaleCard
import com.ryxan.recipescaler.core.designsystem.Spacing
import com.ryxan.recipescaler.core.domain.importer.AiImportConfig
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import com.ryxan.recipescaler.core.data.billing.ProPlan
import com.ryxan.recipescaler.core.designsystem.RescaleTheme
import kotlinx.coroutines.delay

/** Google Play page where a subscriber manages (or cancels) their subscription. */
private const val PLAY_SUBSCRIPTIONS_URL = "https://play.google.com/store/account/subscriptions"

@Composable
fun ProScreen(
    onNavigateUp: () -> Unit,
    onTryAiImport: () -> Unit = {},
    viewModel: ProViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    ProContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateUp = onNavigateUp,
        onPurchase = { productId ->
            (context as? Activity)?.let { viewModel.purchase(it, productId) }
        },
        onRestore = viewModel::restore,
        onRetry = viewModel::retry,
        onTryAiImport = onTryAiImport,
        onBackToRecipes = onNavigateUp,
        onManageSubscription = { uriHandler.openUri(PLAY_SUBSCRIPTIONS_URL) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProContent(
    state: ProUiState,
    onPurchase: (String) -> Unit = {},
    onRestore: () -> Unit = {},
    onRetry: () -> Unit = {},
    onNavigateUp: () -> Unit = {},
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.title_pro)) },
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
            if (!state.isPro) {
                PurchaseBar(
                    selected = selected.takeIf { canBuy },
                    verifying = state.verifying,
                    onPurchase = onPurchase,
                    onRestore = onRestore,
                )
            }
        },
    ) { innerPadding ->
        if (state.isPro) {
            ProActiveContent(
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
                Hero(isPro = state.isPro)

                state.freeScansLeft?.let { UsageMeter(scansLeft = it) }

                BenefitsCard(isPro = state.isPro)

                when {
                    state.isPending -> PendingCard()
                    state.loading -> PlanSkeleton()
                    plans.isEmpty() -> Unavailable(onRetry = onRetry)
                    else -> Column(
                        modifier = Modifier.selectableGroup(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.m),
                    ) {
                        plans.forEach { product ->
                            PlanCard(
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
private fun Hero(isPro: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPro) Icons.Filled.WorkspacePremium else Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
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
private fun UsageMeter(scansLeft: Int) {
    RescaleCard(Modifier.fillMaxWidth()) {
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
private fun BenefitsCard(isPro: Boolean) {
    RescaleCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            BenefitRow(Icons.Filled.AutoAwesome, R.string.pro_benefit_ai, isPro)
            BenefitRow(Icons.Filled.Draw, R.string.pro_benefit_handwriting, isPro)
        }
    }
}

@Composable
private fun BenefitRow(icon: ImageVector, textRes: Int, checked: Boolean) {
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
private fun PlanCard(
    product: ProProduct,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        shape = MaterialTheme.shapes.large,
        color = if (selected) scheme.primaryContainer.copy(alpha = 0.35f) else scheme.surfaceContainerLow,
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
private fun PendingCard() {
    RescaleCard(Modifier.fillMaxWidth()) {
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
private fun PlanSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        repeat(2) { RescaleCard(Modifier.fillMaxWidth().height(72.dp)) {} }
    }
}

@Composable
private fun Unavailable(onRetry: () -> Unit) {
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
private fun PurchaseBar(
    selected: ProProduct?,
    verifying: Boolean,
    onPurchase: (String) -> Unit,
    onRestore: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                    GradientButton(
                        onClick = { onPurchase(selected.id) },
                        modifier = Modifier.fillMaxWidth(),
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
}


// ── Post-purchase: the Pro user just paid — celebrate, confirm, direct ──────

/**
 * The page after buying Pro. The moment right after a purchase is when
 * buyer's remorse peaks, so this state does three jobs the old one-line
 * "You're Pro — thank you!" didn't: it celebrates the purchase, it shows
 * exactly what was bought and what it unlocks, and it gives one obvious
 * next step (use the headline feature) instead of leaving the user
 * wondering where to go.
 */
private fun ProActiveContent(
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Reveal(delayMillis = 0) {
            ProHero()
        }
        Reveal(delayMillis = 90) {
            OwnedPlanCard(plan = state.plan, onManageSubscription = onManageSubscription)
        }
        Reveal(delayMillis = 180) {
            UnlockedCard()
        }
        Reveal(delayMillis = 270) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GradientButton(
                    onClick = onTryAiImport,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.DocumentScanner,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.pro_cta_scan_first))
                }
                OutlinedButton(
                    onClick = onBackToRecipes,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.pro_back_to_recipes))
                }
            }
        }
    }
}

private fun ProHero() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(RescaleTheme.brandGradient, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = RescaleTheme.extendedColors.onGradient,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.pro_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.pro_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Which plan was bought, and — for monthly — where to manage it. */
private fun OwnedPlanCard(
    plan: ProPlan?,
    onManageSubscription: () -> Unit,
) {
    if (plan == null) return
    RescaleCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(
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
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

private fun UnlockedCard() {
    RescaleCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Text(
                text = stringResource(R.string.pro_unlocked_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            UnlockedBenefitRow(
                icon = Icons.Filled.AutoAwesome,
                title = stringResource(R.string.pro_benefit_ai),
                subtitle = stringResource(R.string.pro_benefit_ai_detail),
            )
            UnlockedBenefitRow(
                icon = Icons.Filled.Draw,
                title = stringResource(R.string.pro_benefit_handwriting),
                subtitle = stringResource(R.string.pro_benefit_handwriting_detail),
            )
            UnlockedBenefitRow(
                icon = Icons.Filled.Block,
                title = stringResource(R.string.pro_benefit_no_ads),
                subtitle = stringResource(R.string.pro_benefit_no_ads_detail),
            )
        }
    }
}

private fun UnlockedBenefitRow(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(
            icon = icon,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun IconBadge(
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Surface(
        modifier = Modifier.size(40.dp),
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
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private fun Reveal(delayMillis: Int, content: @Composable () -> Unit) {
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
        label = "pro_reveal",
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
