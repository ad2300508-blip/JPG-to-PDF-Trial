package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import com.example.util.BitmapLoader
import com.example.util.FileNames
import com.example.util.GifPalette
import com.example.util.GifWriter
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.util.resultOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

enum class GifFit(val label: String) {
    FIT("Adatta"),
    FILL("Riempi")
}

data class GifSettings(
    /** How long each photo stays on screen. */
    val frameMs: Int = 600,
    /** Longest side of the GIF in px. */
    val maxSide: Int = 480,
    val fit: GifFit = GifFit.FILL,
    /** Plays forward then backward (A B C B A …) for a smooth loop. */
    val boomerang: Boolean = false,
    val loopForever: Boolean = true
) {
    companion object {
        val SIZES = listOf(320, 480, 720)
    }
}

object GifSequence {
    /** Frame order actually played: forward, plus the way back without repeating the ends. */
    fun order(count: Int, boomerang: Boolean): List<Int> {
        if (count <= 0) return emptyList()
        val forward = (0 until count).toList()
        return if (boomerang && count > 2) forward + (count - 2 downTo 1) else forward
    }

    /** Canvas size: the first photo's proportions (clamped to 1:4..4:1), longest side [maxSide]. */
    fun canvas(firstAspect: Float, maxSide: Int): Pair<Int, Int> {
        val aspect = if (firstAspect.isFinite() && firstAspect > 0f) firstAspect.coerceIn(0.25f, 4f) else 1f
        return if (aspect >= 1f) {
            maxSide to (maxSide / aspect).roundToInt().coerceAtLeast(2)
        } else {
            (maxSide * aspect).roundToInt().coerceAtLeast(2) to maxSide
        }
    }
}

class GifService(private val context: Context) {

    suspend fun render(
        uris: List<Uri>,
        firstAspect: Float,
        settings: GifSettings,
        onProgress: (Float) -> Unit = {}
    ): Result<Pair<Uri, Long>> = withContext(Dispatchers.IO) {
        resultOf {
            require(uris.size >= 2) { "Servono almeno 2 foto" }
            val (w, h) = GifSequence.canvas(firstAspect, settings.maxSide)
            // Quantise each photo once; the boomerang half reuses the same frames.
            val frames = ArrayList<ByteArray>(uris.size)
            val canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(w * h)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            try {
                val canvas = Canvas(canvasBitmap)
                uris.forEachIndexed { index, uri ->
                    ensureActive()
                    canvas.drawColor(android.graphics.Color.WHITE)
                    val photo = BitmapLoader.decode(context, uri, max(w, h) * 2)
                    if (photo != null) {
                        val (src, dst) = placement(photo.width, photo.height, w, h, settings.fit)
                        canvas.drawBitmap(photo, src, dst, paint)
                        photo.recycle()
                    }
                    canvasBitmap.getPixels(pixels, 0, w, 0, 0, w, h)
                    frames += GifPalette.quantize(pixels, w)
                    onProgress((index + 1f) / uris.size * 0.6f)
                }
            } finally {
                canvasBitmap.recycle()
            }

            // Encode to a cache file first: big GIFs never sit fully in memory twice.
            val temp = File.createTempFile("morph_gif", ".gif", context.cacheDir)
            try {
                val order = GifSequence.order(frames.size, settings.boomerang)
                temp.outputStream().buffered().use { stream ->
                    val writer = GifWriter(stream, w, h, settings.loopForever)
                    order.forEachIndexed { i, frame ->
                        ensureActive()
                        writer.addFrame(frames[frame], settings.frameMs)
                        onProgress(0.6f + (i + 1f) / order.size * 0.35f)
                    }
                    writer.finish()
                }
                val name = FileNames.stamped("Animazione", "Animazione", "gif")
                val uri = OutputStorage.save(context, name, "image/gif", OutputFolder.IMAGES) { out ->
                    temp.inputStream().use { it.copyTo(out) }
                }
                onProgress(1f)
                uri to temp.length()
            } finally {
                temp.delete()
            }
        }
    }

    /** FIT letterboxes the whole photo; FILL crops its middle to cover the canvas. */
    private fun placement(pw: Int, ph: Int, w: Int, h: Int, fit: GifFit): Pair<Rect, RectF> {
        val photoAspect = pw.toFloat() / ph
        val canvasAspect = w.toFloat() / h
        return if (fit == GifFit.FILL) {
            val src = if (photoAspect > canvasAspect) {
                val cw = (ph * canvasAspect).roundToInt().coerceIn(1, pw)
                Rect((pw - cw) / 2, 0, (pw - cw) / 2 + cw, ph)
            } else {
                val ch = (pw / canvasAspect).roundToInt().coerceIn(1, ph)
                Rect(0, (ph - ch) / 2, pw, (ph - ch) / 2 + ch)
            }
            src to RectF(0f, 0f, w.toFloat(), h.toFloat())
        } else {
            val scale = minOf(w.toFloat() / pw, h.toFloat() / ph)
            val dw = pw * scale
            val dh = ph * scale
            Rect(0, 0, pw, ph) to RectF((w - dw) / 2f, (h - dh) / 2f, (w + dw) / 2f, (h + dh) / 2f)
        }
    }
}
