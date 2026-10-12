package com.ryxan.recipescaler.feature.pro

import android.app.Activity
import com.ryxan.recipescaler.core.data.billing.PurchaseLaunch
import com.ryxan.recipescaler.testing.FakeProRepository
import com.ryxan.recipescaler.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init refreshes products and loading clears`() = runTest {
        val repo = FakeProRepository()
        val vm = ProViewModel(repo)

        assertEquals(1, repo.refreshCalls)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `isPro and products flow into state`() = runTest {
        val repo = FakeProRepository(pro = true)
        repo.setProducts(
            listOf(
                com.ryxan.recipescaler.core.data.billing.ProProduct(
                    id = "rescale_pro_monthly",
                    title = "Monthly",
                    price = "$3.99",
                    isSubscription = true,
                ),
            ),
        )
        val vm = ProViewModel(repo)

        assertTrue(vm.uiState.value.isPro)
        assertEquals(1, vm.uiState.value.products.size)
    }

    @Test
    fun `purchase error surfaces as a one-shot message`() = runTest {
        val repo = FakeProRepository()
        repo.launchResult = PurchaseLaunch.Error("Billing is not available on this device")
        val vm = ProViewModel(repo)
        val activity = Robolectric.buildActivity(Activity::class.java).get()

        vm.purchase(activity, "rescale_pro_monthly")

        assertEquals(listOf("rescale_pro_monthly"), repo.launches)
        assertEquals("Billing is not available on this device", vm.messages.first())
    }

    @Test
    fun `restore failure surfaces a message`() = runTest {
        val repo = FakeProRepository()
        repo.restoreResult = false
        val vm = ProViewModel(repo)

        vm.restore()

        assertEquals(1, repo.restoreCalls)
        assertTrue(vm.messages.first().isNotBlank())
    }
}
