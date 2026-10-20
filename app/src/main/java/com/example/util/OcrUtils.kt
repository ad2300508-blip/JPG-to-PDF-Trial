package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import com.example.model.OcrBlock
import com.example.model.OcrResult
import com.example.model.PdfSettings
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object OcrUtils {

  private val recognizer by lazy {
    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
  }

  /**
   * Processes the image using ML Kit on-device text recognition.
   * Runs asynchronously on Dispatchers.IO without blocking the main UI thread.
   */
  suspend fun processImageOcr(
    context: Context,
    uri: Uri,
    rotationDegrees: Int = 0,
  ): OcrResult = withContext(Dispatchers.IO) {
    try {
      if (rotationDegrees == 0) {
        val inputImage = InputImage.fromFilePath(context, uri)
        val visionText = recognizer.process(inputImage).await()
        val fullText = visionText.text.trim()
        val blocks = visionText.textBlocks.map { block ->
          OcrBlock(
            text = block.text,
            boundingBox = block.boundingBox
          )
        }
        OcrResult(fullText = fullText, blocks = blocks)
      } else {
        // When user rotated image in app, decode rotated bitmap for accurate OCR coordinates
        val processed = ImageUtils.processImage(
          context = context,
          uri = uri,
          userRotation = rotationDegrees,
          settings = PdfSettings(enableCompression = false)
        )
        val inputImage = InputImage.fromBitmap(processed.bitmap, 0)
        val visionText = recognizer.process(inputImage).await()
        processed.bitmap.recycle()

        val fullText = visionText.text.trim()
        val blocks = visionText.textBlocks.map { block ->
          OcrBlock(
            text = block.text,
            boundingBox = block.boundingBox
          )
        }
        OcrResult(fullText = fullText, blocks = blocks)
      }
    } catch (_: Exception) {
      OcrResult(fullText = "", blocks = emptyList())
    }
  }

  /**
   * Scans OCR blocks specifically targeting bottom areas of the image/spread
   * to extract printed book page numbers (e.g. 366, 367, or range 366-367).
   * Returns a Pair of (primaryPageNumber, badgeText) e.g. (366, "p. 366-367") or null if not detected.
   */
  fun extractBookPageNumber(blocks: List<OcrBlock>, imageHeight: Int, imageWidth: Int): Pair<Int, String>? {
    if (blocks.isEmpty() || imageHeight <= 0) return null

    // Bottom 18% of screen is typical textbook pagination zone
    val bottomThreshold = imageHeight * 0.80f
    val pageNumberCandidates = mutableListOf<Pair<Int, Int>>() // Pair of (X coordinate, parsedInt)

    // Regex to match standalone 1 to 4 digit page numbers (e.g. "366", "Page 366", "pag. 366")
    val pageRegex = """\b(?:p(?:ag)?\.?\s*)?(\d{1,4})\b""".toRegex(RegexOption.IGNORE_CASE)

    for (block in blocks) {
      val box = block.boundingBox ?: continue
      // Must be in bottom region or extreme top corners (some books place page numbers at top outside edges)
      val isBottom = box.bottom >= bottomThreshold
      val isTopCorner = box.top <= imageHeight * 0.12f && (box.left < imageWidth * 0.20f || box.right > imageWidth * 0.80f)

      if (isBottom || isTopCorner) {
        val matches = pageRegex.findAll(block.text)
        for (match in matches) {
          val numStr = match.groupValues[1]
          val num = numStr.toIntOrNull() ?: continue
          // Filter out clock times (e.g. 1045, 1230, etc.) or huge years unless reasonable page numbers
          if (num in 1..2500) {
            val centerX = (box.left + box.right) / 2
            pageNumberCandidates.add(centerX to num)
          }
        }
      }
    }

    if (pageNumberCandidates.isEmpty()) return null

    // Sort candidates by horizontal X position (left to right)
    val sorted = pageNumberCandidates.sortedBy { it.first }
    val numbers = sorted.map { it.second }.distinct()

    return if (numbers.size >= 2) {
      val minPage = numbers.minOrNull() ?: numbers.first()
      val maxPage = numbers.maxOrNull() ?: numbers.last()
      if (maxPage - minPage in 1..2) {
        minPage to "p. $minPage–$maxPage"
      } else {
        numbers.first() to "p. ${numbers.first()}"
      }
    } else {
      numbers.first() to "p. ${numbers.first()}"
    }
  }

  private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { exception -> cont.resumeWithException(exception) }
    addOnCanceledListener { cont.cancel() }
  }
}
