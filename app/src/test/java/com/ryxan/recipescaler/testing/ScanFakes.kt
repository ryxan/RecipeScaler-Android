package com.ryxan.recipescaler.testing

import android.app.Activity
import android.net.Uri
import com.ryxan.recipescaler.core.data.billing.ProProduct
import com.ryxan.recipescaler.core.data.billing.ProRepository
import com.ryxan.recipescaler.core.data.billing.PurchaseLaunch
import com.ryxan.recipescaler.core.data.importer.AiImportQuota
import com.ryxan.recipescaler.core.data.importer.AiImportResult
import com.ryxan.recipescaler.core.data.importer.AiRecipeImporter
import com.ryxan.recipescaler.core.data.importer.ImagePreprocessor
import com.ryxan.recipescaler.core.data.importer.OcrEngine
import com.ryxan.recipescaler.core.data.importer.OcrText
import com.ryxan.recipescaler.core.domain.importer.AiImportConfig
import com.ryxan.recipescaler.core.domain.importer.AiRecipe
import com.ryxan.recipescaler.core.domain.importer.AiIngredient
import com.ryxan.recipescaler.core.domain.importer.AiRecipeMapper
import com.ryxan.recipescaler.feature.importer.ScanImporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeOcrEngine : OcrEngine {
    /** Returned by every recognize call; null simulates recognition failure. */
    var next: OcrText? = OcrText(lines = emptyList(), text = "")
    var calls = 0
    var lastUri: Uri? = null
    var lastJpeg: ByteArray? = null
    /** Optional hook: suspend/inspect before returning [next]. */
    var onRecognize: (suspend () -> Unit)? = null

    override suspend fun recognize(uri: Uri): OcrText? {
        calls++
        lastUri = uri
        onRecognize?.invoke()
        return next
    }

    override suspend fun recognize(jpeg: ByteArray): OcrText? {
        calls++
        lastJpeg = jpeg
        onRecognize?.invoke()
        return next
    }
}

class FakeImagePreprocessor : ImagePreprocessor {
    /** Returned by every jpegForAi call; null simulates an undecodable image. */
    var jpeg: ByteArray? = byteArrayOf(1, 2, 3)
    var jpegCalls = 0
    var captureUri: Uri? = null
    val deleted = mutableListOf<Uri>()

    override fun newCaptureUri(): Uri? = captureUri
    override suspend fun jpegForAi(uri: Uri): ByteArray? {
        jpegCalls++
        return jpeg
    }
    override fun delete(uri: Uri) {
        deleted += uri
    }
}

class FakeAiRecipeImporter : AiRecipeImporter {
    val jpegs = mutableListOf<ByteArray>()
    var nextResult: AiImportResult = SUCCESS
    var onImport: (suspend () -> AiImportResult)? = null

    val callCount: Int get() = jpegs.size

    override suspend fun import(jpeg: ByteArray): AiImportResult {
        jpegs += jpeg
        return onImport?.invoke() ?: nextResult
    }

    companion object {
        val SUCCESS = AiImportResult.Success(
            recipe = AiRecipe(
                title = "Test Cake",
                servings = "8",
                ingredients = listOf(
                    AiIngredient(qty = "1", unit = "cup", name = "flour"),
                    AiIngredient(qty = "2", unit = "tbsp", name = "sugar"),
                ),
            ),
            items = AiRecipeMapper.map(
                AiRecipe(
                    ingredients = listOf(
                        AiIngredient(qty = "1", unit = "cup", name = "flour"),
                        AiIngredient(qty = "2", unit = "tbsp", name = "sugar"),
                    ),
                ),
            ),
        )
    }
}

/** In-memory quota honouring the same free-lifetime / bonus / Pro-daily rules. */
class FakeAiImportQuota : AiImportQuota {
    private val _remainingFree = MutableStateFlow(AiImportConfig.FREE_LIFETIME_LIMIT)
    override val remainingFree: Flow<Int> = _remainingFree

