package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import com.example.util.BitmapLoader
import com.example.util.FileNames
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.util.PhotoMath
import com.example.util.SocialAspect
import com.example.util.WatermarkPosition
import com.example.util.resultOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class SocialBackground(val label: String) { BLUR("Sfocato"), COLOR("Tinta unita") }

data class SocialSettings(
    val aspect: SocialAspect = SocialAspect.SQUARE,
    val background: SocialBackground = SocialBackground.BLUR,
    /** Background colour when [background] is COLOR (ARGB). */
    val color: Long = 0xFFFFFFFF,
    /** Frame around the photo, as a fraction of the canvas' short side (0..0.15). */
    val border: Float = 0.04f,
    /** Rounded corners of the photo, as a fraction of its short side (0..0.2). */
    val corner: Float = 0f
) {
    companion object {
        const val LONG_SIDE = 1920
        val COLORS = listOf(0xFFFFFFFF, 0xFF000000, 0xFFF4EEE2, 0xFF1E2A44, 0xFFFFD6E0, 0xFFD8F3DC)
    }
}

data class WatermarkSettings(
    val text: String = "",
    val position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
    /** 0..1 */
    val opacity: Float = 0.6f,
    /** Text height as a fraction of the photo's short side (0.02..0.15). */
    val size: Float = 0.05f,
    /** White text (with a soft shadow) or black text. */
    val light: Boolean = true
)

data class RotateSettings(val degrees: Int = 0, val flipHorizontal: Boolean = false, val flipVertical: Boolean = false) {
    val isIdentity: Boolean get() = ((degrees % 360) + 360) % 360 == 0 && !flipHorizontal && !flipVertical
}

/** On-device edits for whole batches of photos; every result is a new JPEG (or PNG) in Images. */
class PhotoEditService(private val context: Context) {

    suspend fun social(uri: Uri, name: String, settings: SocialSettings): Result<Pair<Uri, Long>> = withContext(Dispatchers.IO) {
        resultOf {
            val photo = BitmapLoader.decode(context, uri, SocialSettings.LONG_SIDE) ?: error("Immagine non leggibile")
            val (w, h) = PhotoMath.canvasFor(settings.aspect, SocialSettings.LONG_SIDE)
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            try {
                val canvas = Canvas(out)
                val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                if (settings.background == SocialBackground.BLUR) {
                    drawBlurredCover(canvas, photo, w, h)
                } else {
                    canvas.drawColor(settings.color.toInt())
                }
                val inset = min(w, h) * settings.border.coerceIn(0f, 0.15f)
                val r = PhotoMath.contain(photo.width.toFloat(), photo.height.toFloat(), w.toFloat(), h.toFloat(), inset)
                val dst = RectF(r.x, r.y, r.x + r.w, r.y + r.h)
                val radius = min(r.w, r.h) * settings.corner.coerceIn(0f, 0.2f)
                canvas.save()
                if (radius > 0f) canvas.clipPath(Path().apply { addRoundRect(dst, radius, radius, Path.Direction.CW) })
                canvas.drawBitmap(photo, null, dst, paint)
                canvas.restore()
                save(out, "${FileNames.baseName(name)}_${settings.aspect.label.replace(':', 'x')}", png = false)
            } finally {
                photo.recycle()
                out.recycle()
            }
        }
    }

    suspend fun watermark(uri: Uri, name: String, settings: WatermarkSettings): Result<Pair<Uri, Long>> = withContext(Dispatchers.IO) {
        resultOf {
            require(settings.text.isNotBlank()) { "Scrivi il testo della filigrana" }
            val photo = BitmapLoader.decode(context, uri, MAX_SIDE) ?: error("Immagine non leggibile")
            val out = photo.copy(Bitmap.Config.ARGB_8888, true)
            photo.recycle()
            try {
                drawWatermark(Canvas(out), out.width.toFloat(), out.height.toFloat(), settings)
                save(out, "${FileNames.baseName(name)}_filigrana", png = false)
            } finally {
                out.recycle()
            }
        }
    }

    suspend fun rotate(uri: Uri, name: String, settings: RotateSettings): Result<Pair<Uri, Long>> = withContext(Dispatchers.IO) {
        resultOf {
            val photo = BitmapLoader.decode(context, uri, MAX_SIDE) ?: error("Immagine non leggibile")
            val matrix = Matrix().apply {
                postRotate(settings.degrees.toFloat())
                postScale(if (settings.flipHorizontal) -1f else 1f, if (settings.flipVertical) -1f else 1f)
            }
            val out = Bitmap.createBitmap(photo, 0, 0, photo.width, photo.height, matrix, true)
            try {
                val png = name.endsWith(".png", ignoreCase = true)
                save(out, "${FileNames.baseName(name)}_ruotata", png = png)
            } finally {
                if (out != photo) out.recycle()
                photo.recycle()
            }
        }
    }

