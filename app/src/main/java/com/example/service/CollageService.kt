package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import com.example.util.BitmapLoader
import com.example.util.CollageLayout
import com.example.util.CollagePlanner
import com.example.util.FileNames
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.util.resultOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

enum class CollageBackground(val label: String, val argb: Long) {
    WHITE("Bianco", 0xFFFFFFFF),
    CREAM("Panna", 0xFFF4EEE2),
    GRAPHITE("Grafite", 0xFF2A2C33),
    BLACK("Nero", 0xFF000000)
}

enum class CollageFormat(val label: String, val extension: String, val mimeType: String) {
    JPEG("JPG", "jpg", "image/jpeg"),
    PNG("PNG", "png", "image/png")
}

data class CollageSettings(
    val layout: CollageLayout = CollageLayout.GRID,
    /** Space between photos, as a fraction of a photo's side (0..0.1). */
    val gap: Float = 0.03f,
    /** Corner radius, as a fraction of a photo's side (0..0.15). */
    val corner: Float = 0.04f,
    val background: CollageBackground = CollageBackground.WHITE,
    val format: CollageFormat = CollageFormat.JPEG
)

class CollageService(private val context: Context) {

    /** Draws every photo into one image whose longest side is [maxSide] px and saves it in Images. */
    suspend fun render(
        uris: List<Uri>,
        aspects: List<Float>,
        settings: CollageSettings,
        maxSide: Int = 3072,
        onProgress: (Float) -> Unit = {}
    ): Result<Pair<Uri, Long>> = withContext(Dispatchers.IO) {
        resultOf {
            require(uris.isNotEmpty() && uris.size == aspects.size) { "Nessuna foto da unire" }
            val plan = CollagePlanner.plan(settings.layout, aspects, settings.gap)
            val scale = CollagePlanner.scaleFor(plan, maxSide)
            val out = Bitmap.createBitmap(
                (plan.width * scale).roundToInt().coerceAtLeast(1),
                (plan.height * scale).roundToInt().coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            try {
                val canvas = Canvas(out)
                canvas.drawColor(settings.background.argb.toInt())
                val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                val radius = settings.corner * CollagePlanner.UNIT * scale
                plan.cells.forEachIndexed { index, cell ->
                    ensureActive()
                    val dst = RectF(cell.x * scale, cell.y * scale, (cell.x + cell.width) * scale, (cell.y + cell.height) * scale)
                    val photo = BitmapLoader.decode(context, uris[index], max(64, (max(dst.width(), dst.height()) * 1.2f).roundToInt()))
                    if (photo != null) {
                        canvas.save()
                        if (radius > 0f) canvas.clipPath(Path().apply { addRoundRect(dst, radius, radius, Path.Direction.CW) })
                        canvas.drawBitmap(photo, centerCrop(photo, dst.width() / dst.height()), dst, paint)
                        canvas.restore()
                        photo.recycle()
                    }
                    onProgress((index + 1f) / plan.cells.size * 0.85f)
                }
                ensureActive()
                val name = FileNames.stamped("Collage", "Collage", settings.format.extension)
                var size = 0L
                val uri = OutputStorage.save(context, name, settings.format.mimeType, OutputFolder.IMAGES) { stream ->
                    val bytes = java.io.ByteArrayOutputStream()
                    val format = if (settings.format == CollageFormat.PNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                    out.compress(format, 92, bytes)
                    size = bytes.size().toLong()
                    bytes.writeTo(stream)
                }
                onProgress(1f)
                uri to size
            } finally {
                out.recycle()
            }
        }
    }

    /** Source rect that fills a box of [targetAspect] without stretching (the middle of the photo). */
    private fun centerCrop(photo: Bitmap, targetAspect: Float): Rect {
        val aspect = photo.width.toFloat() / photo.height
        return if (aspect > targetAspect) {
            val w = (photo.height * targetAspect).roundToInt().coerceIn(1, photo.width)
            val left = (photo.width - w) / 2
            Rect(left, 0, left + w, photo.height)
        } else {
            val h = (photo.width / targetAspect).roundToInt().coerceIn(1, photo.height)
            val top = (photo.height - h) / 2
            Rect(0, top, photo.width, top + h)
        }
    }
}