    private val _bonusCredits = MutableStateFlow(0)
    override val bonusCredits: Flow<Int> = _bonusCredits

    var freeUsed = 0
    var proUsed = 0
    var reserveCalls = 0
    var refundCalls = 0
    private var lastDebitBonus = false

    override suspend fun tryReserve(isPro: Boolean): Boolean {
        reserveCalls++
        return if (isPro) {
            if (proUsed < AiImportConfig.PREMIUM_DAILY_CAP) {
                proUsed++
                true
            } else {
                false
            }
        } else {
            when {
                freeUsed < AiImportConfig.FREE_LIFETIME_LIMIT -> {
                    freeUsed++
                    _remainingFree.value = AiImportConfig.FREE_LIFETIME_LIMIT - freeUsed
                    lastDebitBonus = false
                    true
                }
                _bonusCredits.value > 0 -> {
                    _bonusCredits.value--
                    lastDebitBonus = true
                    true
                }
                else -> false
            }
        }
    }

    override suspend fun refund(isPro: Boolean) {
        refundCalls++
        if (isPro) {
            proUsed = (proUsed - 1).coerceAtLeast(0)
        } else if (lastDebitBonus) {
            _bonusCredits.value++
        } else {
            freeUsed = (freeUsed - 1).coerceAtLeast(0)
            _remainingFree.value = AiImportConfig.FREE_LIFETIME_LIMIT - freeUsed
        }
    }

    override suspend fun grantCredits(count: Int) {
        _bonusCredits.value += count
    }

    override suspend fun debugSetRemaining(total: Int) {
        val n = total.coerceAtLeast(0)
        val freeLeft = minOf(n, AiImportConfig.FREE_LIFETIME_LIMIT)
        freeUsed = AiImportConfig.FREE_LIFETIME_LIMIT - freeLeft
        _remainingFree.value = freeLeft
        _bonusCredits.value = n - freeLeft
        lastDebitBonus = false
    }
}

class FakeProRepository(pro: Boolean = false) : ProRepository {
    private val _isPro = MutableStateFlow(pro)
    override val isPro: StateFlow<Boolean> = _isPro
    override val isPending: StateFlow<Boolean> = MutableStateFlow(false)
    override val products: StateFlow<List<ProProduct>> =
        MutableStateFlow(emptyList())

    private val _creditsGranted = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    override val creditsGranted: Flow<Int> = _creditsGranted

    suspend fun emitCreditsGranted(count: Int) = _creditsGranted.emit(count)

    var refreshResult = true
    var restoreResult = true
    var launchResult: PurchaseLaunch = PurchaseLaunch.Launched
    var refreshCalls = 0
    var restoreCalls = 0
    val launches = mutableListOf<String>()
    var debugProValue: Boolean? = null

    fun setPro(value: Boolean) {
        _isPro.value = value
    }

    fun setProducts(list: List<ProProduct>) {
        (products as MutableStateFlow).value = list
    }

    override suspend fun refreshProducts(): Boolean {
        refreshCalls++
        return refreshResult
    }

    override suspend fun launchPurchase(activity: Activity, productId: String): PurchaseLaunch {
        launches += productId
        return launchResult
    }

    override suspend fun restore(): Boolean {
        restoreCalls++
        return restoreResult
    }

    override fun setDebugPro(value: Boolean?) {
        debugProValue = value
        if (value != null) _isPro.value = value
    }
}

/** A [ScanImporter] backed by fakes; [aiAvailable] mirrors a Firebase-configured build. */
fun testScanImporter(
    ocr: FakeOcrEngine = FakeOcrEngine(),
    images: FakeImagePreprocessor = FakeImagePreprocessor(),
    importer: FakeAiRecipeImporter = FakeAiRecipeImporter(),
    quota: FakeAiImportQuota = FakeAiImportQuota(),
    pro: FakeProRepository = FakeProRepository(),
    aiAvailable: Boolean = true,
): ScanImporter = ScanImporter(ocr, images, importer, quota, pro, aiAvailable)
