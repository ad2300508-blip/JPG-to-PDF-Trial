package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import com.example.model.OcrBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

object SmartCropUtils {
  private const val TAG = "SmartCropUtils"

  data class NormalizedInsets(
    val top: Float = 0.0f,
    val bottom: Float = 0.0f,
    val left: Float = 0.0f,
    val right: Float = 0.0f,
  ) {
    fun clamp(maxInset: Float = 0.25f): NormalizedInsets {
      return NormalizedInsets(
        top = top.coerceIn(0.0f, maxInset),
        bottom = bottom.coerceIn(0.0f, maxInset),
        left = left.coerceIn(0.0f, maxInset),
        right = right.coerceIn(0.0f, maxInset),
      )
    }

    val isZero: Boolean
      get() = top < 0.001f && bottom < 0.001f && left < 0.001f && right < 0.001f
  }

  data class CropResult(
    val croppedBitmap: Bitmap,
    val cropRect: Rect,
    val insets: NormalizedInsets,
    val originalWidth: Int,
    val originalHeight: Int,
    val methodUsed: String,
    val description: String,
  )

  /**
   * Applies normalized insets (0.0f..0.25f) to any bitmap of width W and height H,
   * computing exact pixel coordinates:
   * left = (W * insets.left).toInt(), top = (H * insets.top).toInt(),
   * width = (W * (1f - insets.left - insets.right)).toInt(),
   * height = (H * (1f - insets.top - insets.bottom)).toInt().
   * This completely eliminates all inSampleSize scaling mismatches.
   */
  fun applyNormalizedCrop(source: Bitmap, insets: NormalizedInsets): Bitmap {
    if (insets.isZero) return source
    val rect = calculatePixelRect(source.width, source.height, insets)
    return cropBitmapToRect(source, rect)
  }

  fun calculatePixelRect(width: Int, height: Int, insets: NormalizedInsets): Rect {
    val clamped = insets.clamp(0.25f)
    val left = (width * clamped.left).toInt().coerceIn(0, width - 1)
    val top = (height * clamped.top).toInt().coerceIn(0, height - 1)
    val right = (width * (1.0f - clamped.right)).toInt().coerceIn(left + 1, width)
    val bottom = (height * (1.0f - clamped.bottom)).toInt().coerceIn(top + 1, height)
    return Rect(left, top, right, bottom)
  }

