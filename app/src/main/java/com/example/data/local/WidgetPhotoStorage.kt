package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Play-Policy compliant private photo store.
 * Copies images selected via Android Photo Picker (zero storage permissions!)
 * into app-private filesDir as downscaled JPEGs so home screen widgets can render them reliably.
 */
object WidgetPhotoStorage {

    private const val MAX_DIMENSION = 640

    suspend fun importFromPhotoPickerUri(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            } ?: throw IllegalStateException("Could not open selected photo")

            val longest = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
            var sampleSize = 1
            while (longest / sampleSize > MAX_DIMENSION * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: throw IllegalStateException("Failed to decode selected photo")

            val scale = (MAX_DIMENSION.toFloat() / max(decoded.width, decoded.height).coerceAtLeast(1))
                .coerceAtMost(1f)
            val finalBitmap = if (scale < 0.99f) {
                val scaled = Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).toInt().coerceAtLeast(1),
                    (decoded.height * scale).toInt().coerceAtLeast(1),
                    true
                )
                decoded.recycle()
                scaled
            } else {
                decoded
            }

            val dir = File(context.filesDir, "widget_photos").apply { mkdirs() }
            val outFile = File(dir, "studio_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { fos ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 88, fos)
            }
            finalBitmap.recycle()
            outFile.absolutePath
        }
    }

    fun loadBitmap(path: String): Bitmap? {
        if (path.isBlank()) return null
        val file = File(path)
        if (!file.exists()) return null
        return runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
    }
}
