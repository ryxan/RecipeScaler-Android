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
        assertEquals(Entitlement(isPro = true, isPending = false), e)
    }
}
