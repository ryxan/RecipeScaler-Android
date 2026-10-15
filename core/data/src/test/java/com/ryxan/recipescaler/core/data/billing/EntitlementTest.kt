package com.ryxan.recipescaler.core.data.billing

import org.junit.Assert.assertEquals
import org.junit.Test

class EntitlementTest {

    private fun purchase(
        products: List<String>,
        state: PurchaseStatus = PurchaseStatus.PURCHASED,
        acknowledged: Boolean = true,
    ) = PurchaseSnapshot(products = products, state = state, acknowledged = acknowledged)

    @Test
    fun `lifetime purchase grants Pro`() {
        val e = entitlementOf(listOf(purchase(listOf(ProProductIds.LIFETIME))))
        assertEquals(Entitlement(isPro = true, isPending = false), e)
    }

    @Test
    fun `monthly subscription purchase grants Pro`() {
        val e = entitlementOf(listOf(purchase(listOf(ProProductIds.MONTHLY))))
        assertEquals(Entitlement(isPro = true, isPending = false), e)
    }

    @Test
    fun `pending purchase is pending but not Pro`() {
        val e = entitlementOf(
            listOf(purchase(listOf(ProProductIds.LIFETIME), state = PurchaseStatus.PENDING)),
        )
        assertEquals(Entitlement(isPro = false, isPending = true), e)
    }

    @Test
    fun `unrelated product grants nothing`() {
        val e = entitlementOf(listOf(purchase(listOf("some_other_app_sku"))))
        assertEquals(Entitlement(isPro = false, isPending = false), e)
    }

    @Test
    fun `empty purchase list is no entitlement`() {
        assertEquals(Entitlement(isPro = false, isPending = false), entitlementOf(emptyList()))
    }

    @Test
    fun `purchased beats pending in the same list`() {
        val e = entitlementOf(
            listOf(
                purchase(listOf(ProProductIds.MONTHLY)),
                purchase(listOf(ProProductIds.LIFETIME), state = PurchaseStatus.PENDING),
            ),
        )
        assertEquals(Entitlement(isPro = true, isPending = false, plan = ProPlan.Monthly), e)
    }

    @Test
    fun `monthly purchase reports the monthly plan`() {
        val e = entitlementOf(listOf(purchase(listOf(ProProductIds.MONTHLY))))
        assertEquals(ProPlan.Monthly, e.plan)
    }

    @Test
    fun `lifetime purchase reports the lifetime plan`() {
        val e = entitlementOf(listOf(purchase(listOf(ProProductIds.LIFETIME))))
        assertEquals(ProPlan.Lifetime, e.plan)
    }

    @Test
    fun `both plans owned reports monthly so it can be managed`() {
        val e = entitlementOf(
            listOf(
                purchase(listOf(ProProductIds.MONTHLY)),
                purchase(listOf(ProProductIds.LIFETIME)),
            ),
        )
        assertEquals(ProPlan.Monthly, e.plan)
    }

    @Test
    fun `pending-only purchase reports no plan`() {
        val e = entitlementOf(
            listOf(purchase(listOf(ProProductIds.MONTHLY), state = PurchaseStatus.PENDING)),
        )
        assertEquals(null, e.plan)
    }

    @Test
    fun `no entitlement reports no plan`() {
        assertEquals(null, entitlementOf(emptyList()).plan)
    }
}
