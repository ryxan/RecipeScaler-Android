package com.ryxan.recipescaler.feature.pro

import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ryxan.recipescaler.R
import com.ryxan.recipescaler.core.data.billing.ProPlan
import com.ryxan.recipescaler.core.data.billing.ProProduct
import com.ryxan.recipescaler.core.designsystem.GradientButton
import com.ryxan.recipescaler.core.designsystem.GradientTopAppBar
import com.ryxan.recipescaler.core.designsystem.RescaleCard
import com.ryxan.recipescaler.core.designsystem.RescaleTheme
import com.ryxan.recipescaler.core.designsystem.Spacing
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
    onNavigateUp: () -> Unit = {},
    onTryAiImport: () -> Unit = {},
    onBackToRecipes: () -> Unit = {},
    onManageSubscription: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
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
            ProPurchaseContent(
                state = state,
                onPurchase = onPurchase,
                onRestore = onRestore,
                modifier = Modifier.padding(innerPadding),
            )
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
@Composable
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
            PlanCard(plan = state.plan, onManageSubscription = onManageSubscription)
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

@Composable
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
@Composable
private fun PlanCard(
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

@Composable
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
            BenefitRow(
                icon = Icons.Filled.AutoAwesome,
                title = stringResource(R.string.pro_benefit_ai),
                subtitle = stringResource(R.string.pro_benefit_ai_detail),
            )
            BenefitRow(
                icon = Icons.Filled.Draw,
                title = stringResource(R.string.pro_benefit_handwriting),
                subtitle = stringResource(R.string.pro_benefit_handwriting_detail),
            )
            BenefitRow(
                icon = Icons.Filled.Block,
                title = stringResource(R.string.pro_benefit_no_ads),
                subtitle = stringResource(R.string.pro_benefit_no_ads_detail),
            )
        }
    }
}

@Composable
private fun BenefitRow(icon: ImageVector, title: String, subtitle: String) {
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

// ── Pre-purchase: the paywall (unchanged) ───────────────────────────────────

@Composable
private fun ProPurchaseContent(
    state: ProUiState,
    onPurchase: (String) -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RescaleCard(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(Spacing.l),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                Text(
                    text = stringResource(R.string.pro_benefit_ai),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.pro_benefit_handwriting),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        when {
            state.isPending -> PendingContent()
            state.loading -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
            state.products.isEmpty() -> Text(
                text = stringResource(R.string.pro_unavailable),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> state.products.forEach { product: ProProduct ->
                if (product.isSubscription) {
                    GradientButton(
                        onClick = { onPurchase(product.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.pro_price_monthly,
                                product.price,
                            ),
                        )
                    }
                } else {
                    FilledTonalButton(
                        onClick = { onPurchase(product.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.pro_price_lifetime,
                                product.price,
                            ),
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onRestore,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_restore_purchases))
        }
    }
}

/** A pending payment needs reassurance, not just a status label. */
@Composable
private fun PendingContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    MaterialTheme.colorScheme.secondaryContainer,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.HourglassEmpty,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.pro_pending),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.pro_pending_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Shared building blocks ──────────────────────────────────────────────────

@Composable
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

/** Fades and lifts content in once — subtle staggered entrance. */
@Composable
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
