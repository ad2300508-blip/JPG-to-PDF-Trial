package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import com.example.model.PdfDocumentItem
import com.example.model.PdfExportConfig
import com.example.util.BitmapLoader
import com.example.util.FileNames
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.util.resultOf
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.coroutines.coroutineContext

/**
 * Builds a PDF from images with PDFBox: every page image is JPEG-compressed with the chosen quality,
 * so the output weight really follows the "Qualità" option.
 */
class PdfConverterService(private val context: Context) {

    init {
        runCatching { PDFBoxResourceLoader.init(context) }
    }

    suspend fun generatePdf(
        items: List<PdfDocumentItem>,
        config: PdfExportConfig,
        onProgress: (Int, Int) -> Unit
    ): Result<Uri> = withContext(Dispatchers.IO) {
        resultOf {
            PDFBoxResourceLoader.init(context)
            val total = items.size
            var skipped = 0

            PDDocument().use { document ->
                for ((index, item) in items.withIndex()) {
                    coroutineContext.ensureActive()
                    onProgress(index + 1, total)

                    val bitmap = loadProcessedBitmap(item, config)
                    if (bitmap == null) {
                        skipped++
                        continue
                    }
                    val image = JPEGFactory.createFromImage(document, bitmap, config.quality.jpegQuality / 100f)
                    val imageWidth = bitmap.width.toFloat()
                    val imageHeight = bitmap.height.toFloat()
                    bitmap.recycle()

                    val (pageWidth, pageHeight) = pageSizeFor(config, imageWidth, imageHeight)
                    val page = PDPage(PDRectangle(pageWidth, pageHeight))
                    document.addPage(page)

                    val margin = if (config.pageSize == "Original") 0f else config.marginMm * MM_TO_PT
                    val footer = if (config.pageNumbers && config.pageSize != "Original") 14f else 0f
                    val availableWidth = (pageWidth - margin * 2).coerceAtLeast(1f)
                    val availableHeight = (pageHeight - margin * 2 - footer).coerceAtLeast(1f)
                    val scale = minOf(availableWidth / imageWidth, availableHeight / imageHeight)
                    val drawWidth = imageWidth * scale
                    val drawHeight = imageHeight * scale
                    val x = margin + (availableWidth - drawWidth) / 2f
                    val y = margin + footer + (availableHeight - drawHeight) / 2f

                    PDPageContentStream(document, page).use { stream ->
                        stream.drawImage(image, x, y, drawWidth, drawHeight)
                        if (config.pageNumbers) {
                            drawPageNumber(stream, document.numberOfPages, pageWidth, margin)
                        }
                    }
                }

                if (document.numberOfPages == 0) {
                    throw IllegalStateException("Nessuna immagine leggibile: controlla che i file siano JPG, PNG o WebP")
                }

                applyMetadata(document, config)
                if (config.password.isNotBlank()) {
                    val policy = StandardProtectionPolicy(config.password, config.password, AccessPermission())
                    policy.encryptionKeyLength = 128
                    document.protect(policy)
                }

                val fileName = FileNames.stamped(config.title, "Documento", "pdf")
                OutputStorage.save(context, fileName, "application/pdf", OutputFolder.PDFS) { out ->
                    document.save(out)
                }
            }
        }
    }

    private fun pageSizeFor(config: PdfExportConfig, imageWidth: Float, imageHeight: Float): Pair<Float, Float> {
        val imageIsLandscape = imageWidth > imageHeight
        val landscape = when (config.orientation) {
            "Landscape" -> true
            "Portrait" -> false
            else -> imageIsLandscape
        }
        return when (config.pageSize) {
            "Original" -> {
                // Same proportions as the image, longest side as long as an A4 page.
                val longSide = 842f
                if (imageIsLandscape) longSide to (longSide * imageHeight / imageWidth).coerceAtLeast(72f)
                else (longSide * imageWidth / imageHeight).coerceAtLeast(72f) to longSide
            }
            "Letter" -> if (landscape) 792f to 612f else 612f to 792f
            else -> if (landscape) 842f to 595f else 595f to 842f
        }
    }

