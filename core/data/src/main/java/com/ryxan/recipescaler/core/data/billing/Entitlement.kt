package com.ryxan.recipescaler.core.data.billing

/** Billing-library-free view of a Play purchase, so [entitlementOf] stays unit-testable. */
data class PurchaseSnapshot(
    val products: List<String>,
    val state: PurchaseStatus,
    val acknowledged: Boolean,
)

enum class PurchaseStatus { PURCHASED, PENDING, OTHER }

/** The entitlement derived from a set of purchases. */
data class Entitlement(val isPro: Boolean, val isPending: Boolean, val plan: ProPlan? = null)

/**
 * Reduces Play purchases to the Pro entitlement. Only Rescale's own product
 * IDs count; a PURCHASED state grants Pro immediately (no backend
 * verification), while PENDING surfaces as pending without granting access.
 * When both plans are owned, [ProPlan.Monthly] wins: it's the one that needs
 * managing (the lifetime unlock is permanent regardless).
 */
fun entitlementOf(purchases: List<PurchaseSnapshot>): Entitlement {
    val ours = purchases.filter { purchase ->
        purchase.products.any { it == ProProductIds.MONTHLY || it == ProProductIds.LIFETIME }
    }
    val purchased = ours.filter { it.state == PurchaseStatus.PURCHASED }
    val plan = when {
        purchased.any { ProProductIds.MONTHLY in it.products } -> ProPlan.Monthly
        purchased.any { ProProductIds.LIFETIME in it.products } -> ProPlan.Lifetime
        else -> null
    }
    return Entitlement(
        isPro = purchased.isNotEmpty(),
        isPending = purchased.isEmpty() &&
            ours.any { it.state == PurchaseStatus.PENDING },
        plan = plan,
    )
}
