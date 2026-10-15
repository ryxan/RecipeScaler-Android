package com.ryxan.recipescaler.core.data.billing

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.ryxan.recipescaler.core.data.importer.AiImportQuota
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Drives [PlayBillingProRepository] through [PlayBillingProRepository.onPurchasesUpdated]
 * (the public [com.android.billingclient.api.PurchasesUpdatedListener] method) with
 * a fake [ProVerificationApi]. `Purchase` is constructed straight from a JSON
 * payload; `"acknowledged": true` keeps the acknowledge path from touching the
 * (unconnected) BillingClient. Play's own connection/query paths suspend or
 * fail harmlessly under Robolectric — these tests only assert the entitlement
 * the verification step applies.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayBillingProRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private var fileCounter = 0

    /**
     * The scope to inject as `appScope`. Must be a *foreground* test scope —
     * `advanceUntilIdle` intentionally stops once only `backgroundScope` work
     * remains, which would starve the repository's verification launches. A
     * plain [TestScope] on the same scheduler runs under `advanceUntilIdle`
     * and is never joined, so the init block's Play-connection query parking
     * on Robolectric's unbindable billing service can't hang teardown.
     */
    private fun TestScope.newAppScope() = TestScope(testScheduler)

    private fun newRepo(
        scope: CoroutineScope,
        api: ProVerificationApi,
    ): PlayBillingProRepository = PlayBillingProRepository(
        context = context,
        dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            File(tempFolder.root, "pro_${fileCounter++}.preferences_pb")
        },
        appScope = scope,
        aiQuota = FakeAiImportQuota(),
        verificationApi = api,
    )

    @Test
    fun `server-valid purchase grants Pro`() = runTest {
        val api = FakeProVerificationApi(
            Result.success(VerifyPurchaseResponse(valid = true, status = "active")),
        )
        val repo = newRepo(newAppScope(), api)
        advanceUntilIdle()

        repo.onPurchasesUpdated(okResult(), mutableListOf(purchase(ProProductIds.MONTHLY)))
        advanceUntilIdle()

        assertTrue(repo.isPro.value)
        assertFalse(repo.isPending.value)
        val request = api.requests.single()
        assertEquals(ProProductIds.MONTHLY, request.productId)
        assertEquals("subscription", request.productType)
        assertEquals(context.packageName, request.packageName)
        assertEquals(TEST_PURCHASE_TOKEN, request.purchaseToken)
    }

    @Test
    fun `lifetime purchase is verified as inapp`() = runTest {
        val api = FakeProVerificationApi(
            Result.success(VerifyPurchaseResponse(valid = true, status = "active")),
        )
        val repo = newRepo(newAppScope(), api)
        advanceUntilIdle()

        repo.onPurchasesUpdated(okResult(), mutableListOf(purchase(ProProductIds.LIFETIME)))
        advanceUntilIdle()

        assertTrue(repo.isPro.value)
        assertEquals("inapp", api.requests.single().productType)
    }

    @Test
    fun `invalid verdict does not grant Pro`() = runTest {
        val api = FakeProVerificationApi(
            Result.success(VerifyPurchaseResponse(valid = false, status = "invalid")),
        )
        val repo = newRepo(newAppScope(), api)
        advanceUntilIdle()

        repo.onPurchasesUpdated(okResult(), mutableListOf(purchase(ProProductIds.MONTHLY)))
        advanceUntilIdle()

        assertFalse(repo.isPro.value)
    }

    @Test
    fun `pending verdict does not grant Pro`() = runTest {
        val api = FakeProVerificationApi(
            Result.success(VerifyPurchaseResponse(valid = false, status = "pending")),
        )
        val repo = newRepo(newAppScope(), api)
        advanceUntilIdle()

        repo.onPurchasesUpdated(okResult(), mutableListOf(purchase(ProProductIds.MONTHLY)))
        advanceUntilIdle()

        assertFalse(repo.isPro.value)
    }

    @Test
    fun `unreachable backend fails open on the client purchase state`() = runTest {
        val api = FakeProVerificationApi(Result.failure(IOException("no route to host")))
        val repo = newRepo(newAppScope(), api)
        advanceUntilIdle()

        repo.onPurchasesUpdated(okResult(), mutableListOf(purchase(ProProductIds.MONTHLY)))
        advanceUntilIdle()

        assertTrue(repo.isPro.value)
    }

    @Test
    fun `backend timeout fails open`() = runTest {
        val api = FakeProVerificationApi(Result.failure(SocketTimeoutException("read timed out")))
        val repo = newRepo(newAppScope(), api)
        advanceUntilIdle()

        repo.onPurchasesUpdated(okResult(), mutableListOf(purchase(ProProductIds.MONTHLY)))
        advanceUntilIdle()

        assertTrue(repo.isPro.value)
    }

    @Test
    fun `rate limit fails open`() = runTest {
        val api = FakeProVerificationApi(Result.failure(HttpError(429)))
        val repo = newRepo(newAppScope(), api)
        advanceUntilIdle()

        repo.onPurchasesUpdated(okResult(), mutableListOf(purchase(ProProductIds.MONTHLY)))
        advanceUntilIdle()

        assertTrue(repo.isPro.value)
    }

    @Test
    fun `transient Google error fails open`() = runTest {
        val api = FakeProVerificationApi(Result.failure(HttpError(503)))
        val repo = newRepo(newAppScope(), api)
        advanceUntilIdle()

        repo.onPurchasesUpdated(okResult(), mutableListOf(purchase(ProProductIds.MONTHLY)))
        advanceUntilIdle()

        assertTrue(repo.isPro.value)
    }

    @Test
    fun `registry rejection fails closed`() = runTest {
        val api = FakeProVerificationApi(Result.failure(HttpError(403)))
        val repo = newRepo(newAppScope(), api)
        advanceUntilIdle()

        repo.onPurchasesUpdated(okResult(), mutableListOf(purchase(ProProductIds.MONTHLY)))
        advanceUntilIdle()

        assertFalse(repo.isPro.value)
    }

    @Test
    fun `denial emits verificationDenied and verifying tracks the request`() = runTest {
        val api = FakeProVerificationApi(Result.failure(HttpError(403)))
        val repo = newRepo(newAppScope(), api)
        val denied = mutableListOf<Unit>()
        // Unconfined so the subscriber resumes inline with tryEmit —
        // a test-dispatched collector isn't guaranteed to have subscribed
        // before the verification coroutine emits.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repo.verificationDenied.toList(denied)
        }
        var verifyingDuringCall: Boolean? = null
        api.onVerify = { verifyingDuringCall = repo.verifying.value }
        advanceUntilIdle()

        repo.onPurchasesUpdated(okResult(), mutableListOf(purchase(ProProductIds.MONTHLY)))
        advanceUntilIdle()

        assertFalse(repo.isPro.value)
        assertEquals(1, denied.size)
        assertEquals(true, verifyingDuringCall)
        assertFalse(repo.verifying.value)
    }

    @Test
    fun `backend product type prefers Play details over the id convention`() {
        // Play-reported type wins even when it contradicts the product id.
        assertEquals(
            "subscription",
            backendProductType(ProProductIds.LIFETIME, BillingClient.ProductType.SUBS),
        )
        assertEquals(
            "inapp",
            backendProductType(ProProductIds.MONTHLY, BillingClient.ProductType.INAPP),
        )
        // No details (purchase from another session): fall back to the id.
        assertEquals("subscription", backendProductType(ProProductIds.MONTHLY, null))
        assertEquals("inapp", backendProductType(ProProductIds.LIFETIME, null))
        assertEquals("inapp", backendProductType(ProProductIds.AI_SCANS_100, null))
    }

    @Test
    fun `denied purchase does not revoke an existing entitlement`() = runTest {
        val api = FakeProVerificationApi(
            Result.success(VerifyPurchaseResponse(valid = true, status = "active")),
        )
        val repo = newRepo(newAppScope(), api)
        advanceUntilIdle()

        repo.onPurchasesUpdated(okResult(), mutableListOf(purchase(ProProductIds.LIFETIME)))
        advanceUntilIdle()
        assertTrue(repo.isPro.value)

        // A later purchase attempt rejected by the backend must not clobber
        // the Pro already in force.
        api.result = Result.failure(HttpError(403))
        repo.onPurchasesUpdated(okResult(), mutableListOf(purchase(ProProductIds.MONTHLY)))
        advanceUntilIdle()

        assertTrue(repo.isPro.value)
    }

    @Test
    fun `unexpected exception fails closed`() = runTest {
        val api = FakeProVerificationApi(Result.failure(IllegalStateException("boom")))
        val repo = newRepo(newAppScope(), api)
        advanceUntilIdle()

        repo.onPurchasesUpdated(okResult(), mutableListOf(purchase(ProProductIds.MONTHLY)))
        advanceUntilIdle()

        assertFalse(repo.isPro.value)
    }

    private fun okResult(): BillingResult = BillingResult.newBuilder()
        .setResponseCode(BillingClient.BillingResponseCode.OK)
        .build()

    private fun purchase(
        productId: String,
        state: Int = Purchase.PurchaseState.PURCHASED,
    ): Purchase = Purchase(
        """
        {
          "orderId": "GPA.0000-0000-0000-00000",
          "packageName": "com.ryxan.recipescaler",
          "productId": "$productId",
          "productIds": ["$productId"],
          "purchaseTime": 1729000000000,
          "purchaseState": $state,
          "purchaseToken": "$TEST_PURCHASE_TOKEN",
          "acknowledged": true,
          "autoRenewing": false
        }
        """.trimIndent(),
        "test-signature",
    )

    private class FakeProVerificationApi(
        var result: Result<VerifyPurchaseResponse>,
    ) : ProVerificationApi {
        val requests = mutableListOf<VerifyPurchaseRequest>()
        var onVerify: (suspend () -> Unit)? = null

        override suspend fun verifyPurchase(request: VerifyPurchaseRequest): VerifyPurchaseResponse {
            requests += request
            onVerify?.invoke()
            return result.getOrThrow()
        }
    }

    private class FakeAiImportQuota : AiImportQuota {
        override val remainingFree: Flow<Int> = MutableStateFlow(0)
        override val bonusCredits: Flow<Int> = MutableStateFlow(0)
        override suspend fun tryReserve(isPro: Boolean): Boolean = false
        override suspend fun refund(isPro: Boolean) = Unit
        override suspend fun grantCredits(count: Int) = Unit
        override suspend fun debugSetRemaining(total: Int) = Unit
    }

    private companion object {
        const val TEST_PURCHASE_TOKEN = "test-purchase-token"
    }
}
