package com.ryxan.recipescaler.feature.importer

import android.net.Uri
import com.ryxan.recipescaler.core.data.billing.ProRepository
import com.ryxan.recipescaler.core.data.importer.AiImportQuota
import com.ryxan.recipescaler.core.data.importer.AiImportResult
import com.ryxan.recipescaler.core.data.importer.AiRecipeImporter
import com.ryxan.recipescaler.core.data.importer.ImagePreprocessor
import com.ryxan.recipescaler.core.data.importer.OcrEngine
import com.ryxan.recipescaler.core.domain.model.ParsedIngredient
import com.ryxan.recipescaler.core.domain.parsing.RecipeOcrParser
import com.ryxan.recipescaler.feature.editor.MAX_INGREDIENTS
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

sealed interface ScanOutcome {
    data class Parsed(
        val items: List<ParsedIngredient>,
        val title: String?,
        val servings: String?,
        val offerAiRetry: Boolean,
    ) : ScanOutcome

    data class Failed(
        val error: ScanError,
        val canRetryWithAi: Boolean,
        val canUseOnDevice: Boolean,
    ) : ScanOutcome

    data object PaywallRequired : ScanOutcome
}

/** Sealed type for the distinguishable scan failure modes. */
enum class ScanError {
    /** OCR ran successfully but the parser found no ingredient lines. */
    NO_INGREDIENTS_FOUND,

    /** The image couldn't be decoded or OCR itself failed. */
    RECOGNITION_FAILED,

    AI_NETWORK_ERROR,
    AI_BLOCKED,
    AI_FAILED,
    AI_LIMIT_REACHED,
    AI_RATE_LIMITED,
}

/**
 * Runs one scan: on-device OCR or the AI importer, with quota bookkeeping and
 * a JPEG cached for "Try AI import" / "Scan on device" retries.
 *
 * Not a singleton — one instance per EditorViewModel, so the cached JPEG is
 * scoped to the edit session. A free-import credit is spent only on a
 * successful AI import; every other outcome (including cancellation) refunds.
 */