    /** A shareable card: one column per colour with its hex code. */
    suspend fun savePalette(colors: List<Int>, name: String): Result<Pair<Uri, Long>> = withContext(Dispatchers.IO) {
        resultOf {
            require(colors.isNotEmpty()) { "Nessun colore da salvare" }
            val w = 1200
            val h = 600
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            try {
                val canvas = Canvas(out)
                canvas.drawColor(android.graphics.Color.WHITE)
                val column = w.toFloat() / colors.size
                val fill = Paint(Paint.ANTI_ALIAS_FLAG)
                val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 34f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                colors.forEachIndexed { i, color ->
                    fill.color = color
                    canvas.drawRect(i * column, 0f, (i + 1) * column, h - 90f, fill)
                    text.color = android.graphics.Color.DKGRAY
                    canvas.drawText(PhotoMath.hex(color), i * column + column / 2f, h - 35f, text)
                }
                save(out, "${FileNames.baseName(name)}_palette", png = true)
            } finally {
                out.recycle()
            }
        }
    }

    /** Small decode for the palette (colour statistics don't need detail). */
    suspend fun samplePixels(uri: Uri, side: Int = 96): IntArray? = withContext(Dispatchers.IO) {
        val bitmap = BitmapLoader.decode(context, uri, side) ?: return@withContext null
        try {
            IntArray(bitmap.width * bitmap.height).also { bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height) }
        } finally {
            bitmap.recycle()
        }
    }

    /** "Cover" the canvas with the photo, then blur it by shrinking and enlarging with filtering. */
    private fun drawBlurredCover(canvas: Canvas, photo: Bitmap, w: Int, h: Int) {
        val cover = PhotoMath.cover(photo.width.toFloat(), photo.height.toFloat(), w.toFloat(), h.toFloat())
        val tinyW = max(1, w / 48)
        val tinyH = max(1, h / 48)
        val tiny = Bitmap.createBitmap(tinyW, tinyH, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        Canvas(tiny).apply {
            val s = tinyW.toFloat() / w
            drawBitmap(photo, null, RectF(cover.x * s, cover.y * s, (cover.x + cover.w) * s, (cover.y + cover.h) * s), paint)
        }
        val mid = Bitmap.createScaledBitmap(tiny, max(1, w / 12), max(1, h / 12), true)
        canvas.drawBitmap(mid, Rect(0, 0, mid.width, mid.height), RectF(0f, 0f, w.toFloat(), h.toFloat()), paint)
        // A light veil keeps the sharp photo in front readable.
        canvas.drawColor(0x22000000)
        if (mid != tiny) mid.recycle()
        tiny.recycle()
    }

    private fun drawWatermark(canvas: Canvas, w: Float, h: Float, settings: WatermarkSettings) {
        val short = min(w, h)
        val alpha = (settings.opacity.coerceIn(0.05f, 1f) * 255).roundToInt()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = short * settings.size.coerceIn(0.02f, 0.15f)
            typeface = Typeface.DEFAULT_BOLD
            color = if (settings.light) android.graphics.Color.WHITE else android.graphics.Color.BLACK
            this.alpha = alpha
            if (settings.light) setShadowLayer(textSize * 0.08f, 0f, textSize * 0.04f, android.graphics.Color.argb(alpha / 2, 0, 0, 0))
        }
        val text = settings.text.trim()
        val bounds = Rect().also { paint.getTextBounds(text, 0, text.length, it) }
        val textW = paint.measureText(text)
        val textH = bounds.height().toFloat()
        if (settings.position == WatermarkPosition.TILED) {
            canvas.save()
            canvas.rotate(-30f, w / 2f, h / 2f)
            val stepX = textW + short * 0.12f
            val stepY = textH * 4f
            var row = 0
            var y = -h
            while (y < h * 2) {
                var x = -w + if (row % 2 == 0) 0f else stepX / 2f
                while (x < w * 2) {
                    canvas.drawText(text, x, y, paint)
                    x += stepX
                }
                y += stepY
                row++
            }
            canvas.restore()
        } else {
            val (x, top) = PhotoMath.watermarkOrigin(settings.position, w, h, textW, textH, short * 0.04f)
            canvas.drawText(text, x, top - bounds.top, paint)
        }
    }

    private suspend fun save(bitmap: Bitmap, baseName: String, png: Boolean): Pair<Uri, Long> {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 92, bytes)
        val fileName = FileNames.stamped(baseName, "Foto", if (png) "png" else "jpg")
        val uri = OutputStorage.save(context, fileName, if (png) "image/png" else "image/jpeg", OutputFolder.IMAGES) { out ->
            bytes.writeTo(out)
        }
        return uri to bytes.size().toLong()
    }

    private companion object {
        const val MAX_SIDE = 4096
    }
}
