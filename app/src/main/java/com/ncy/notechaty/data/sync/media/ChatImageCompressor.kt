package com.ncy.notechaty.data.sync.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Log
import java.io.File
import kotlin.math.max

/**
 * Shrinks a chat picture or background before it is stored and synced. A chat
 * picture is shown at most a few hundred pixels wide, so a 12 MP camera photo
 * is wasted space on every paired device.
 *
 * The result is always a JPEG: turned upright from its EXIF orientation, with
 * transparency flattened onto white and the metadata (including location)
 * dropped. If the image can't be decoded, the original is kept as it is.
 */
object ChatImageCompressor {
    const val AVATAR_MAX_PX = 512
    const val BACKGROUND_MAX_PX = 1920
    private const val QUALITY = 85

    /** Writes a compressed copy of [source] to [target]; returns false if [source] couldn't be read as an image. */
    fun compress(source: File, target: File, maxPx: Int): Boolean {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false

        // Decode at the smallest power-of-two size that is still at least maxPx, then scale exactly.
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
        val decoded = BitmapFactory.decodeFile(source.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return false

        val scale = minOf(1f, maxPx.toFloat() / max(decoded.width, decoded.height))
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotationOf(source))
        }
        val transformed = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (transformed !== decoded) decoded.recycle()

        // JPEG has no alpha: a transparent PNG would otherwise turn black.
        val opaque = Bitmap.createBitmap(transformed.width, transformed.height, Bitmap.Config.ARGB_8888)
        Canvas(opaque).apply {
            drawColor(Color.WHITE)
            drawBitmap(transformed, 0f, 0f, null)
        }
        transformed.recycle()

        return try {
            target.outputStream().use { opaque.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
        } finally {
            opaque.recycle()
        }
    }

    private fun rotationOf(file: File): Float = try {
        when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } catch (e: Exception) {
        Log.w("ChatImageCompressor", "couldn't read orientation", e)
        0f
    }
}