  /**
   * 100% On-Device Zero-Cost Safe Content-Aware Edge Detector.
   * Scans inward from top, bottom, left, and right borders:
   * 1. Hard Safety Caps: Maximum 5% top (0.05f), 6% bottom (0.06f), 6% sides (0.06f).
   * 2. Stops immediately at white/light paper (R > 235, G > 235, B > 235 on > 55% of row/col).
   * 3. OCR Hard Shield: Never intersects or cuts off text, keeping a 1.5% margin outside detected text.
   */
  fun detectSafeContentInsets(bitmap: Bitmap, ocrBlocks: List<OcrBlock>): NormalizedInsets {
    val width = bitmap.width
    val height = bitmap.height
    if (width < 60 || height < 60) return NormalizedInsets()

    // 1. Hard safety caps
    val maxCapTop = 0.050f // max 5.0%
    val maxCapBottom = 0.060f // max 6.0%
    val maxCapSide = 0.060f // max 6.0%

    val maxTopPx = (height * maxCapTop).toInt()
    val maxBottomPx = (height * maxCapBottom).toInt()
    val maxSidePx = (width * maxCapSide).toInt()

    val stepY = max(1, height / 240)
    val stepX = max(1, width / 240)

    // A) Scan Top edge inward: stop at white paper or sudden text content
    var detectedTopPx = 0
    var foundTopPaper = false
    for (y in 0 until maxTopPx step stepY) {
      val whiteRatio = calculateRowWhitePaperRatio(bitmap, y, stepX)
      if (whiteRatio > 0.55f) {
        // Hit white/light page background! Stop trimming immediately.
        detectedTopPx = y
        foundTopPaper = true
        break
      }
      val lum = calculateRowLuminance(bitmap, y, stepX)
      // If we transition out of dark status bar into reading background
      if (lum > 75) {
        detectedTopPx = y
        foundTopPaper = true
        break
      }
    }
    if (!foundTopPaper) {
      // Dark status bar extends up to or through max top cap
      if (calculateRowLuminance(bitmap, 0, stepX) < 65) {
        detectedTopPx = maxTopPx
      }
    }

    // B) Scan Bottom edge upward: stop at white paper
    var detectedBottomPx = 0
    var foundBottomPaper = false
    for (y in (height - 1) downTo (height - maxBottomPx) step stepY) {
      val whiteRatio = calculateRowWhitePaperRatio(bitmap, y, stepX)
      if (whiteRatio > 0.55f) {
        detectedBottomPx = height - 1 - y
        foundBottomPaper = true
        break
      }
      val lum = calculateRowLuminance(bitmap, y, stepX)
      if (lum > 75) {
        detectedBottomPx = height - 1 - y
        foundBottomPaper = true
        break
      }
    }
    if (!foundBottomPaper) {
      if (calculateRowLuminance(bitmap, height - 1, stepX) < 65) {
        detectedBottomPx = maxBottomPx
      }
    }

    // C) Scan Left & Right edges inward (pillarboxes)
    var detectedLeftPx = 0
    var foundLeftPaper = false
    for (x in 0 until maxSidePx step stepX) {
      val whiteRatio = calculateColWhitePaperRatio(bitmap, x, stepY)
      if (whiteRatio > 0.55f) {
        detectedLeftPx = x
        foundLeftPaper = true
        break
      }
      val lum = calculateColLuminance(bitmap, x, stepY)
      if (lum > 75) {
        detectedLeftPx = x
        foundLeftPaper = true
        break
      }
    }
    if (!foundLeftPaper) {
      if (calculateColLuminance(bitmap, 0, stepY) < 65) {
        detectedLeftPx = maxSidePx
      }
    }

    var detectedRightPx = 0
    var foundRightPaper = false
    for (x in (width - 1) downTo (width - maxSidePx) step stepX) {
      val whiteRatio = calculateColWhitePaperRatio(bitmap, x, stepY)
      if (whiteRatio > 0.55f) {
        detectedRightPx = width - 1 - x
        foundRightPaper = true
        break
      }
      val lum = calculateColLuminance(bitmap, x, stepY)
      if (lum > 75) {
        detectedRightPx = width - 1 - x
        foundRightPaper = true
        break
      }
    }
    if (!foundRightPaper) {
      if (calculateColLuminance(bitmap, width - 1, stepY) < 65) {
        detectedRightPx = maxSidePx
      }
    }

    // Convert to normalized fractional insets
    var insetTop = (detectedTopPx.toFloat() / height).coerceIn(0.0f, maxCapTop)
    var insetBottom = (detectedBottomPx.toFloat() / height).coerceIn(0.0f, maxCapBottom)
    var insetLeft = (detectedLeftPx.toFloat() / width).coerceIn(0.0f, maxCapSide)
    var insetRight = (detectedRightPx.toFloat() / width).coerceIn(0.0f, maxCapSide)

    // D) OCR Bounding-Box Hard Shield:
    // If OCR text blocks exist, ensure crop line stays at least 1.5% away from any genuine page content
    if (ocrBlocks.isNotEmpty()) {
      val statusCutoffY = height * 0.038f // ignore clock/battery in top 3.8%
      val navCutoffY = height * 0.955f // ignore nav gesture line in bottom 4.5%
      val marginSafetyY = 0.015f
      val marginSafetyX = 0.015f

      for (block in ocrBlocks) {
        val box = block.boundingBox ?: continue
        // Skip isolated tiny text in extreme status bar or nav bar
        if (box.top < statusCutoffY && box.height() < height * 0.03f) continue
        if (box.bottom > navCutoffY && box.height() < height * 0.025f) continue

        val boxTopNorm = box.top.toFloat() / height
        val boxBottomNorm = (height - box.bottom).toFloat() / height
        val boxLeftNorm = box.left.toFloat() / width
        val boxRightNorm = (width - box.right).toFloat() / width

        // Clamp insets so they NEVER cut into text + 1.5% margin
        if (insetTop >= boxTopNorm - marginSafetyY) {
          insetTop = max(0.0f, boxTopNorm - marginSafetyY)
        }
        if (insetBottom >= boxBottomNorm - marginSafetyY) {
          insetBottom = max(0.0f, boxBottomNorm - marginSafetyY)
        }
        if (insetLeft >= boxLeftNorm - marginSafetyX) {
          insetLeft = max(0.0f, boxLeftNorm - marginSafetyX)
        }
        if (insetRight >= boxRightNorm - marginSafetyX) {
          insetRight = max(0.0f, boxRightNorm - marginSafetyX)
        }
      }
    }

    return NormalizedInsets(
      top = insetTop,
      bottom = insetBottom,
      left = insetLeft,
      right = insetRight,
    ).clamp(0.06f)
  }

