package com.ryxan.recipescaler.core.data.importer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
// NATIVE graphics mode gives BitmapFactory a real decoder; the default
// LEGACY mode can't decode JPEG bytes.
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AndroidImagePreprocessorTest {

    private val preprocessor = AndroidImagePreprocessor(ApplicationProvider.getApplicationContext())

    @Test
    fun `jpegForAi decodes a real jpeg file into bounded bytes`() = runTest {
        // Encode a real JPEG on-device so the decoder gets standard bytes —
        // exotic minimal JPEGs are rejected even by real decoders.
        val source = Bitmap.createBitmap(80, 120, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.BLUE)
        }
        val imageFile = File(RuntimeEnvironment.getApplication().cacheDir, "ai-scan-test.jpg")
        imageFile.outputStream().use {
            assertTrue(source.compress(Bitmap.CompressFormat.JPEG, 90, it))
        }

        val jpeg = preprocessor.jpegForAi(Uri.fromFile(imageFile))

        assertNotNull(jpeg)
        assertTrue(jpeg!!.isNotEmpty())
        // The re-encoded image decodes again, within the 1600px edge bound.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, bounds)
        assertTrue(bounds.outWidth > 0 && bounds.outHeight > 0)
        assertTrue(maxOf(bounds.outWidth, bounds.outHeight) <= 1600)
    }

    @Test
    fun `jpegForAi returns null for garbage bytes`() = runTest {
        val imageFile = File(RuntimeEnvironment.getApplication().cacheDir, "not-an-image.bin")
        imageFile.writeBytes(byteArrayOf(1, 2, 3, 4, 5))

        assertNull(preprocessor.jpegForAi(Uri.fromFile(imageFile)))
    }

}
