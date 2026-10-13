package com.ryxan.recipescaler.feature.editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.ryxan.recipescaler.core.data.ads.NoopRewardedAds
import com.ryxan.recipescaler.core.data.importer.AiImportResult
import com.ryxan.recipescaler.core.data.importer.OcrText
import com.ryxan.recipescaler.core.domain.importer.AiImportConfig
import com.ryxan.recipescaler.feature.importer.ScanError
import com.ryxan.recipescaler.feature.recipes.UndoDeleteController
import com.ryxan.recipescaler.testing.FakeAiImportQuota
import com.ryxan.recipescaler.testing.FakeAiRecipeImporter
import com.ryxan.recipescaler.testing.FakeImagePreprocessor
import com.ryxan.recipescaler.testing.FakeOcrEngine
import com.ryxan.recipescaler.testing.FakeProRepository
import com.ryxan.recipescaler.testing.FakeRecipeRepository
import com.ryxan.recipescaler.testing.MainDispatcherRule
import com.ryxan.recipescaler.testing.testScanImporter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditorViewModelScanTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val uri: Uri = Uri.parse("content://test/scan.jpg")

    private class Fixture {
        val repo = FakeRecipeRepository()
        val ocr = FakeOcrEngine()
        val images = FakeImagePreprocessor()
        val importer = FakeAiRecipeImporter()
        val quota = FakeAiImportQuota()
        val pro = FakeProRepository()
        lateinit var vm: EditorViewModel
    }

    private fun newVm(
        f: Fixture,
        scope: kotlinx.coroutines.CoroutineScope,
        aiAvailable: Boolean = true,
    ): EditorViewModel = EditorViewModel(
        SavedStateHandle(),
        f.repo,
        UndoDeleteController(f.repo, scope),
        testScanImporter(f.ocr, f.images, f.importer, f.quota, f.pro, aiAvailable),
        scope,
        f.pro,
        NoopRewardedAds(),
    )

    @Test
    fun `scan on blank rows replaces silently`() = runTest {
        val f = Fixture()
        f.ocr.next = OcrText(emptyList(), "1 cup flour\n2 eggs")
        val vm = newVm(f, backgroundScope)

        vm.onImageCaptured(uri, deleteWhenDone = false)

        val s = vm.uiState.value
        assertNull(s.dialog)
        assertNull(s.importBusy)
        assertEquals(listOf("Flour", "Eggs"), s.rows.map { it.name })
    }

    @Test
    fun `scan on non-blank rows asks for confirmation`() = runTest {
        val f = Fixture()
        f.ocr.next = OcrText(emptyList(), "1 cup flour\n2 eggs")
        val vm = newVm(f, backgroundScope)
        vm.onRowNameChange(vm.uiState.value.rows[0].key, "butter")

        vm.onImageCaptured(uri, deleteWhenDone = false)

        val dialog = vm.uiState.value.dialog as? EditorDialog.ConfirmImport
        assertEquals(2, dialog?.items?.size)

        vm.confirmImportAppend()
        assertEquals(listOf("butter", "Flour", "Eggs"), vm.uiState.value.rows.map { it.name })
    }

    @Test
    fun `AI scan prefills blank title and yield only`() = runTest {
        val f = Fixture()
        val vm = newVm(f, backgroundScope)
        vm.setUseAiImport(true)

        vm.onImageCaptured(uri, deleteWhenDone = false)

        val s = vm.uiState.value
        assertEquals("Test Cake", s.title)
        assertEquals("8", s.yieldText)
        assertEquals(listOf("flour", "sugar"), s.rows.map { it.name })
    }

    @Test
    fun `AI prefill does not overwrite non-blank fields`() = runTest {
        val f = Fixture()
        val vm = newVm(f, backgroundScope)
        vm.setUseAiImport(true)
        vm.onTitleChange("My Recipe")
        vm.onYieldTextChange("4")

        vm.onImageCaptured(uri, deleteWhenDone = false)

        val s = vm.uiState.value
        assertEquals("My Recipe", s.title)
        assertEquals("4", s.yieldText)
    }

    @Test
    fun `low-confidence import sets needsReview cleared on edit`() = runTest {
        val f = Fixture()
        f.importer.nextResult = AiImportResult.Success(
            recipe = com.ryxan.recipescaler.core.domain.importer.AiRecipe(
                ingredients = listOf(
                    com.ryxan.recipescaler.core.domain.importer.AiIngredient(
                        qty = "some", unit = "cup", name = "flour",
                    ),
                ),
            ),
            items = com.ryxan.recipescaler.core.domain.importer.AiRecipeMapper.map(
                com.ryxan.recipescaler.core.domain.importer.AiRecipe(
                    ingredients = listOf(
                        com.ryxan.recipescaler.core.domain.importer.AiIngredient(
                            qty = "some", unit = "cup", name = "flour",
                        ),
                    ),
                ),
            ),
        )
        val vm = newVm(f, backgroundScope)
        vm.setUseAiImport(true)

        vm.onImageCaptured(uri, deleteWhenDone = false)

        val row = vm.uiState.value.rows[0]
        assertTrue(row.needsReview)

        vm.onRowNameChange(row.key, "flour 2")
        assertFalse(vm.uiState.value.rows[0].needsReview)
    }

    @Test
    fun `a second scan while busy is ignored`() = runTest {
        val f = Fixture()
        val gate = CompletableDeferred<Unit>()
        f.ocr.onRecognize = { gate.await() }
        f.ocr.next = OcrText(emptyList(), "1 cup flour\n2 eggs")
        val vm = newVm(f, backgroundScope)

        vm.onImageCaptured(uri, deleteWhenDone = false)
        // First scan is parked inside recognize — busy guard must hold.
        assertEquals(ImportBusy.ON_DEVICE, vm.uiState.value.importBusy)
        vm.onImageCaptured(Uri.parse("content://test/other.jpg"), deleteWhenDone = false)

        gate.complete(Unit)
        kotlinx.coroutines.yield()
        assertEquals(1, f.ocr.calls)
    }

    @Test
    fun `exhausted free credits open the AI paywall`() = runTest {
        val f = Fixture()
        repeat(AiImportConfig.FREE_LIFETIME_LIMIT) { f.quota.tryReserve(isPro = false) }
        val vm = newVm(f, backgroundScope)
        vm.setUseAiImport(true)

        vm.onImageCaptured(uri, deleteWhenDone = false)

        assertEquals(EditorDialog.AiPaywall, vm.uiState.value.dialog)
        assertEquals(0, f.importer.callCount)
    }

    @Test
    fun `AI failure opens ScanFailed with fallback flags`() = runTest {
        val f = Fixture()
        f.importer.nextResult = AiImportResult.NetworkError
        val vm = newVm(f, backgroundScope)
        vm.setUseAiImport(true)

        vm.onImageCaptured(uri, deleteWhenDone = false)

        val dialog = vm.uiState.value.dialog as? EditorDialog.ScanFailed
        assertEquals(ScanError.AI_NETWORK_ERROR, dialog?.error)
        assertTrue(dialog?.canUseOnDevice == true)
        assertFalse(dialog?.canRetryWithAi == true)
    }

    @Test
    fun `poor OCR result applies then fires ScanLooksPoor`() = runTest {
        val f = Fixture()
        f.ocr.next = OcrText(emptyList(), "1 cup flour") // single row → poor
        val vm = newVm(f, backgroundScope)

        vm.onImageCaptured(uri, deleteWhenDone = false)

        assertEquals(1, vm.uiState.value.rows.size)
        assertEquals(EditorEvent.ScanLooksPoor, vm.events.first())
    }
}
