package com.ryxan.recipescaler.core.data.billing

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Product IDs registered in Google Play Console. Must match Play Console exactly. */
object ProProductIds {
    /** Monthly subscription (BillingClient.ProductType.SUBS). */
    const val MONTHLY = "rescale_pro_monthly"

    /** One-time lifetime unlock (BillingClient.ProductType.INAPP). */
    const val LIFETIME = "rescale_pro_lifetime"

    /**
     * Consumable AI scan pack — grants
     * [com.ryxan.recipescaler.core.domain.importer.AiImportConfig.SCANS_PER_PACK]
     * bonus credits. Consumed immediately so it can be repurchased.
     */
    const val AI_SCANS_100 = "rescale_ai_scans_100"
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

/** Which Pro plan the user owns, when known. */
enum class ProPlan { Monthly, Lifetime }

/**
 * Source of truth for the Rescale Pro entitlement. Consumers observe [isPro]
 * rather than querying Play Billing directly.
 */
interface ProRepository {
    val isPro: StateFlow<Boolean>
    val isPending: StateFlow<Boolean>
    val products: StateFlow<List<ProProduct>>

    /**
     * The owned Pro plan, or null for free users / when the plan couldn't be
     * determined (e.g. the debug entitlement override, which carries no
     * purchase). The post-purchase screen uses this for the plan row and the
     * "Manage subscription" link, which only applies to [ProPlan.Monthly].
     */
    val plan: StateFlow<ProPlan?>

    /**
     * One-shot event with the number of bonus AI scans granted by a
     * consumable purchase ([ProProductIds.AI_SCANS_100]).
     */
    val creditsGranted: Flow<Int>

    /** Refresh the purchasable product list. False when Play Billing is unreachable. */
    suspend fun refreshProducts(): Boolean

    /** Launch the Play purchase flow for [productId]. */
    suspend fun launchPurchase(activity: Activity, productId: String): PurchaseLaunch

    /** Re-query Play for active purchases. False when the query couldn't run. */
    suspend fun restore(): Boolean

    /** Debug builds only: pin the entitlement for testing. No-op in release. */
    fun setDebugPro(value: Boolean?)
}
