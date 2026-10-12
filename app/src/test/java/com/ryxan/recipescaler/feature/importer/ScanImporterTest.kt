package com.ryxan.recipescaler.feature.importer

import android.net.Uri
import com.ryxan.recipescaler.core.data.importer.AiImportResult
import com.ryxan.recipescaler.core.data.importer.OcrText
import com.ryxan.recipescaler.core.domain.importer.AiImportConfig
import com.ryxan.recipescaler.testing.FakeAiImportQuota
import com.ryxan.recipescaler.testing.FakeAiRecipeImporter
import com.ryxan.recipescaler.testing.FakeImagePreprocessor
import com.ryxan.recipescaler.testing.FakeOcrEngine
import com.ryxan.recipescaler.testing.FakeProRepository
import com.ryxan.recipescaler.testing.testScanImporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScanImporterTest {

    private val uri: Uri = Uri.parse("content://test/scan.jpg")

    private fun ocrText(text: String) = OcrText(lines = emptyList(), text = text)

    private class Fixture(
        val ocr: FakeOcrEngine = FakeOcrEngine(),
        val images: FakeImagePreprocessor = FakeImagePreprocessor(),
        val importer: FakeAiRecipeImporter = FakeAiRecipeImporter(),
        val quota: FakeAiImportQuota = FakeAiImportQuota(),
        val pro: FakeProRepository = FakeProRepository(),
        aiAvailable: Boolean = true,
    ) {
        val scan = testScanImporter(ocr, images, importer, quota, pro, aiAvailable)
    }

    // ── On-device path ────────────────────────────────────────────────────────

    @Test
    fun `on-device scan parses lines and returns Parsed`() = runTest {
        val f = Fixture()
        f.ocr.next = ocrText("1 cup flour\n2 eggs")

        val outcome = f.scan.scan(uri, useAi = false, deleteWhenDone = false)

        val parsed = outcome as ScanOutcome.Parsed
        assertEquals(2, parsed.items.size)
        assertEquals("Flour", parsed.items[0].item.name)
        assertFalse(parsed.offerAiRetry)
    }

    @Test
    fun `ocr failure maps to RECOGNITION_FAILED and offers AI retry when jpeg caches`() = runTest {
        val f = Fixture()
        f.ocr.next = null

        val outcome = f.scan.scan(uri, useAi = false, deleteWhenDone = false)

        val failed = outcome as ScanOutcome.Failed
        assertEquals(ScanError.RECOGNITION_FAILED, failed.error)
        assertTrue(failed.canRetryWithAi)
        assertFalse(failed.canUseOnDevice)
        assertEquals(1, f.images.jpegCalls)
    }

    @Test
    fun `empty parse maps to NO_INGREDIENTS_FOUND`() = runTest {
        val f = Fixture()
        f.ocr.next = ocrText("")

        val outcome = f.scan.scan(uri, useAi = false, deleteWhenDone = false)

        val failed = outcome as ScanOutcome.Failed
        assertEquals(ScanError.NO_INGREDIENTS_FOUND, failed.error)
        assertTrue(failed.canRetryWithAi)
    }

    @Test
    fun `poor parse applies but offers AI retry`() = runTest {
        val f = Fixture()
        f.ocr.next = ocrText("1 cup flour") // single ingredient → poor

        val outcome = f.scan.scan(uri, useAi = false, deleteWhenDone = false)

        val parsed = outcome as ScanOutcome.Parsed
        assertEquals(1, parsed.items.size)
        assertTrue(parsed.offerAiRetry)
    }

    @Test
    fun `temp file is deleted when deleteWhenDone`() = runTest {
        val f = Fixture()
        f.ocr.next = ocrText("1 cup flour\n2 eggs")

        f.scan.scan(uri, useAi = false, deleteWhenDone = true)

        assertEquals(listOf(uri), f.images.deleted)
    }

    @Test
    fun `temp file is kept when deleteWhenDone is false`() = runTest {
        val f = Fixture()
        f.ocr.next = ocrText("1 cup flour\n2 eggs")

        f.scan.scan(uri, useAi = false, deleteWhenDone = false)

        assertTrue(f.images.deleted.isEmpty())
    }

    // ── AI path ───────────────────────────────────────────────────────────────

    @Test
    fun `AI success spends one credit and returns recipe fields`() = runTest {
        val f = Fixture()

        val outcome = f.scan.scan(uri, useAi = true, deleteWhenDone = false)

        val parsed = outcome as ScanOutcome.Parsed
        assertEquals("Test Cake", parsed.title)
        assertEquals("8", parsed.servings)
        assertEquals(2, parsed.items.size)
        assertFalse(parsed.offerAiRetry)
        assertEquals(1, f.importer.callCount)
        assertEquals(1, f.quota.freeUsed)
        assertEquals(0, f.quota.refundCalls)
    }

    @Test
    fun `undecodable image maps to RECOGNITION_FAILED without touching quota`() = runTest {
        val f = Fixture()
        f.images.jpeg = null

        val outcome = f.scan.scan(uri, useAi = true, deleteWhenDone = false)

        val failed = outcome as ScanOutcome.Failed
        assertEquals(ScanError.RECOGNITION_FAILED, failed.error)
        assertFalse(failed.canRetryWithAi)
        assertEquals(0, f.importer.callCount)
        assertEquals(0, f.quota.reserveCalls)
    }

    @Test
    fun `every non-success result refunds the credit`() = runTest {
        val variants = listOf(
            AiImportResult.NothingFound,
            AiImportResult.NetworkError,
            AiImportResult.Blocked,
            AiImportResult.RateLimited,
            AiImportResult.Failed(RuntimeException("boom")),
        )
        for (variant in variants) {
            val f = Fixture()
            f.importer.nextResult = variant

            val outcome = f.scan.scan(uri, useAi = true, deleteWhenDone = false)

            assertTrue("expected Failed for $variant", outcome is ScanOutcome.Failed)
            assertEquals("expected one refund for $variant", 1, f.quota.refundCalls)
            assertEquals(0, f.quota.freeUsed)
        }
    }

    @Test
    fun `a thrown exception refunds and maps to AI_FAILED`() = runTest {
        val f = Fixture()
        f.importer.onImport = { throw RuntimeException("boom") }

        val outcome = f.scan.scan(uri, useAi = true, deleteWhenDone = false)

        val failed = outcome as ScanOutcome.Failed
        assertEquals(ScanError.AI_FAILED, failed.error)
        assertEquals(1, f.quota.refundCalls)
    }

    @Test
    fun `non-success variants map to the right ScanError`() = runTest {
        val cases = mapOf(
            AiImportResult.NothingFound to ScanError.NO_INGREDIENTS_FOUND,
            AiImportResult.NetworkError to ScanError.AI_NETWORK_ERROR,
            AiImportResult.Blocked to ScanError.AI_BLOCKED,
            AiImportResult.RateLimited to ScanError.AI_RATE_LIMITED,
            AiImportResult.Failed() to ScanError.AI_FAILED,
        )
        for ((result, expected) in cases) {
            val f = Fixture()
            f.importer.nextResult = result
            val outcome = f.scan.scan(uri, useAi = true, deleteWhenDone = false)
            assertEquals(expected, (outcome as ScanOutcome.Failed).error)
        }
    }

    @Test
    fun `cancellation mid-import still refunds the credit`() = runTest {
        val f = Fixture()
        val gate = CompletableDeferred<AiImportResult>()
        f.importer.onImport = { gate.await() }

        val job = async { f.scan.scan(uri, useAi = true, deleteWhenDone = false) }
        gate.completeExceptionally(CancellationException("cancelled"))

        try {
            job.await()
        } catch (e: CancellationException) {
            // expected — the scan rethrows cancellation
        }
        assertEquals(1, f.quota.refundCalls)
        assertEquals(0, f.quota.freeUsed)
    }

    @Test
    fun `free user with no credits gets the paywall and no import call`() = runTest {
        val f = Fixture()
        repeat(AiImportConfig.FREE_LIFETIME_LIMIT) { f.quota.tryReserve(isPro = false) }

        val outcome = f.scan.scan(uri, useAi = true, deleteWhenDone = false)

        assertEquals(ScanOutcome.PaywallRequired, outcome)
        assertEquals(0, f.importer.callCount)
    }

    @Test
    fun `Pro user bypasses the free lifetime limit`() = runTest {
        val f = Fixture(pro = FakeProRepository(pro = true))
        repeat(AiImportConfig.FREE_LIFETIME_LIMIT) { f.quota.tryReserve(isPro = false) }

        val outcome = f.scan.scan(uri, useAi = true, deleteWhenDone = false)

        assertTrue(outcome is ScanOutcome.Parsed)
        assertEquals(1, f.importer.callCount)
        assertEquals(1, f.quota.proUsed)
    }

    @Test
    fun `Pro user at the daily cap gets AI_LIMIT_REACHED`() = runTest {
        val f = Fixture(pro = FakeProRepository(pro = true))
        repeat(AiImportConfig.PREMIUM_DAILY_CAP) { f.quota.tryReserve(isPro = true) }

        val outcome = f.scan.scan(uri, useAi = true, deleteWhenDone = false)

        val failed = outcome as ScanOutcome.Failed
        assertEquals(ScanError.AI_LIMIT_REACHED, failed.error)
        assertEquals(0, f.importer.callCount)
    }

    @Test
    fun `retry reuses the cached jpeg bytes`() = runTest {
        val f = Fixture()
        f.ocr.next = null // on-device scan fails; jpeg is cached for retry

        f.scan.scan(uri, useAi = false, deleteWhenDone = false)
        val outcome = f.scan.retryWithAi()

        assertTrue(outcome is ScanOutcome.Parsed)
        assertEquals(1, f.importer.callCount)
        assertTrue(f.importer.jpegs.single().contentEquals(f.images.jpeg!!))
    }

    @Test
    fun `retry after clear returns null`() = runTest {
        val f = Fixture()
        f.ocr.next = null

        f.scan.scan(uri, useAi = false, deleteWhenDone = false)
        f.scan.clear()

        assertNull(f.scan.retryWithAi())
        assertEquals(0, f.importer.callCount)
    }

    @Test
    fun `aiAvailable false never touches AI or quota and offers no retry`() = runTest {
        val f = Fixture(aiAvailable = false)
        f.ocr.next = null

        val outcome = f.scan.scan(uri, useAi = true, deleteWhenDone = false)

        val failed = outcome as ScanOutcome.Failed
        assertFalse(failed.canRetryWithAi)
        assertEquals(0, f.importer.callCount)
        assertEquals(0, f.quota.reserveCalls)
        assertEquals(0, f.images.jpegCalls)
        assertNull(f.scan.retryWithAi())
    }

    @Test
    fun `AI network error offers on-device fallback which OCRs the cached jpeg`() = runTest {
        val f = Fixture()
        f.importer.nextResult = AiImportResult.NetworkError

        val outcome = f.scan.scan(uri, useAi = true, deleteWhenDone = false)
        val failed = outcome as ScanOutcome.Failed
        assertEquals(ScanError.AI_NETWORK_ERROR, failed.error)
        assertTrue(failed.canUseOnDevice)

        f.ocr.next = ocrText("1 cup flour\n2 eggs")
        val retry = f.scan.useOnDevice() as ScanOutcome.Parsed
        assertEquals(2, retry.items.size)
        assertFalse(retry.offerAiRetry)
        assertTrue(f.ocr.lastJpeg!!.contentEquals(f.images.jpeg!!))
    }
}
