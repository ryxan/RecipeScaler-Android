package com.ryxan.recipescaler.core.data.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/** Product IDs registered in Google Play Console. Must match Play Console exactly. */
object ProProductIds {
    /** Monthly subscription (BillingClient.ProductType.SUBS). */
    const val MONTHLY = "rescale_pro_monthly"

    /** One-time lifetime unlock (BillingClient.ProductType.INAPP). */
    const val LIFETIME = "rescale_pro_lifetime"
}

/**
 * A purchasable Pro product for display. Play Billing types deliberately do
 * not leak through this interface.
 */
data class ProProduct(
    val id: String,
    val title: String,
    val price: String,
    val isSubscription: Boolean,
)

/** Immediate result of asking Play to show the purchase UI. */
sealed interface PurchaseLaunch {
    /** Purchase UI is on screen; the outcome arrives via [ProRepository.isPro]/[isPending]. */
    data object Launched : PurchaseLaunch
    data object Cancelled : PurchaseLaunch
    data class Error(val message: String) : PurchaseLaunch
}

/**
 * Source of truth for the Rescale Pro entitlement. Consumers observe [isPro]
 * rather than querying Play Billing directly.
 */
interface ProRepository {
    val isPro: StateFlow<Boolean>
    val isPending: StateFlow<Boolean>
    val products: StateFlow<List<ProProduct>>

    /** Refresh the purchasable product list. False when Play Billing is unreachable. */
    suspend fun refreshProducts(): Boolean

    /** Launch the Play purchase flow for [productId]. */
    suspend fun launchPurchase(activity: Activity, productId: String): PurchaseLaunch

    /** Re-query Play for active purchases. False when the query couldn't run. */
    suspend fun restore(): Boolean

    /** Debug builds only: pin the entitlement for testing. No-op in release. */
    fun setDebugPro(value: Boolean?)
}
