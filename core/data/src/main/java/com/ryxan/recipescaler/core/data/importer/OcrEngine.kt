package com.ryxan.recipescaler.core.data.importer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ryxan.recipescaler.core.data.BuildConfig
import com.ryxan.recipescaler.core.domain.parsing.OcrLine
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Recognised text from an image scan. [lines] carries positioned OCR lines for
 * the layout-aware parser; it is empty when any line lacks a bounding box, in
 * which case callers should parse [text] instead.
 */
data class OcrText(val lines: List<OcrLine>, val text: String)

/** On-device text recognition; null means recognition failed. */
interface OcrEngine {
    suspend fun recognize(uri: Uri): OcrText?
    suspend fun recognize(jpeg: ByteArray): OcrText?
}

/**
 * ML Kit Latin text recognizer. One lazy client per app lifetime — creating a
 * recognizer per scan is expensive.
 */
@Singleton
class MlKitOcrEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : OcrEngine {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun recognize(uri: Uri): OcrText? {
        var bitmap: Bitmap? = null
        return try {
            val (image, decoded) = loadScanImage(uri)
            bitmap = decoded
            runRecognition(image)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "OCR failed for $uri", e)
            null
        } finally {
            bitmap?.recycle()
        }
    }

    override suspend fun recognize(jpeg: ByteArray): OcrText? {
        var bitmap: Bitmap? = null
        return try {
            bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) ?: return null
            runRecognition(InputImage.fromBitmap(bitmap, 0))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "OCR failed for cached jpeg", e)
            null
        } finally {
            bitmap?.recycle()
        }
    }

    private suspend fun runRecognition(image: InputImage): OcrText {
        val visionText = recognizer.process(image).await()
        // Raw text goes to logcat so mis-parse reports can be diagnosed against
        // what ML Kit actually returned rather than the cleaned-up result.
        if (BuildConfig.DEBUG) Log.d(TAG, "OCR raw text:\n${visionText.text}")
        val rawLines = visionText.textBlocks.flatMap { it.lines }
        val lines = if (rawLines.isNotEmpty() && rawLines.all { it.boundingBox != null }) {
            rawLines.map { line ->
                val box = line.boundingBox!!
                OcrLine(
                    text = line.text,
                    left = box.left,
                    top = box.top,
                    right = box.right,
                    bottom = box.bottom,
                    confidence = line.confidence,
                )
            }
        } else {
            emptyList()
        }
        return OcrText(lines = lines, text = visionText.text)
    }

    /**
     * Decodes [uri] into an [InputImage], downscaling so the longest edge is at
     * most [MAX_SCAN_EDGE_PX]. Image decoding runs on [Dispatchers.IO].
     *
     * On API 28+ [ImageDecoder] applies EXIF orientation itself, so the bitmap
     * is already upright and [InputImage] gets rotation 0. On API 26–27 we fall
     * back to [InputImage.fromFilePath], which also handles EXIF.
     *
     * Returns the [InputImage] paired with the decoded [Bitmap] (non-null only
     * on the ImageDecoder path) so the caller can recycle it after recognition.
     */
    private suspend fun loadScanImage(uri: Uri): Pair<InputImage, Bitmap?> =
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val bitmap = ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(context.contentResolver, uri),
                ) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val w = info.size.width
                    val h = info.size.height
                    val longest = maxOf(w, h)
                    if (longest > MAX_SCAN_EDGE_PX) {
                        val scale = MAX_SCAN_EDGE_PX.toFloat() / longest
                        decoder.setTargetSize(
                            (w * scale).toInt().coerceAtLeast(1),
                            (h * scale).toInt().coerceAtLeast(1),
                        )
                    }
                }
                InputImage.fromBitmap(bitmap, 0) to bitmap
            } else {
                InputImage.fromFilePath(context, uri) to null
            }
        }

    private companion object {
        const val TAG = "RecipeScaler"

        /** Longest-edge pixel cap for scan images; large photos waste memory/OCR time. */
        const val MAX_SCAN_EDGE_PX = 2048
    }
}
