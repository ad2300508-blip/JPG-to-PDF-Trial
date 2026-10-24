package com.example.service

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.PdfDocumentItem
import com.example.model.PdfExportConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class PdfConverterService(private val context: Context) {

    suspend fun generatePdf(
        items: List<PdfDocumentItem>,
        config: PdfExportConfig,
        onProgress: (Int, Int) -> Unit
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            var pageIndex = 1
            val totalPages = items.size

            val (pageWidth, pageHeight) = when (config.pageSize) {
                "Letter" -> Pair(612, 792)
                "Original" -> Pair(595, 842)
                else -> Pair(595, 842) // A4 default
            }

            for ((index, item) in items.withIndex()) {
                onProgress(index + 1, totalPages)
                val bitmap = loadProcessedBitmap(item, config) ?: continue

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex++).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val margin = (config.marginMm * 2.83465f).toInt() // mm to points
                val availableWidth = pageWidth - (margin * 2)
                val availableHeight = pageHeight - (margin * 2)

                val scale = minOf(
                    availableWidth.toFloat() / bitmap.width.toFloat(),
                    availableHeight.toFloat() / bitmap.height.toFloat()
                )

                val scaledWidth = bitmap.width * scale
                val scaledHeight = bitmap.height * scale
                val left = margin + (availableWidth - scaledWidth) / 2f
                val top = margin + (availableHeight - scaledHeight) / 2f

                val paint = Paint(Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(
                    bitmap,
                    null,
                    android.graphics.RectF(left, top, left + scaledWidth, top + scaledHeight),
                    paint
                )

                pdfDocument.finishPage(page)
                bitmap.recycle()
            }

            val filename = "${config.title.ifBlank { "OmniConvert_Doc" }}_${System.currentTimeMillis()}.pdf"
            val outputUri = savePdfToDownloads(filename, pdfDocument)
            pdfDocument.close()

            Result.success(outputUri)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun loadProcessedBitmap(item: PdfDocumentItem, config: PdfExportConfig): Bitmap? {
        val uri = item.uri ?: return null
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        var original = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (original == null) return null

        // Spread splitting (Book mode)
        if (item.isSplitSpread) {
            val halfWidth = original.width / 2
            val cropped = if (item.splitPart == 1) {
                Bitmap.createBitmap(original, 0, 0, halfWidth, original.height)
            } else {
                Bitmap.createBitmap(original, halfWidth, 0, halfWidth, original.height)
            }
            if (cropped != original) original.recycle()
            original = cropped
        }

        // Binarization & contrast boost for book screenshot cleanup
        if (config.autoContrastClean || item.isBinarized) {
            val processed = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(processed)
            val paint = Paint()

            val cm = ColorMatrix()
            cm.setSaturation(0f) // Grayscale
            if (item.contrastBoost > 1.0f || config.autoContrastClean) {
                val contrast = if (item.contrastBoost > 1.0f) item.contrastBoost else 1.4f
                val t = (1.0f - contrast) / 2.0f * 255.0f
                val matrix = floatArrayOf(
                    contrast, 0f, 0f, 0f, t,
                    0f, contrast, 0f, 0f, t,
                    0f, 0f, contrast, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                )
                cm.postConcat(ColorMatrix(matrix))
            }
            paint.colorFilter = ColorMatrixColorFilter(cm)
            canvas.drawBitmap(original, 0f, 0f, paint)
            original.recycle()
            original = processed
        }

        return original
    }

    private fun savePdfToDownloads(filename: String, document: PdfDocument): Uri {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/OmniConvert/PDFs")
            }
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Failed to create MediaStore entry")

        resolver.openOutputStream(uri)?.use { out ->
            document.writeTo(out)
        }

        return uri
    }
}
