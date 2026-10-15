package com.ryxan.recipescaler.core.data.billing

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.ryxan.recipescaler.core.data.BuildConfig
import com.ryxan.recipescaler.core.data.di.ApplicationScope
import com.ryxan.recipescaler.core.data.importer.AiImportQuota
import com.ryxan.recipescaler.core.domain.importer.AiImportConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.jvm.Volatile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Google Play Billing implementation of [ProRepository].
 *
 * New Pro purchases are verified server-side (POST verify-purchase on
 * api.riverwatch.cc) before the entitlement is granted — the client-side
 * PURCHASED state can be spoofed on rooted devices. Backend rate limits
 * (HTTP 429), transient Google errors (503) and network failures fail open
 * on the client state; explicit rejections fail closed without touching the
 * current entitlement. The restore path ([queryActivePurchases], re-run on
 * init and every process resume) intentionally skips backend verification —
 * Play's own query is authoritative for expiry/refund revocation — and the
 * last known entitlement is persisted as an offline grace cache so Pro
 * users don't see the free UI while the first query is in flight.
 *
 * Connection is serialised through [connectionMutex] so concurrent callers
 * share a single [BillingClient.startConnection] attempt rather than racing.
 */
@Singleton
class PlayBillingProRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    @ApplicationScope private val appScope: CoroutineScope,
    private val aiQuota: AiImportQuota,
    private val verificationApi: ProVerificationApi,
) : ProRepository, PurchasesUpdatedListener {

    private val billingClient: BillingClient by lazy {
        BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build(),
            )
            .enableAutoServiceReconnection()
            .build()
    }

    private val _isPro = MutableStateFlow(false)
    override val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _isPending = MutableStateFlow(false)
    override val isPending: StateFlow<Boolean> = _isPending.asStateFlow()

    private val _plan = MutableStateFlow<ProPlan?>(null)
    override val plan: StateFlow<ProPlan?> = _plan.asStateFlow()

    private val _products = MutableStateFlow<List<ProProduct>>(emptyList())
    override val products: StateFlow<List<ProProduct>> = _products.asStateFlow()

    private val _creditsGranted = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    override val creditsGranted: Flow<Int> = _creditsGranted.asSharedFlow()

    private val _verifying = MutableStateFlow(false)
    override val verifying: StateFlow<Boolean> = _verifying.asStateFlow()

    private val _verificationDenied = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val verificationDenied: Flow<Unit> = _verificationDenied.asSharedFlow()

    /** Play-side details for [products], needed to build the purchase flow params. */
    @Volatile
    private var productDetails: Map<String, ProductDetails> = emptyMap()

    @Volatile
    private var isConnected = false

    /**
     * Debug-only forced entitlement. When non-null, queries return immediately
     * with this state instead of hitting Play, so init/resume re-queries can't
     * clobber it. Always null in release — [setDebugPro] is a no-op unless
     * [BuildConfig.DEBUG].
     */
    @Volatile
    private var debugPro: Boolean? = null

    private val connectionMutex = Mutex()

    init {
        // Seed from the persisted entitlement, then query Play so cold starts
        // show Pro UI immediately for entitled users.
        appScope.launch {
            val persisted = try {
                dataStore.data.first()[KEY_IS_PRO] ?: false
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                false
            }
            if (persisted && debugPro == null) _isPro.value = true
            try {
                queryActivePurchases()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Failed to query active purchases on init", e)
            }
        }

        // Re-query on every app resume to pick up purchases completed on
        // another device or while the app was backgrounded. addObserver must
        // run on the main thread; this @Singleton may be constructed off it.
        Handler(Looper.getMainLooper()).post {
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    appScope.launch {
                        try {
                            queryActivePurchases()
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            Log.e(TAG, "Failed to query active purchases on resume", e)
                        }
                    }
                }
            })
        }
    }

    // -------------------------------------------------------------------------
    // PurchasesUpdatedListener
    // -------------------------------------------------------------------------

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases.orEmpty().forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User cancelled purchase")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "Item already owned, re-querying active purchases")
                appScope.launch {
                    try {
                        queryActivePurchases()
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Log.e(TAG, "Failed to re-query after ITEM_ALREADY_OWNED", e)
                    }
                }
            }
            else -> {
                Log.e(TAG, "Purchase error: ${billingResult.debugMessage} (code: ${billingResult.responseCode})")
            }
        }
    }

    // -------------------------------------------------------------------------
    // Connection
    // -------------------------------------------------------------------------

    private suspend fun ensureConnected(): Boolean = connectionMutex.withLock {
        if (isConnected) return@withLock true

        suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    isConnected = billingResult.responseCode == BillingClient.BillingResponseCode.OK
                    if (!continuation.isCompleted) continuation.resume(isConnected)
                }

                override fun onBillingServiceDisconnected() {
                    isConnected = false
                    if (!continuation.isCompleted) continuation.resume(false)
                }
            })
        }
    }

    // -------------------------------------------------------------------------
    // ProRepository
    // -------------------------------------------------------------------------

    override suspend fun refreshProducts(): Boolean {
        if (!ensureConnected()) return false

        val found = mutableListOf<ProProduct>()
        val details = mutableMapOf<String, ProductDetails>()
        var ok = true

        suspend fun query(productId: String, productType: String, essential: Boolean = true) {
            try {
                val params = QueryProductDetailsParams.newBuilder()
                    .setProductList(
                        listOf(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(productId)
                                .setProductType(productType)
                                .build(),
                        ),
                    )
                    .build()
                val result = billingClient.queryProductDetails(params)
                if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    result.productDetailsList.orEmpty().forEach { d ->
                        mapProduct(d)?.let { found += it }
                        details[d.productId] = d
                    }
                } else {
                    Log.e(TAG, "Product query failed for $productId: ${result.billingResult.debugMessage}")
                    if (essential) ok = false
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Exception querying product $productId", e)
                if (essential) ok = false
            }
        }

        query(ProProductIds.MONTHLY, BillingClient.ProductType.SUBS)
        query(ProProductIds.LIFETIME, BillingClient.ProductType.INAPP)
        // Optional consumable — the app must keep working before it exists
        // in Play Console, so a failure here never fails the refresh.
        query(ProProductIds.AI_SCANS_100, BillingClient.ProductType.INAPP, essential = false)

        _products.value = found
        productDetails = details
        return ok && found.isNotEmpty()
    }

    override suspend fun launchPurchase(activity: Activity, productId: String): PurchaseLaunch {
        if (!ensureConnected()) return PurchaseLaunch.Error("Billing service unavailable")

        val details = productDetails[productId]
            ?: return PurchaseLaunch.Error("Product unavailable")

        val offerToken = when {
            productId == ProProductIds.MONTHLY ->
                details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            else ->
                details.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken
                    ?: details.oneTimePurchaseOfferDetails?.offerToken
        } ?: return PurchaseLaunch.Error("No purchase offer available")

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build(),
                ),
            )
            .build()

        val result = billingClient.launchBillingFlow(activity, params)
        return when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> PurchaseLaunch.Launched
            BillingClient.BillingResponseCode.USER_CANCELED -> PurchaseLaunch.Cancelled
            else -> {
                Log.e(TAG, "Failed to launch purchase flow: ${result.debugMessage} (code: ${result.responseCode})")
                PurchaseLaunch.Error(mapBillingErrorToUserMessage(result.responseCode))
            }
        }
    }

    override suspend fun restore(): Boolean = queryActivePurchases()

    override fun setDebugPro(value: Boolean?) {
        if (!BuildConfig.DEBUG) return
        debugPro = value
        if (value != null) {
            applyEntitlement(Entitlement(isPro = value, isPending = false))
        } else {
            appScope.launch {
                try {
                    queryActivePurchases()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e(TAG, "Failed to re-query after clearing debug override", e)
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Purchase queries and handling
    // -------------------------------------------------------------------------

    private suspend fun queryActivePurchases(): Boolean {
        debugPro?.let {
            applyEntitlement(Entitlement(isPro = it, isPending = false))
            return true
        }
        if (!ensureConnected()) return false

        try {
            val snapshots = mutableListOf<PurchaseSnapshot>()
            for (type in listOf(BillingClient.ProductType.SUBS, BillingClient.ProductType.INAPP)) {
                val result = billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(type).build(),
                )
                if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.w(TAG, "Purchase query failed with code ${result.billingResult.responseCode}")
                    return false
                }
                result.purchasesList.orEmpty().forEach { purchase ->
                    if (purchase.products.contains(ProProductIds.AI_SCANS_100)) {
                        // Consumables are consumed, never acknowledged — Play
                        // stops returning them once consumed, so recovery here
                        // can't double-grant.
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            consumeScanPack(purchase)
                        }
                        return@forEach
                    }
                    snapshots += PurchaseSnapshot(
                        products = purchase.products,
                        state = when (purchase.purchaseState) {
                            Purchase.PurchaseState.PURCHASED -> PurchaseStatus.PURCHASED
                            Purchase.PurchaseState.PENDING -> PurchaseStatus.PENDING
                            else -> PurchaseStatus.OTHER
                        },
                        acknowledged = purchase.isAcknowledged,
                    )
                    acknowledgePurchaseIfNeeded(purchase)
                }
            }
            applyEntitlement(entitlementOf(snapshots))
            return true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Exception querying active purchases", e)
            return false
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                if (purchase.products.contains(ProProductIds.AI_SCANS_100)) {
                    appScope.launch { consumeScanPack(purchase) }
                    return
                }
                acknowledgePurchaseIfNeeded(purchase)
                if (purchase.products.any {
                        it == ProProductIds.MONTHLY || it == ProProductIds.LIFETIME
                    }
                ) {
                    // Grant only after the backend confirms the purchase —
                    // see verifyPurchaseWithBackend for the failure policy.
                    // The plan comes from the verified purchase, so the
                    // post-purchase page can show what was bought.
                    appScope.launch {
                        if (verifyPurchaseWithBackend(purchase)) {
                            val plan = when {
                                ProProductIds.MONTHLY in purchase.products -> ProPlan.Monthly
                                else -> ProPlan.Lifetime
                            }
                            applyEntitlement(
                                Entitlement(isPro = true, isPending = false, plan = plan),
                            )
                        } else {
                            Log.w(TAG, "Purchase verification denied; entitlement unchanged")
                            _verificationDenied.tryEmit(Unit)
                        }
                    }
                }
            }
            Purchase.PurchaseState.PENDING -> {
                _isPending.value = true
                Log.d(TAG, "Purchase pending")
            }
            else -> Log.w(TAG, "Purchase in state ${purchase.purchaseState}")
        }
    }

    /**
     * Verify [purchase] with the riverwatch backend. True means the
     * entitlement may be granted.
     *
     * The backend calls the Google Play Developer API, the authoritative
     * source of truth — the client-side PURCHASED state can be spoofed on
     * rooted devices. Failure policy:
     * - `valid == false` (any status, incl. 'pending'/'invalid') or a non-2xx
     *   other than 429/503 → fail closed: no grant. The existing entitlement
     *   is left untouched, so a failed *new* purchase can't revoke live Pro.
     * - HTTP 429 (rate limited) or 503 (transient Google error), or the
     *   backend being unreachable ([UnknownHostException],
     *   [SocketTimeoutException], [IOException]) → fail open on the
     *   client-side PURCHASED state.
     * - [CancellationException] propagates; any other exception fails closed.
     *
     * The purchase token is sent to the backend but never logged.
     */
    private suspend fun verifyPurchaseWithBackend(purchase: Purchase): Boolean {
        val productId = purchase.products.firstOrNull()
        if (productId == null) {
            Log.e(TAG, "Purchase has no product ID")
            return false
        }
        val request = VerifyPurchaseRequest(
            // context.packageName is com.ryxan.recipescaler.debug in debug
            // builds — the backend intentionally rejects it; setDebugPro
            // covers debug testing.
            packageName = context.packageName,
            productId = productId,
            purchaseToken = purchase.purchaseToken,
            productType = backendProductType(productId, productDetails[productId]?.productType),
        )
        _verifying.value = true
        return try {
            val response = verificationApi.verifyPurchase(request)
            Log.d(TAG, "Verification response: valid=${response.valid}, status=${response.status}")
            if (!response.valid) {
                Log.w(TAG, "Backend rejected purchase: status=${response.status}")
            }
            response.valid
        } catch (e: HttpError) {
            if (e.code == 429 || e.code == 503) {
                // Rate-limited or Google transiently down — same treatment as
                // a network failure.
                Log.w(TAG, "Backend temporarily unavailable (HTTP ${e.code}); granting on client state")
                true
            } else {
                Log.e(TAG, "Backend rejected purchase with HTTP ${e.code}")
                false
            }
        } catch (e: UnknownHostException) {
            Log.w(TAG, "Backend unreachable (DNS); granting on client state")
            true
        } catch (e: SocketTimeoutException) {
            Log.w(TAG, "Backend timed out; granting on client state")
            true
        } catch (e: IOException) {
            Log.w(TAG, "Backend unreachable (I/O); granting on client state")
            true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Verification exception, failing closed", e)
            false
        } finally {
            _verifying.value = false
        }
    }

    /**
     * Consume an AI scan pack purchase and credit the bonus balance.
     *
     * Consume-then-grant means a crash in between can lose a pack (Play stops
     * reporting the purchase) — the inverse order could double-grant on the
     * recovery query, which is the worse failure.
     */
    private suspend fun consumeScanPack(purchase: Purchase) {
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        val result = suspendCancellableCoroutine<BillingResult> { continuation ->
            billingClient.consumeAsync(params) { billingResult, _ ->
                continuation.resume(billingResult)
            }
        }
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            aiQuota.grantCredits(AiImportConfig.SCANS_PER_PACK)
            _creditsGranted.emit(AiImportConfig.SCANS_PER_PACK)
            Log.d(TAG, "AI scan pack consumed and credited")
        } else {
            Log.e(TAG, "Failed to consume AI scan pack: ${result.debugMessage}")
        }
    }

    private fun applyEntitlement(entitlement: Entitlement) {
        _isPro.value = entitlement.isPro
        _isPending.value = entitlement.isPending
        _plan.value = entitlement.plan
        appScope.launch {
            try {
                dataStore.edit { it[KEY_IS_PRO] = entitlement.isPro }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    private fun acknowledgePurchaseIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { result ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.e(TAG, "Failed to acknowledge purchase: ${result.debugMessage}")
                }
            }
        }
    }

    private fun mapProduct(details: ProductDetails): ProProduct? = when (details.productType) {
        BillingClient.ProductType.SUBS -> {
            val price = details.subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "—"
            ProProduct(
                id = details.productId,
                title = details.title,
                price = price,
                isSubscription = true,
            )
        }
        BillingClient.ProductType.INAPP -> {
            val offer = details.oneTimePurchaseOfferDetailsList?.firstOrNull()
                ?: details.oneTimePurchaseOfferDetails
                ?: return null
            ProProduct(
                id = details.productId,
                title = details.title,
                price = offer.formattedPrice,
                isSubscription = false,
            )
        }
        else -> null
    }

    private companion object {
        const val TAG = "ProRepository"
        val KEY_IS_PRO = booleanPreferencesKey("pro_entitled")

        fun mapBillingErrorToUserMessage(responseCode: Int): String = when (responseCode) {
            BillingClient.BillingResponseCode.USER_CANCELED -> "Purchase cancelled"
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "Play Store is unavailable. Please try again later."
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "Billing is not available on this device"
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "This item is not available for purchase"
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "Configuration error. Please contact support."
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "You already own this item"
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> "Purchase not found"
            else -> "An error occurred. Please try again."
        }
    }
}

/**
 * The `productType` sent to the verify endpoint. Play's own product type is
 * authoritative when details were fetched in this session; the product-id
 * convention is the fallback for purchases completed in a different session
 * (the details map is empty).
 */
internal fun backendProductType(productId: String, playProductType: String?): String =
    when (playProductType) {
        BillingClient.ProductType.SUBS -> "subscription"
        BillingClient.ProductType.INAPP -> "inapp"
        else -> if (productId == ProProductIds.MONTHLY) "subscription" else "inapp"
    }
