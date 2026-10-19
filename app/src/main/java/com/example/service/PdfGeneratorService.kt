package com.example.service

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.model.ImageItem
import com.example.model.PdfPageSize
import com.example.model.PdfSettings
import com.example.util.ImageUtils
import com.example.util.PdfSecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGeneratorService {

  data class GenerationResult(
    val file: File,
    val fileSize: Long,
    val pageCount: Int,
    val fileName: String,
    val isEncrypted: Boolean = false,
    val hasSearchableText: Boolean = false,
  )

  /**
   * Generates a single PDF document from a list of images according to settings.
   * Recycles bitmaps immediately after rendering each page to prevent OutOfMemoryError.
   * Embeds searchable OCR text layer and applies enterprise-grade password encryption if configured.
   */
  suspend fun generatePdf(
    context: Context,
    images: List<ImageItem>,
    settings: PdfSettings,
    onProgress: (current: Int, total: Int, status: String) -> Unit,
  ): GenerationResult = withContext(Dispatchers.IO) {
    require(images.isNotEmpty()) { "No images provided for PDF generation" }

    // Ensure cache directory exists
    val pdfDir = File(context.cacheDir, "pdfs").apply { if (!exists()) mkdirs() }

    val formattedDate = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
    val baseName = if (settings.outputFileName.isNotBlank()) {
      val trimmed = settings.outputFileName.trim()
      if (trimmed.endsWith(".pdf", ignoreCase = true)) trimmed.substring(0, trimmed.length - 4) else trimmed
    } else {
      "Converted_$formattedDate"
    }
    val finalFileName = "$baseName.pdf"
    val outputFile = File(pdfDir, finalFileName)
    if (outputFile.exists()) {
      outputFile.delete()
    }

    val pdfDocument = PdfDocument()
    val paint = Paint().apply {
      isAntiAlias = true
      isFilterBitmap = true
    }

    // Transparent text paint for embedding invisible searchable OCR layer
    val ocrTextPaint = Paint().apply {
      isAntiAlias = true
      color = Color.TRANSPARENT
      textSize = 10f
    }

    var totalWordsEmbedded = 0

    try {
      val total = images.size
      for (index in images.indices) {
        val item = images[index]
        onProgress(index, total, "Processing page ${index + 1} of $total...")

        // Process image with downsampling, EXIF rotation, user rotation, and genuine JPEG compression
        val processed = ImageUtils.processImage(context, item.uri, item.rotationDegrees, settings)
        val bitmap = processed.bitmap

        val (pageWidth, pageHeight, destRect) = calculatePageLayout(
          bitmapWidth = bitmap.width,
          bitmapHeight = bitmap.height,
          pageSize = settings.pageSize,
          marginPoints = settings.pageMargin.points,
        )

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Clean white background
        canvas.drawColor(Color.WHITE)

        // Draw image into destination rectangle
        canvas.drawBitmap(bitmap, null, destRect, paint)

        // Embed invisible searchable OCR text layer into the PDF document stream
        if (settings.embedSearchableText && item.hasOcrResult && item.ocrBlocks.isNotEmpty()) {
          val scaleX = destRect.width() / bitmap.width.toFloat()
          val scaleY = destRect.height() / bitmap.height.toFloat()

          for (block in item.ocrBlocks) {
            val box = block.boundingBox
            if (box != null && block.text.isNotBlank()) {
              val drawX = destRect.left + (box.left * scaleX).coerceAtLeast(0f)
              val drawY = destRect.top + (box.bottom * scaleY).coerceAtMost(destRect.height())
              canvas.drawText(block.text, drawX, drawY, ocrTextPaint)
              totalWordsEmbedded += block.text.split("\\s+".toRegex()).count { it.isNotBlank() }
            }
          }
        }

        pdfDocument.finishPage(page)

        // CRITICAL: Recycle bitmap immediately to prevent OOM
        bitmap.recycle()

        onProgress(index + 1, total, "Completed page ${index + 1} of $total")
      }

      onProgress(total, total, "Saving PDF document...")
      FileOutputStream(outputFile).use { fos ->
        pdfDocument.writeTo(fos)
        fos.flush()
      }

      var isEncrypted = false

      // Enterprise-Grade PDF Encryption
      if (settings.enableEncryption && (settings.userPassword.isNotBlank() || settings.ownerPassword.isNotBlank())) {
        onProgress(total, total, "Encrypting document with 128-bit security...")
        val encryptedFile = File(pdfDir, "sec_${System.currentTimeMillis()}_$finalFileName")
        try {
          PdfSecurityUtils.encryptPdf(
            context = context,
            inputFile = outputFile,
            outputFile = encryptedFile,
            userPassword = settings.userPassword,
            ownerPassword = settings.ownerPassword,
          )

          if (encryptedFile.exists() && encryptedFile.length() > 0) {
            outputFile.delete()
            if (!encryptedFile.renameTo(outputFile)) {
              encryptedFile.copyTo(outputFile, overwrite = true)
              encryptedFile.delete()
            }
            isEncrypted = true
          }
        } catch (e: Exception) {
          // If encryption failed, keep original unencrypted file but log error
          e.printStackTrace()
        }
      }

      val generatedSize = outputFile.length()
      GenerationResult(
        file = outputFile,
        fileSize = generatedSize,
        pageCount = total,
        fileName = finalFileName,
        isEncrypted = isEncrypted,
        hasSearchableText = totalWordsEmbedded > 0,
      )
    } finally {
      pdfDocument.close()
    }
  }

  private data class PageLayout(
    val pageWidth: Int,
    val pageHeight: Int,
    val destRect: RectF,
  )

  private fun calculatePageLayout(
    bitmapWidth: Int,
    bitmapHeight: Int,
    pageSize: PdfPageSize,
    marginPoints: Float,
  ): PageLayout {
    return when (pageSize) {
      PdfPageSize.FIT_IMAGE -> {
        // Page dimensions match the image dimensions + margins
        val pw = (bitmapWidth + (2 * marginPoints)).toInt().coerceAtLeast(100)
        val ph = (bitmapHeight + (2 * marginPoints)).toInt().coerceAtLeast(100)
        val rect = RectF(
          marginPoints,
          marginPoints,
          marginPoints + bitmapWidth,
          marginPoints + bitmapHeight,
        )
        PageLayout(pw, ph, rect)
      }

      PdfPageSize.A4 -> {
        // Standard A4 is 595 x 842 points. Match page orientation to image orientation
        val isLandscape = bitmapWidth > bitmapHeight
        val standardWidth = if (isLandscape) 842 else 595
        val standardHeight = if (isLandscape) 595 else 842

        val availWidth = (standardWidth - (2 * marginPoints)).coerceAtLeast(10f)
        val availHeight = (standardHeight - (2 * marginPoints)).coerceAtLeast(10f)

        val scale = minOf(
          availWidth / bitmapWidth.toFloat(),
          availHeight / bitmapHeight.toFloat(),
        )

        val targetW = bitmapWidth * scale
        val targetH = bitmapHeight * scale

        // Center on the A4 page
        val left = marginPoints + ((availWidth - targetW) / 2f)
        val top = marginPoints + ((availHeight - targetH) / 2f)

        val rect = RectF(left, top, left + targetW, top + targetH)
        PageLayout(standardWidth, standardHeight, rect)
      }
    }
  }
}
