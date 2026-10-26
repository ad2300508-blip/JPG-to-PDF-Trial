package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import com.example.util.BitmapLoader
import com.example.util.FileNames
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.util.PhotoFilter
import com.example.util.PhotoFilterMath
import com.example.util.resultOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoFilterService(private val context: Context) {

    /** Applies [filter] at [intensity] (0..1) to one photo and saves it as JPEG in Images. */
    suspend fun apply(uri: Uri, name: String, filter: PhotoFilter, intensity: Float): Result<Pair<Uri, Long>> =
        withContext(Dispatchers.IO) {
            resultOf {
                val source = BitmapLoader.decode(context, uri, MAX_SIDE) ?: error("Immagine non leggibile")
                val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
                try {
                    val matrix = PhotoFilterMath.lerp(PhotoFilterMath.IDENTITY, filter.matrix, intensity)
                    val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(ColorMatrix(matrix)) }
                    Canvas(out).apply {
                        drawColor(android.graphics.Color.WHITE)
                        drawBitmap(source, 0f, 0f, paint)
                    }
                    val fileName = FileNames.stamped("${FileNames.baseName(name)}_${filter.label.replace("/", "")}", "Foto", "jpg")
                    var size = 0L
                    val saved = OutputStorage.save(context, fileName, "image/jpeg", OutputFolder.IMAGES) { stream ->
                        val bytes = java.io.ByteArrayOutputStream()
                        out.compress(Bitmap.CompressFormat.JPEG, 92, bytes)
                        size = bytes.size().toLong()
                        bytes.writeTo(stream)
                    }
                    saved to size
                } finally {
                    source.recycle()
                    out.recycle()
                }
            }
        }

    private companion object {
        const val MAX_SIDE = 4096
    }
}
