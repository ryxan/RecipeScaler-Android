package com.ryxan.recipescaler.core.data.importer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Log
import com.ryxan.recipescaler.core.data.BuildConfig
import com.ryxan.recipescaler.core.domain.importer.AiImportConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Prepares and cleans up images for the scan / AI-import flows. */
interface ImagePreprocessor {
    /** Decode [uri] and re-encode as a bounded JPEG for the AI importer; null on failure. */
    suspend fun jpegForAi(uri: Uri): ByteArray?

    /** Deletes a temp scan file (file://). Never throws. */
    fun delete(uri: Uri)
}

/** Image decoding/scaling shared by the document-scanner and gallery paths. */
@Singleton
class AndroidImagePreprocessor @Inject constructor(
    @ApplicationContext private val context: Context,
) : ImagePreprocessor {

    /**
     * Decode [uri] and re-encode as a bounded JPEG for the AI importer:
     * [BitmapFactory.Options.inSampleSize] bounds decode → EXIF rotation →
     * scale so the longest edge is at most [AiImportConfig.MAX_IMAGE_EDGE_PX] →
     * JPEG at [AiImportConfig.JPEG_QUALITY] quality. Returns null when the
     * source can't be decoded.
     */
    override suspend fun jpegForAi(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        try {
            // ImageDecoder (API 28+) handles EXIF rotation and formats
            // BitmapFactory can't decode from non-seekable streams (HEIF).
            // BitmapFactory + ExifInterface is the fallback below API 28 and
            // for sources ImageDecoder rejects.
            val raw = decodeViaImageDecoder(uri)
                ?: decodeViaBitmapFactory(uri)?.let { applyExifRotation(uri, it) }
            if (raw == null) {
                if (BuildConfig.DEBUG) Log.w(TAG, "jpegForAi: undecodable image $uri")
                return@withContext null
            }

            val scaled = scaleToMaxEdge(raw, AiImportConfig.MAX_IMAGE_EDGE_PX)
            ByteArrayOutputStream().use { out ->
                if (!scaled.compress(Bitmap.CompressFormat.JPEG, AiImportConfig.JPEG_QUALITY, out)) {
                    if (BuildConfig.DEBUG) Log.w(TAG, "jpegForAi: JPEG compress failed $uri")
                    return@withContext null
                }
                out.toByteArray()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "jpegForAi failed", e)
            null
        }
    }

    override fun delete(uri: Uri) {
        try {
            if (uri.scheme == "file") uri.path?.let { File(it).delete() }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "delete failed for $uri", e)
        }
    }

    /**
     * Decode via [ImageDecoder] (API 28+). Applies EXIF orientation itself —
     * do not run [applyExifRotation] on the result.
     */
    private fun decodeViaImageDecoder(uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = false
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "ImageDecoder failed for $uri", e)
            null
        }
    }

    /**
     * Decode via [BitmapFactory] with bounds pass + inSampleSize. EXIF is
     * NOT applied — callers must run [applyExifRotation] on the result.
     */
    private suspend fun decodeViaBitmapFactory(uri: Uri): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val bytes = readBytes(uri) ?: return@withContext null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= AiImportConfig.MAX_IMAGE_EDGE_PX) {
                    sample *= 2
                }
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w(TAG, "BitmapFactory decode failed for $uri", e)
                null
            }
        }

    /** Read [uri]'s bytes; falls back to the filesystem for file:// URIs. */
    private fun readBytes(uri: Uri): ByteArray? = try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: if (uri.scheme == "file") {
                // Paths.get(URI) handles "file:///C:/..." correctly on Windows;
                // Uri.path alone would give "/C:\...", which isn't a valid File path.
                runCatching { Files.readAllBytes(Paths.get(URI(uri.toString()))) }.getOrNull()
            } else {
                null
            }
    } catch (e: Exception) {
        null
    }

    private suspend fun applyExifRotation(uri: Uri, bitmap: Bitmap): Bitmap =
        withContext(Dispatchers.IO) {
            val orientation = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            // A malformed/absent EXIF block shouldn't fail the whole conversion.
            if (BuildConfig.DEBUG) Log.w(TAG, "EXIF read failed for $uri", e)
            ExifInterface.ORIENTATION_NORMAL
        }
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (degrees == 0f) {
                bitmap
            } else {
                Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height,
                    Matrix().apply { postRotate(degrees) }, true,
                )
            }
        }

    private fun scaleToMaxEdge(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    private companion object {
        const val TAG = "RecipeScaler"
    }
}