  /**
   * Main entry point for on-device smart auto-crop. 100% offline & zero cost.
   */
  suspend fun autoCropBookScreenshot(
    bitmap: Bitmap,
    ocrBlocks: List<OcrBlock> = emptyList(),
    customInsets: NormalizedInsets? = null,
  ): CropResult = withContext(Dispatchers.Default) {
    val insets = customInsets ?: detectSafeContentInsets(bitmap, ocrBlocks)
    val pixelRect = calculatePixelRect(bitmap.width, bitmap.height, insets)
    val cropped = cropBitmapToRect(bitmap, pixelRect)
    val cleaned = ImageEnhancerUtils.eraseDynamicFloatingOverlays(cropped)

    CropResult(
      croppedBitmap = cleaned,
      cropRect = pixelRect,
      insets = insets,
      originalWidth = bitmap.width,
      originalHeight = bitmap.height,
      methodUsed = "Smart Offline Content Trimmer",
      description = "Safe edge trim (T:${(insets.top*100).toInt()}% B:${(insets.bottom*100).toInt()}% L:${(insets.left*100).toInt()}% R:${(insets.right*100).toInt()}%)"
    )
  }

  private fun calculateRowWhitePaperRatio(bitmap: Bitmap, y: Int, stepX: Int): Float {
    var whiteCount = 0
    var total = 0
    val width = bitmap.width
    for (x in 0 until width step stepX) {
      val pixel = bitmap.getPixel(x, y)
      val r = (pixel shr 16) and 0xFF
      val g = (pixel shr 8) and 0xFF
      val b = pixel and 0xFF
      if (r > 235 && g > 235 && b > 235) {
        whiteCount++
      }
      total++
    }
    return if (total > 0) whiteCount.toFloat() / total else 0f
  }

  private fun calculateColWhitePaperRatio(bitmap: Bitmap, x: Int, stepY: Int): Float {
    var whiteCount = 0
    var total = 0
    val height = bitmap.height
    for (y in 0 until height step stepY) {
      val pixel = bitmap.getPixel(x, y)
      val r = (pixel shr 16) and 0xFF
      val g = (pixel shr 8) and 0xFF
      val b = pixel and 0xFF
      if (r > 235 && g > 235 && b > 235) {
        whiteCount++
      }
      total++
    }
    return if (total > 0) whiteCount.toFloat() / total else 0f
  }

  private fun calculateRowLuminance(bitmap: Bitmap, y: Int, stepX: Int): Float {
    var sum = 0f
    var count = 0
    val width = bitmap.width
    for (x in 0 until width step stepX) {
      val pixel = bitmap.getPixel(x, y)
      val r = (pixel shr 16) and 0xFF
      val g = (pixel shr 8) and 0xFF
      val b = pixel and 0xFF
      val lum = 0.299f * r + 0.587f * g + 0.114f * b
      sum += lum
      count++
    }
    return if (count > 0) sum / count else 0f
  }

  private fun calculateColLuminance(bitmap: Bitmap, x: Int, stepY: Int): Float {
    var sum = 0f
    var count = 0
    val height = bitmap.height
    for (y in 0 until height step stepY) {
      val pixel = bitmap.getPixel(x, y)
      val r = (pixel shr 16) and 0xFF
      val g = (pixel shr 8) and 0xFF
      val b = pixel and 0xFF
      val lum = 0.299f * r + 0.587f * g + 0.114f * b
      sum += lum
      count++
    }
    return if (count > 0) sum / count else 0f
  }

  /**
   * Detects if an image is a landscape dual-page spread (width > height * 1.15)
   * and splits it into two separate vertical pages: Left page first (50%), Right page second (50%).
   */
  fun splitLandscapeDualPage(source: Bitmap): Pair<Bitmap, Bitmap> {
    val width = source.width
    val height = source.height

    // Search for spine fold within 47%..53% of width
    val searchMinX = (width * 0.47f).toInt()
    val searchMaxX = (width * 0.53f).toInt()
    val sampleStepY = max(1, height / 100)

    var spineX = width / 2
    var minLumSum = Float.MAX_VALUE

    for (x in searchMinX..searchMaxX step 2) {
      val colLum = calculateColLuminance(source, x, sampleStepY)
      if (colLum < minLumSum) {
        minLumSum = colLum
        spineX = x
      }
    }

    // Strip thin center fold seam shadow (~0.6% width)
    val spineMargin = max(1, (width * 0.006f).toInt())
    val leftCropWidth = (spineX - spineMargin).coerceIn(10, width / 2)
    val rightCropStartX = (spineX + spineMargin).coerceIn(width / 2, width - 10)
    val rightCropWidth = (width - rightCropStartX).coerceIn(10, width)

    val leftPage = Bitmap.createBitmap(source, 0, 0, leftCropWidth, height)
    val rightPage = Bitmap.createBitmap(source, rightCropStartX, 0, rightCropWidth, height)

    return Pair(leftPage, rightPage)
  }

  fun cropBitmapToRect(source: Bitmap, rect: Rect): Bitmap {
    val left = rect.left.coerceIn(0, source.width - 1)
    val top = rect.top.coerceIn(0, source.height - 1)
    val width = rect.width().coerceIn(1, source.width - left)
    val height = rect.height().coerceIn(1, source.height - top)

    return Bitmap.createBitmap(source, left, top, width, height)
  }
}
