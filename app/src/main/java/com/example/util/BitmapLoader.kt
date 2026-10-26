package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Decodes images the way the user sees them: EXIF orientation applied (camera photos are often stored
 * sideways) and downsampled while decoding so big photos never blow up the heap.
 */
object BitmapLoader {

    data class Bounds(val width: Int, val height: Int)

    /** Size of the image after EXIF rotation, without decoding pixels. */
    fun readBounds(context: Context, uri: Uri): Bounds? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        }
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
        val rotation = readExif(context, uri).first
        return if (rotation == 90 || rotation == 270) Bounds(opts.outHeight, opts.outWidth) else Bounds(opts.outWidth, opts.outHeight)
    }

    /** Rotation in degrees and whether the image is mirrored, read from EXIF (0/false when missing). */
    fun readExif(context: Context, uri: Uri): Pair<Int, Boolean> = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            exif.rotationDegrees to exif.isFlipped
        }
    }.getOrNull() ?: (0 to false)

    /**
     * Decodes [uri] so that its longest side is at most [maxDimension] px, with EXIF orientation applied.
     * Retries at half the size if the device runs out of memory.
     */
    fun decode(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        var target = maxDimension
        repeat(3) {
            try {
                return decodeInternal(context, uri, target)
            } catch (oom: OutOfMemoryError) {
                System.gc()
                target /= 2
            }
        }
        return null
    }

    private fun decodeInternal(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDimension) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: return null

        val (rotation, flipped) = readExif(context, uri)
        val longest = max(decoded.width, decoded.height)
        val scale = if (longest > maxDimension) maxDimension.toFloat() / longest else 1f
        if (rotation == 0 && !flipped && scale == 1f) return decoded

        val matrix = Matrix().apply {
            if (scale != 1f) postScale(scale, scale)
            if (flipped) postScale(-1f, 1f)
            if (rotation != 0) postRotate(rotation.toFloat())
        }
        val transformed = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (transformed != decoded) decoded.recycle()
        return transformed
    }

    /** Rotates by a multiple of 90 degrees, recycling the source when a new bitmap is created. */
    fun rotate(source: Bitmap, degrees: Int): Bitmap {
        val normalized = ((degrees % 360) + 360) % 360
        if (normalized == 0) return source
        val matrix = Matrix().apply { postRotate(normalized.toFloat()) }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (rotated != source) source.recycle()
        return rotated
    }

    /** Crops using fractions of width/height (0..1), recycling the source when a new bitmap is created. */
    fun cropFraction(source: Bitmap, left: Float, top: Float, right: Float, bottom: Float): Bitmap {
        val l = (left.coerceIn(0f, 1f) * source.width).roundToInt().coerceIn(0, source.width - 1)
        val t = (top.coerceIn(0f, 1f) * source.height).roundToInt().coerceIn(0, source.height - 1)
        val r = (right.coerceIn(0f, 1f) * source.width).roundToInt()
        val b = (bottom.coerceIn(0f, 1f) * source.height).roundToInt()
        val w = (r - l).coerceIn(1, source.width - l)
        val h = (b - t).coerceIn(1, source.height - t)
        if (l == 0 && t == 0 && w == source.width && h == source.height) return source
        val cropped = Bitmap.createBitmap(source, l, t, w, h)
        if (cropped != source) source.recycle()
        return cropped
    }

    /** Draws the bitmap on an opaque white background (JPEG has no alpha: transparent areas would turn black). */
    fun flattenOnWhite(source: Bitmap, paint: Paint? = null): Bitmap {
        if (!source.hasAlpha() && paint == null) return source
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(out).apply {
            drawColor(Color.WHITE)
            drawBitmap(source, 0f, 0f, paint ?: Paint(Paint.FILTER_BITMAP_FLAG))
        }
        source.recycle()
        return out
    }

    fun scaleDown(source: Bitmap, maxDimension: Int): Bitmap {
        val longest = max(source.width, source.height)
        if (longest <= maxDimension) return source
        val scale = maxDimension.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            source,
            (source.width * scale).roundToInt().coerceAtLeast(1),
            (source.height * scale).roundToInt().coerceAtLeast(1),
            true
        )
        if (scaled != source) source.recycle()
        return scaled
    }
}
