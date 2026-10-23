package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import com.example.model.PdfDocumentItem
import com.example.model.PdfMergeSettings
import com.example.util.PdfSecurityUtils
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfMergeService {
  private const val TAG = "PdfMergeService"

  /**
   * Fast metadata extraction & thumbnail rendering for a selected PDF URI.
   * Runs natively on Dispatchers.IO.
   */
  suspend fun extractPdfItem(context: Context, uri: Uri): PdfDocumentItem = withContext(Dispatchers.IO) {
    var displayName = "Document_${System.currentTimeMillis()}.pdf"
    var fileSize = 0L

    try {
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
          if (nameIndex != -1 && !cursor.isNull(nameIndex)) {
            displayName = cursor.getString(nameIndex)
          }
          if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
            fileSize = cursor.getLong(sizeIndex)
          }
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to query PDF name: ${e.message}")
    }

    if (fileSize <= 0L) {
      try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
          fileSize = stream.available().toLong()
        }
      } catch (_: Exception) {}
    }

    var pageCount = 0
    var thumbnailBitmap: Bitmap? = null
    var isEncrypted = false
    var isCorrupted = false
    var errorMessage: String? = null

    var pfd: ParcelFileDescriptor? = null
    var renderer: PdfRenderer? = null
    try {
      pfd = context.contentResolver.openFileDescriptor(uri, "r")
      if (pfd != null) {
        renderer = PdfRenderer(pfd)
        pageCount = renderer.pageCount
        if (pageCount > 0) {
          renderer.openPage(0).use { firstPage ->
            val origWidth = firstPage.width.coerceAtLeast(1)
            val origHeight = firstPage.height.coerceAtLeast(1)
            val maxThumbDim = 280
            val scale = (maxThumbDim.toFloat() / maxOf(origWidth, origHeight)).coerceAtMost(1.5f)
            val thumbW = (origWidth * scale).toInt().coerceAtLeast(1)
            val thumbH = (origHeight * scale).toInt().coerceAtLeast(1)

            val bmp = Bitmap.createBitmap(thumbW, thumbH, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            firstPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            thumbnailBitmap = bmp
          }
        }
      }
    } catch (se: SecurityException) {
      isEncrypted = true
      errorMessage = "Password protected document"
    } catch (e: Exception) {
      isCorrupted = true
      errorMessage = e.localizedMessage ?: "Unable to read PDF"
    } finally {
      try {
        renderer?.close()
      } catch (_: Exception) {}
      try {
        pfd?.close()
      } catch (_: Exception) {}
    }

    PdfDocumentItem(
      uri = uri,
      fileName = displayName,
      fileSizeBytes = fileSize,
      pageCount = pageCount,
      thumbnailBitmap = thumbnailBitmap,
      isEncrypted = isEncrypted,
      isCorrupted = isCorrupted,
      errorMessage = errorMessage,
    )
  }

  /**
   * Merges multiple PDF files into a single, cohesive PDF document.
   * Supports page range filtering (e.g., "1-3, 5") per document.
   * If password encryption is configured, applies standard PDF encryption.
   */
  suspend fun mergePdfs(
    context: Context,
    items: List<PdfDocumentItem>,
    settings: PdfMergeSettings,
    onProgress: (current: Int, total: Int, currentName: String) -> Unit,
  ): Result<File> = withContext(Dispatchers.IO) {
    if (items.size < 2) {
      return@withContext Result.failure(IllegalArgumentException("Please select at least 2 PDF documents to merge."))
    }

    PdfSecurityUtils.init(context)

    val tempDir = File(context.cacheDir, "pdf_merge_${System.currentTimeMillis()}").apply { mkdirs() }
    val tempSourceFiles = mutableListOf<File>()

    try {
      val merger = PDFMergerUtility()
      val outputFileName = if (settings.outputFileName.isNotBlank()) {
        val base = settings.outputFileName.trim()
        if (base.endsWith(".pdf", ignoreCase = true)) base else "$base.pdf"
      } else {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        "Merged_Doc_$stamp.pdf"
      }

      val unencryptedMergedFile = File(tempDir, "temp_merged_raw.pdf")
      merger.destinationFileName = unencryptedMergedFile.absolutePath

      for (i in items.indices) {
        val item = items[i]
        onProgress(i + 1, items.size, item.fileName)

        // Cache URI to temp file
        val tempSource = File(tempDir, "source_$i.pdf")
        context.contentResolver.openInputStream(item.uri)?.use { input ->
          FileOutputStream(tempSource).use { output ->
            input.copyTo(output)
          }
        } ?: return@withContext Result.failure(IllegalStateException("Could not read ${item.fileName}"))

        tempSourceFiles.add(tempSource)

        // If custom page ranges are specified (e.g. "1-3, 5")
        val effectiveRange = item.pageRange.trim()
        if (effectiveRange.isNotBlank() && effectiveRange.lowercase() != "all") {
          val filteredFile = File(tempDir, "filtered_$i.pdf")
          val success = extractPageSubset(tempSource, filteredFile, effectiveRange)
          if (success) {
            merger.addSource(filteredFile)
          } else {
            // Fallback to full file if range is invalid
            merger.addSource(tempSource)
          }
        } else {
          merger.addSource(tempSource)
        }
      }

      // Merge all documents seamlessly preserving vector text, fonts, and bookmarks
      merger.mergeDocuments(null)

      if (!unencryptedMergedFile.exists() || unencryptedMergedFile.length() == 0L) {
        return@withContext Result.failure(IllegalStateException("Failed to generate merged PDF."))
      }

      // Save into application files directory for persistent access
      val outputDir = File(context.filesDir, "merged_pdfs").apply { mkdirs() }
      val finalMergedFile = File(outputDir, outputFileName)

      if (settings.enableEncryption && settings.userPassword.isNotBlank()) {
        PdfSecurityUtils.encryptPdf(
          context = context,
          inputFile = unencryptedMergedFile,
          outputFile = finalMergedFile,
          userPassword = settings.userPassword,
          ownerPassword = settings.ownerPassword,
        )
      } else {
        unencryptedMergedFile.copyTo(finalMergedFile, overwrite = true)
      }

      Result.success(finalMergedFile)
    } catch (e: Exception) {
      Log.e(TAG, "Error merging PDFs", e)
      Result.failure(e)
    } finally {
      // Clean up temporary intermediate files
      try {
        tempDir.deleteRecursively()
      } catch (_: Exception) {}
    }
  }

  /**
   * Extracts a specified subset of pages (1-indexed, e.g. "1-3, 5") into a new PDF.
   */
  private fun extractPageSubset(sourceFile: File, destinationFile: File, pageRangeStr: String): Boolean {
    return try {
      val doc = PDDocument.load(sourceFile)
      val totalPages = doc.numberOfPages
      val pageIndicesToKeep = parsePageRanges(pageRangeStr, totalPages)

      if (pageIndicesToKeep.isEmpty()) {
        doc.close()
        return false
      }

      val subsetDoc = PDDocument()
      for (zeroIndex in pageIndicesToKeep) {
        if (zeroIndex in 0 until totalPages) {
          subsetDoc.addPage(doc.getPage(zeroIndex))
        }
      }
      subsetDoc.save(destinationFile)
      subsetDoc.close()
      doc.close()
      true
    } catch (e: Exception) {
      Log.w(TAG, "Failed to extract page subset: ${e.message}")
      false
    }
  }

  /**
   * Parses page range string like "1-3, 5, 7-9" into 0-indexed integer set.
   */
  fun parsePageRanges(rangeStr: String, totalPages: Int): List<Int> {
    val result = mutableSetOf<Int>()
    val tokens = rangeStr.split(",")
    for (rawToken in tokens) {
      val token = rawToken.trim()
      if (token.isEmpty()) continue
      if (token.contains("-")) {
        val parts = token.split("-")
        if (parts.size == 2) {
          val start = parts[0].trim().toIntOrNull() ?: 1
          val end = parts[1].trim().toIntOrNull() ?: totalPages
          val safeStart = maxOf(1, minOf(start, end))
          val safeEnd = minOf(totalPages, maxOf(start, end))
          for (p in safeStart..safeEnd) {
            result.add(p - 1)
          }
        }
      } else {
        val pageNum = token.toIntOrNull()
        if (pageNum != null && pageNum in 1..totalPages) {
          result.add(pageNum - 1)
        }
      }
    }
    return result.sorted()
  }
}