class ScanImporter @Inject constructor(
    private val ocrEngine: OcrEngine,
    private val images: ImagePreprocessor,
    private val aiImporter: AiRecipeImporter,
    private val quota: AiImportQuota,
    private val proRepository: ProRepository,
    @param:Named("aiImportAvailable") val aiAvailable: Boolean,
) {
    /** Imports a free user can still run — free allowance plus bonus credits. */
    val remainingImports = combine(quota.remainingFree, quota.bonusCredits) { free, bonus ->
        free + bonus
    }
    val isPro get() = proRepository.isPro

    /** JPEG prepared for the AI importer, cached for retries; cleared per scan. */
    internal var cachedJpeg: ByteArray? = null

    suspend fun scan(uri: Uri, useAi: Boolean, deleteWhenDone: Boolean): ScanOutcome {
        clear()
        try {
            return if (useAi && aiAvailable) {
                scanWithAi(uri)
            } else {
                scanOnDevice(uri)
            }
        } finally {
            if (deleteWhenDone) images.delete(uri)
        }
    }

    /** "Try AI import" after a failed or poor on-device scan; null without a cached JPEG. */
    suspend fun retryWithAi(): ScanOutcome? {
        if (!aiAvailable) return null
        val jpeg = cachedJpeg ?: return null
        return runAi(jpeg)
    }

    /**
     * "Scan on device" after an AI network error — OCR on the cached JPEG,
     * never offers an AI retry again.
     */
    suspend fun useOnDevice(): ScanOutcome? {
        val jpeg = cachedJpeg ?: return null
        val ocr = ocrEngine.recognize(jpeg)
        return ocrToOutcome(ocr, uri = null)
    }

    fun clear() {
        cachedJpeg = null
    }

    // ── On-device path ────────────────────────────────────────────────────────

    private suspend fun scanOnDevice(uri: Uri): ScanOutcome {
        val ocr = ocrEngine.recognize(uri)
        return ocrToOutcome(ocr, uri)
    }

    private suspend fun ocrToOutcome(
        ocr: com.ryxan.recipescaler.core.data.importer.OcrText?,
        uri: Uri?,
    ): ScanOutcome {
        if (ocr == null) {
            if (uri != null) cacheJpeg(uri)
            return ScanOutcome.Failed(
                ScanError.RECOGNITION_FAILED,
                canRetryWithAi = aiAvailable && cachedJpeg != null,
                canUseOnDevice = false,
            )
        }
        // Parse past the cap so truncation can be detected, like importText.
        val parsed = if (ocr.lines.isNotEmpty()) {
            RecipeOcrParser.parseLines(ocr.lines, MAX_INGREDIENTS * 4)
        } else {
            RecipeOcrParser.parse(ocr.text, MAX_INGREDIENTS * 4)
        }
        if (parsed.isEmpty()) {
            if (uri != null) cacheJpeg(uri)
            return ScanOutcome.Failed(
                ScanError.NO_INGREDIENTS_FOUND,
                canRetryWithAi = aiAvailable && cachedJpeg != null,
                canUseOnDevice = false,
            )
        }
        val poor = parsed.size < POOR_RESULT_MIN_ITEMS ||
            parsed.count { it.lowConfidence } * 2 > parsed.size
        if (poor && uri != null) cacheJpeg(uri)
        return ScanOutcome.Parsed(
            items = parsed,
            title = null,
            servings = null,
            offerAiRetry = poor && aiAvailable && cachedJpeg != null,
        )
    }

    /** Re-encode [uri] for the AI importer once per scan, for the retry cache. */
    private suspend fun cacheJpeg(uri: Uri) {
        if (aiAvailable && cachedJpeg == null) {
            cachedJpeg = images.jpegForAi(uri)
        }
    }

    // ── AI path ───────────────────────────────────────────────────────────────

    private suspend fun scanWithAi(uri: Uri): ScanOutcome {
        val jpeg = images.jpegForAi(uri)
            ?: return ScanOutcome.Failed(
                ScanError.RECOGNITION_FAILED,
                canRetryWithAi = false,
                canUseOnDevice = false,
            )
        cachedJpeg = jpeg
        return runAi(jpeg)
    }

    /**
     * Reserve → import → outcome for an already-prepared JPEG. A credit is
     * spent only on success — every other outcome (including coroutine
     * cancellation) refunds exactly once.
     */
    private suspend fun runAi(jpeg: ByteArray): ScanOutcome {
        val isPro = proRepository.isPro.value
        val reserved = quota.tryReserve(isPro)
        if (!reserved) {
            return if (isPro) {
                ScanOutcome.Failed(ScanError.AI_LIMIT_REACHED, canRetryWithAi = false, canUseOnDevice = false)
            } else {
                ScanOutcome.PaywallRequired
            }
        }
        var refunded = false
        suspend fun refundOnce() {
            if (!refunded) {
                refunded = true
                quota.refund(isPro)
            }
        }
        try {
            return when (val result = aiImporter.import(jpeg)) {
                is AiImportResult.Success -> ScanOutcome.Parsed(
                    items = result.items,
                    title = result.recipe.title,
                    servings = result.recipe.servings,
                    offerAiRetry = false,
                )
                else -> {
                    refundOnce()
                    ScanOutcome.Failed(
                        result.toScanError(),
                        canRetryWithAi = false,
                        canUseOnDevice = result == AiImportResult.NetworkError && cachedJpeg != null,
                    )
                }
            }
        } catch (e: CancellationException) {
            withContext(NonCancellable) { refundOnce() }
            throw e
        } catch (e: Exception) {
            refundOnce()
            return ScanOutcome.Failed(ScanError.AI_FAILED, canRetryWithAi = false, canUseOnDevice = false)
        }
    }

    private fun AiImportResult.toScanError(): ScanError = when (this) {
        AiImportResult.NothingFound -> ScanError.NO_INGREDIENTS_FOUND
        AiImportResult.NetworkError -> ScanError.AI_NETWORK_ERROR
        AiImportResult.Blocked -> ScanError.AI_BLOCKED
        AiImportResult.RateLimited -> ScanError.AI_RATE_LIMITED
        is AiImportResult.Failed -> ScanError.AI_FAILED
        is AiImportResult.Success -> ScanError.AI_FAILED // unreachable — handled earlier
    }

    private companion object {
        /**
         * An OCR parse with fewer rows than this — or one where over half the
         * rows are low-confidence — is treated as a poor result and gets a
         * "Try AI import" offer.
         */
        const val POOR_RESULT_MIN_ITEMS = 2
    }
}