    private fun drawPageNumber(stream: PDPageContentStream, number: Int, pageWidth: Float, margin: Float) {
        val text = number.toString()
        val font = PDType1Font.HELVETICA
        val fontSize = 9f
        val textWidth = font.getStringWidth(text) / 1000f * fontSize
        stream.beginText()
        stream.setFont(font, fontSize)
        stream.setNonStrokingColor(0.45f, 0.45f, 0.45f)
        stream.newLineAtOffset((pageWidth - textWidth) / 2f, (margin / 2f).coerceAtLeast(10f))
        stream.showText(text)
        stream.endText()
    }

    private fun applyMetadata(document: PDDocument, config: PdfExportConfig) {
        val info = document.documentInformation
        info.title = config.title.ifBlank { "Documento" }
        if (!config.stripMetadata) {
            if (config.author.isNotBlank()) info.author = config.author
            if (config.subject.isNotBlank()) info.subject = config.subject
            if (config.keywords.isNotBlank()) info.keywords = config.keywords
        }
        info.creator = config.creator.ifBlank { "Morph" }
        info.producer = "Morph"
        val created = (if (config.stripMetadata) null else parseDate(config.creationDate)) ?: Calendar.getInstance()
        info.creationDate = created
        info.modificationDate = created
    }

    private fun parseDate(raw: String): Calendar? {
        if (raw.isBlank()) return null
        return runCatching {
            val parsed = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).parse(raw) ?: return null
            Calendar.getInstance().apply { time = parsed }
        }.getOrNull()
    }

    /** Decode → rotate → crop → split spread → colour cleanup, always ending on an opaque bitmap. */
    private fun loadProcessedBitmap(item: PdfDocumentItem, config: PdfExportConfig): Bitmap? {
        val uri = item.uri ?: return null
        var bitmap = BitmapLoader.decode(context, uri, config.quality.maxDimension) ?: return null

        bitmap = BitmapLoader.rotate(bitmap, item.rotationDegrees)

        item.cropRectNormalized?.takeIf { !it.isFullFrame }?.let { crop ->
            bitmap = BitmapLoader.cropFraction(bitmap, crop.left, crop.top, crop.right, crop.bottom)
        }

        if (item.isSplitSpread) {
            bitmap = if (item.splitPart == 1) {
                BitmapLoader.cropFraction(bitmap, 0f, 0f, 0.5f, 1f)
            } else {
                BitmapLoader.cropFraction(bitmap, 0.5f, 0f, 1f, 1f)
            }
        }

        val filter = colorFilterFor(item, config)
        return BitmapLoader.flattenOnWhite(bitmap, filter?.let { Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = it } })
    }

    private fun colorFilterFor(item: PdfDocumentItem, config: PdfExportConfig): ColorMatrixColorFilter? {
        val strongClean = item.isBinarized
        val clean = config.autoContrastClean || strongClean
        val gray = config.grayscale || strongClean
        if (!clean && !gray && item.contrastBoost <= 1f) return null

        val matrix = ColorMatrix()
        when {
            gray -> matrix.setSaturation(0f)
            clean -> matrix.setSaturation(0.85f) // tones down the yellow cast of paper under lamps
        }
        val contrast = when {
            strongClean -> 1.8f
            item.contrastBoost > 1f -> item.contrastBoost
            clean -> 1.3f
            else -> 1f
        }
        if (contrast != 1f) {
            // Contrast around mid grey plus a small lift so the paper turns white instead of grey.
            val lift = if (clean) 18f else 0f
            val t = (1f - contrast) * 128f + lift
            matrix.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, t,
                        0f, contrast, 0f, 0f, t,
                        0f, 0f, contrast, 0f, t,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        return ColorMatrixColorFilter(matrix)
    }

    private companion object {
        const val MM_TO_PT = 2.83465f
    }
}
