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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.jvm.Volatile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Google Play Billing implementation of [ProRepository].
 *
 * Entitlement is granted on the client-side PURCHASED state — there is no
 * backend verification server for Rescale, so acknowledge-anything-purchased
 * is the whole rule (refund revocation still propagates through Play's
 * purchase queries). Purchases are re-queried on init and on every process
 * resume, and the last known entitlement is persisted so Pro users don't see
 * the free UI while the first query is in flight.
 *
 * Connection is serialised through [connectionMutex] so concurrent callers
 * share a single [BillingClient.startConnection] attempt rather than racing.
 */
@Singleton
class PlayBillingProRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    @ApplicationScope private val appScope: CoroutineScope,
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

    private val _products = MutableStateFlow<List<ProProduct>>(emptyList())
    override val products: StateFlow<List<ProProduct>> = _products.asStateFlow()

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

        suspend fun query(productId: String, productType: String) {
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
                    ok = false
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Exception querying product $productId", e)
                ok = false
            }
        }

        query(ProProductIds.MONTHLY, BillingClient.ProductType.SUBS)
        query(ProProductIds.LIFETIME, BillingClient.ProductType.INAPP)

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
                acknowledgePurchaseIfNeeded(purchase)
                if (purchase.products.any {
                        it == ProProductIds.MONTHLY || it == ProProductIds.LIFETIME
                    }
                ) {
                    applyEntitlement(Entitlement(isPro = true, isPending = false))
                }
            }
            Purchase.PurchaseState.PENDING -> {
                _isPending.value = true
                Log.d(TAG, "Purchase pending")
            }
            else -> Log.w(TAG, "Purchase in state ${purchase.purchaseState}")
        }
    }

    private fun applyEntitlement(entitlement: Entitlement) {
        _isPro.value = entitlement.isPro
        _isPending.value = entitlement.isPending
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
