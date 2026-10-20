package com.example.util

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.model.OcrBlock
import com.example.model.SmartCropMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

object SmartCropUtils {
  private const val TAG = "SmartCropUtils"

  private val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(30, TimeUnit.SECONDS)
      .readTimeout(45, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS)
      .build()
  }

  data class CropResult(
    val croppedBitmap: Bitmap,
    val cropRect: Rect,
    val originalWidth: Int,
    val originalHeight: Int,
    val methodUsed: String,
    val description: String,
  )

  /**
   * Main entry point for smart auto-cropping book screenshots.
   */
  suspend fun autoCropBookScreenshot(
    bitmap: Bitmap,
    ocrBlocks: List<OcrBlock> = emptyList(),
    cropMode: SmartCropMode = SmartCropMode.OFFLINE_OCR_EDGE,
    customGeminiApiKey: String = "",
  ): CropResult = withContext(Dispatchers.Default) {
    if (cropMode == SmartCropMode.GEMINI_VISION) {
      val geminiKey = customGeminiApiKey.trim().ifEmpty {
        try {
          BuildConfig.GEMINI_API_KEY.trim()
        } catch (e: Throwable) {
          ""
        }
      }
      if (geminiKey.isNotEmpty() && !geminiKey.startsWith("MY_GEMINI")) {
        try {
          val rect = detectWithGeminiVision(bitmap, geminiKey)
          if (rect != null) {
            val cropped = cropBitmapToRect(bitmap, rect)
            return@withContext CropResult(
              croppedBitmap = cropped,
              cropRect = rect,
              originalWidth = bitmap.width,
              originalHeight = bitmap.height,
              methodUsed = "Gemini AI Vision",
              description = "Cropped via Gemini AI Vision: stripped status bars, toolbars, and borders (${rect.width()}×${rect.height()})"
            )
          }
        } catch (e: Exception) {
          Log.w(TAG, "Gemini vision crop failed, falling back to local detector: ${e.message}")
        }
      }
    }

    // Local Smart Book Page Detector (Offline & Instant)
    val rect = detectLocalBookPage(bitmap, ocrBlocks)
    val cropped = cropBitmapToRect(bitmap, rect)
    // Clean fullscreen "ESC" toast overlays and reader toolbar icons
    val cleanedCropped = cleanOverlayChromeAndToasts(cropped, ocrBlocks)
    CropResult(
      croppedBitmap = cleanedCropped,
      cropRect = rect,
      originalWidth = bitmap.width,
      originalHeight = bitmap.height,
      methodUsed = "Smart Offline (OCR + Edge)",
      description = "Smart Local Crop: stripped status bar, toast banners & borders (${rect.width()}×${rect.height()})"
    )
  }

  /**
   * Universal Content-Aware Edge Trimmer:
   * Scans inward from top, bottom, left, and right edges using variance, edge contrast,
   * and OCR bounding boxes to find the true bounding box of the document, web page,
   * book page, chat, or primary content, stripping status bars, nav bars, and letterboxes.
   * Adds clean, customizable uniform padding (3%) around the content box.
   */
  fun detectLocalBookPage(bitmap: Bitmap, ocrBlocks: List<OcrBlock>): Rect {
    val width = bitmap.width
    val height = bitmap.height

    if (width < 50 || height < 50) {
      return Rect(0, 0, width, height)
    }

    val maxTopCheck = (height * 0.16f).toInt()
    val minTopCheck = (height * 0.022f).toInt()
    val maxBottomCheck = (height * 0.84f).toInt()
    val maxSideCheck = (width * 0.25f).toInt()

    val sampleStepY = max(1, height / 220)
    val sampleStepX = max(1, width / 220)

    // Calculate row luminances and variances
    val topRowData = mutableListOf<Pair<Float, Float>>() // lum, variance
    for (y in 0 until maxTopCheck step sampleStepY) {
      topRowData.add(calculateRowMetrics(bitmap, y, sampleStepX))
    }

    val bottomRowData = mutableListOf<Pair<Float, Float>>()
    for (y in (height - 1) downTo maxBottomCheck step sampleStepY) {
      bottomRowData.add(calculateRowMetrics(bitmap, y, sampleStepX))
    }

    val leftColData = mutableListOf<Pair<Float, Float>>()
    for (x in 0 until maxSideCheck step sampleStepX) {
      leftColData.add(calculateColMetrics(bitmap, x, sampleStepY))
    }

    val rightColData = mutableListOf<Pair<Float, Float>>()
    for (x in (width - 1) downTo (width - maxSideCheck) step sampleStepX) {
      rightColData.add(calculateColMetrics(bitmap, x, sampleStepY))
    }

    var cropTop = 0
    var cropBottom = height
    var cropLeft = 0
    var cropRight = width

    // Top status bar detection: either solid dark (lum < 45) or uniform low-variance header transition
    val topDarkIndex = topRowData.indexOfFirst { it.first > 45 }
    if (topDarkIndex > 0) {
      val detected = topDarkIndex * sampleStepY
      if (detected in minTopCheck..maxTopCheck) {
        cropTop = detected
      }
    } else {
      // Contrast / variance change detection (e.g. solid white status bar transitioning to content)
      for (i in 1 until topRowData.size) {
        val diffLum = kotlin.math.abs(topRowData[i].first - topRowData[i - 1].first)
        val varRise = topRowData[i].second - topRowData[i - 1].second
        if ((diffLum > 25 || varRise > 150) && (i * sampleStepY) in minTopCheck..maxTopCheck) {
          cropTop = i * sampleStepY
          break
        }
      }
    }

    // Bottom navigation/taskbar detection
    val bottomDarkIndex = bottomRowData.indexOfFirst { it.first > 45 }
    if (bottomDarkIndex > 0) {
      val detected = height - (bottomDarkIndex * sampleStepY)
      if (detected >= maxBottomCheck) {
        cropBottom = detected
      }
    } else {
      for (i in 1 until bottomRowData.size) {
        val diffLum = kotlin.math.abs(bottomRowData[i].first - bottomRowData[i - 1].first)
        val varRise = bottomRowData[i].second - bottomRowData[i - 1].second
        if ((diffLum > 25 || varRise > 150) && (height - i * sampleStepY) >= maxBottomCheck) {
          cropBottom = height - i * sampleStepY
          break
        }
      }
    }

    // Side letterbox/pillarbox detection
    val leftDarkIndex = leftColData.indexOfFirst { it.first > 45 }
    if (leftDarkIndex > 0) {
      val detected = leftDarkIndex * sampleStepX
      if (detected in 1..maxSideCheck) cropLeft = detected
    } else {
      for (i in 1 until leftColData.size) {
        if (leftColData[i].second > 80 && (i * sampleStepX) in 1..maxSideCheck) {
          cropLeft = i * sampleStepX
          break
        }
      }
    }

    val rightDarkIndex = rightColData.indexOfFirst { it.first > 45 }
    if (rightDarkIndex > 0) {
      val detected = width - (rightDarkIndex * sampleStepX)
      if (detected >= (width - maxSideCheck)) cropRight = detected
    } else {
      for (i in 1 until rightColData.size) {
        if (rightColData[i].second > 80 && (width - i * sampleStepX) >= (width - maxSideCheck)) {
          cropRight = width - i * sampleStepX
          break
        }
      }
    }

    // ML Kit OCR bounding box preservation
    if (ocrBlocks.isNotEmpty()) {
      var ocrMinX = Float.MAX_VALUE
      var ocrMinY = Float.MAX_VALUE
      var ocrMaxX = Float.MIN_VALUE
      var ocrMaxY = Float.MIN_VALUE
      var validBlockCount = 0

      val statusBarCutoffY = height * 0.038f
      val navBarCutoffY = height * 0.975f

      for (block in ocrBlocks) {
        val box = block.boundingBox ?: continue
        if (box.top < statusBarCutoffY && box.height() < height * 0.03f) continue
        if (box.bottom > navBarCutoffY && box.height() < height * 0.025f) continue

        ocrMinX = min(ocrMinX, box.left.toFloat())
        ocrMinY = min(ocrMinY, box.top.toFloat())
        ocrMaxX = max(ocrMaxX, box.right.toFloat())
        ocrMaxY = max(ocrMaxY, box.bottom.toFloat())
        validBlockCount++
      }

      if (validBlockCount > 0 && ocrMinX < ocrMaxX && ocrMinY < ocrMaxY) {
        if (ocrMinY > statusBarCutoffY && cropTop < (ocrMinY * 0.85f).toInt()) {
          cropTop = max(cropTop, (ocrMinY * 0.92f).toInt())
        }
        if (ocrMaxY < navBarCutoffY) {
          val bookPageBottom = (ocrMaxY + (height - ocrMaxY) * 0.35f).toInt()
          cropBottom = min(cropBottom, bookPageBottom.coerceAtMost(height))
        }
      }
    } else {
      if (cropTop == 0 && height > 600) {
        cropTop = (height * 0.038f).toInt()
      }
      if (cropBottom == height && height > 600) {
        cropBottom = (height * 0.965f).toInt()
      }
    }

    // Uniform 3% padding margin around the detected content box
    val marginX = ((cropRight - cropLeft) * 0.03f).toInt()
    val marginY = ((cropBottom - cropTop) * 0.03f).toInt()

    val finalLeft = max(0, cropLeft - marginX)
    val finalTop = max(0, cropTop - marginY)
    val finalRight = min(width, cropRight + marginX)
    val finalBottom = min(height, cropBottom + marginY)

    if (finalRight - finalLeft < width * 0.30f || finalBottom - finalTop < height * 0.30f) {
      return Rect(0, 0, width, height)
    }

    return Rect(finalLeft, finalTop, finalRight, finalBottom)
  }

  private fun calculateRowMetrics(bitmap: Bitmap, y: Int, stepX: Int): Pair<Float, Float> {
    var sum = 0f
    var sumSq = 0f
    var count = 0
    val width = bitmap.width
    for (x in 0 until width step stepX) {
      val pixel = bitmap.getPixel(x, y)
      val r = (pixel shr 16) and 0xFF
      val g = (pixel shr 8) and 0xFF
      val b = pixel and 0xFF
      val lum = 0.299f * r + 0.587f * g + 0.114f * b
      sum += lum
      sumSq += (lum * lum)
      count++
    }
    if (count == 0) return Pair(0f, 0f)
    val mean = sum / count
    val variance = (sumSq / count) - (mean * mean)
    return Pair(mean, max(0f, variance))
  }

  private fun calculateColMetrics(bitmap: Bitmap, x: Int, stepY: Int): Pair<Float, Float> {
    var sum = 0f
    var sumSq = 0f
    var count = 0
    val height = bitmap.height
    for (y in 0 until height step stepY) {
      val pixel = bitmap.getPixel(x, y)
      val r = (pixel shr 16) and 0xFF
      val g = (pixel shr 8) and 0xFF
      val b = pixel and 0xFF
      val lum = 0.299f * r + 0.587f * g + 0.114f * b
      sum += lum
      sumSq += (lum * lum)
      count++
    }
    if (count == 0) return Pair(0f, 0f)
    val mean = sum / count
    val variance = (sumSq / count) - (mean * mean)
    return Pair(mean, max(0f, variance))
  }

  /**
   * Cleans dark fullscreen toast overlays ("ESC" banners) and bottom reader UI icons:
   * 1. Detects dark rectangular overlay in bottom 12% center region (e.g., Zanichelli/bSmart ESC message).
   *    Fills that dark rectangle with pure paper white (#FFFFFF) so it completely disappears.
   * 2. Masks reader navigation arrows (< >) or publisher logos in bottom corners below text baseline.
   */
  fun cleanOverlayChromeAndToasts(bitmap: Bitmap, ocrBlocks: List<OcrBlock> = emptyList()): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width < 100 || height < 100) return bitmap

    // Ensure bitmap is mutable or copy to ARGB_8888
    val mutableBitmap = if (bitmap.isMutable) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(mutableBitmap)
    val eraserPaint = android.graphics.Paint().apply {
      color = android.graphics.Color.WHITE
      style = android.graphics.Paint.Style.FILL
    }

    // A) Dark Fullscreen Toast Banner Eraser:
    // Check bottom 14% of the image, centered horizontally (width between 25% and 75%)
    val toastTopY = (height * 0.86f).toInt()
    val toastBottomY = (height * 0.98f).toInt()
    val toastLeftX = (width * 0.20f).toInt()
    val toastRightX = (width * 0.80f).toInt()

    var detectedToastTop = -1
    var detectedToastBottom = -1
    var detectedToastLeft = width
    var detectedToastRight = 0

    val stepX = max(1, width / 120)
    val stepY = max(1, height / 120)

    // Scan for consecutive dark rows in the center horizontal section
    for (y in toastTopY until toastBottomY step stepY) {
      var darkCount = 0
      var totalCount = 0
      var minXInRow = width
      var maxXInRow = 0

      for (x in toastLeftX until toastRightX step stepX) {
        val pixel = mutableBitmap.getPixel(x, y)
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        val lum = 0.299f * r + 0.587f * g + 0.114f * b
        if (lum < 90) { // Dark toast banner background (black/dark gray)
          darkCount++
          minXInRow = min(minXInRow, x)
          maxXInRow = max(maxXInRow, x)
        }
        totalCount++
      }

      val ratio = if (totalCount > 0) darkCount.toFloat() / totalCount else 0f
      // If at least 35% of the sampled row is dark, it's part of the toast banner
      if (ratio >= 0.35f) {
        if (detectedToastTop == -1) detectedToastTop = y
        detectedToastBottom = y
        detectedToastLeft = min(detectedToastLeft, minXInRow)
        detectedToastRight = max(detectedToastRight, maxXInRow)
      }
    }

    // If a dark toast banner was detected, erase it with paper white
    if (detectedToastTop != -1 && detectedToastBottom > detectedToastTop && (detectedToastRight - detectedToastLeft) > width * 0.25f) {
      val padX = (width * 0.02f).toInt()
      val padY = (height * 0.01f).toInt()
      val eraseRect = android.graphics.Rect(
        max(0, detectedToastLeft - padX),
        max(0, detectedToastTop - padY),
        min(width, detectedToastRight + padX),
        min(height, detectedToastBottom + padY)
      )
      canvas.drawRect(eraseRect, eraserPaint)
      Log.d(TAG, "Erased dark toast banner: $eraseRect")
    }

    // B) Bottom Corner Reader Icons Masking:
    // Reader UI buttons (< >, QQ zoom) appear in extreme bottom-left and bottom-right corners (bottom 5%).
    // We mask the extreme lower toolbar strip (bottom 4%) if it contains high-contrast non-text icons,
    // ensuring we do not touch the page numbers which sit slightly higher (typically 5-10% from bottom).
    val extremeBottomY = (height * 0.965f).toInt()
    // Check if bottom 3.5% has icons on left (0 to 12%) or right (88% to 100%)
    val leftCornerRect = android.graphics.Rect(0, extremeBottomY, (width * 0.14f).toInt(), height)
    val rightCornerRect = android.graphics.Rect((width * 0.86f).toInt(), extremeBottomY, width, height)

    // Verify OCR blocks don't contain page numbers inside extreme bottom 3.5% before erasing
    val hasTextInLeftExtreme = ocrBlocks.any { b ->
      b.boundingBox?.let { it.top >= extremeBottomY && it.left < width * 0.15f } == true
    }
    val hasTextInRightExtreme = ocrBlocks.any { b ->
      b.boundingBox?.let { it.top >= extremeBottomY && it.right > width * 0.85f } == true
    }

    if (!hasTextInLeftExtreme) {
      canvas.drawRect(leftCornerRect, eraserPaint)
    }
    if (!hasTextInRightExtreme) {
      canvas.drawRect(rightCornerRect, eraserPaint)
    }

    return mutableBitmap
  }

  /**
   * Detects if an image is a landscape dual-page spread (width > height * 1.25)
   * and splits it into two separate vertical pages: Left page and Right page,
   * removing the center spine seam shadow.
   */
  fun splitLandscapeDualPage(source: Bitmap): Pair<Bitmap, Bitmap> {
    val width = source.width
    val height = source.height

    // Find the vertical center spine seam (within 46% to 54% of width)
    val searchMinX = (width * 0.46f).toInt()
    val searchMaxX = (width * 0.54f).toInt()
    val sampleStepY = max(1, height / 100)

    var spineX = width / 2
    var minLumSum = Float.MAX_VALUE

    // Spine often has a subtle dark fold shadow or separator line
    for (x in searchMinX..searchMaxX step 2) {
      val colLum = calculateColLuminance(source, x, sampleStepY)
      if (colLum < minLumSum) {
        minLumSum = colLum
        spineX = x
      }
    }

    // Spine width to cut out (approx 0.8% of width to strip the gray shadow fold)
    val spineMargin = max(2, (width * 0.008f).toInt())
    val leftCropWidth = (spineX - spineMargin).coerceIn(10, width / 2)
    val rightCropStartX = (spineX + spineMargin).coerceIn(width / 2, width - 10)
    val rightCropWidth = (width - rightCropStartX).coerceIn(10, width)

    val leftPage = Bitmap.createBitmap(source, 0, 0, leftCropWidth, height)
    val rightPage = Bitmap.createBitmap(source, rightCropStartX, 0, rightCropWidth, height)

    return Pair(leftPage, rightPage)
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
   * Calls Gemini Flash Vision API via direct REST to detect the exact normalized
   * bounding box [ymin, xmin, ymax, xmax] of the pure book page.
   */
  private suspend fun detectWithGeminiVision(bitmap: Bitmap, apiKey: String): Rect? =
    withContext(Dispatchers.IO) {
      try {
        // Downscale bitmap for fast upload (<1024px)
        val maxDim = 1024
        val scale = min(1.0f, maxDim.toFloat() / max(bitmap.width, bitmap.height))
        val scaledBitmap = if (scale < 1.0f) {
          Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt(),
            (bitmap.height * scale).toInt(),
            true
          )
        } else {
          bitmap
        }

        val base64Jpeg = ByteArrayOutputStream().use { stream ->
          scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
          Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        }

        val prompt = "Analyze this screenshot of a tablet reading an ebook or document. " +
            "Detect the exact rectangular bounding box of the book page content, excluding Android status bars (clock, battery, Wi-Fi), bottom navigation/gesture bars, reader app controls, and pillarbox borders. " +
            "Return a JSON object with keys 'ymin', 'xmin', 'ymax', 'xmax' representing the normalized coordinates (between 0.0 and 1.0) of the clean book page."

        val jsonRequest = JSONObject().apply {
          val contents = JSONArray().apply {
            val contentObj = JSONObject().apply {
              val parts = JSONArray().apply {
                put(JSONObject().apply { put("text", prompt) })
                put(JSONObject().apply {
                  put("inlineData", JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", base64Jpeg)
                  })
                })
              }
              put("parts", parts)
            }
            put(contentObj)
          }
          put("contents", contents)

          val genConfig = JSONObject().apply {
            put("responseMimeType", "application/json")
            put("temperature", 0.1)
          }
          put("generationConfig", genConfig)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
          .url(url)
          .post(requestBody)
          .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
          Log.w(TAG, "Gemini API HTTP ${response.code}: ${response.body?.string()}")
          return@withContext null
        }

        val responseText = response.body?.string() ?: return@withContext null
        val responseJson = JSONObject(responseText)
        val candidates = responseJson.optJSONArray("candidates") ?: return@withContext null
        if (candidates.length() == 0) return@withContext null

        val candidate = candidates.getJSONObject(0)
        val parts = candidate.optJSONObject("content")?.optJSONArray("parts") ?: return@withContext null
        if (parts.length() == 0) return@withContext null

        val text = parts.getJSONObject(0).optString("text", "")
        if (text.isBlank()) return@withContext null

        // Parse JSON object from model
        val cleanText = text.trim().removeSurrounding("```json", "```").trim()
        val boxJson = JSONObject(cleanText)

        val ymin = boxJson.optDouble("ymin", 0.0).toFloat().coerceIn(0f, 1f)
        val xmin = boxJson.optDouble("xmin", 0.0).toFloat().coerceIn(0f, 1f)
        val ymax = boxJson.optDouble("ymax", 1.0).toFloat().coerceIn(0f, 1f)
        val xmax = boxJson.optDouble("xmax", 1.0).toFloat().coerceIn(0f, 1f)

        if (ymax > ymin + 0.2f && xmax > xmin + 0.2f) {
          val left = (xmin * bitmap.width).toInt().coerceIn(0, bitmap.width)
          val top = (ymin * bitmap.height).toInt().coerceIn(0, bitmap.height)
          val right = (xmax * bitmap.width).toInt().coerceIn(0, bitmap.width)
          val bottom = (ymax * bitmap.height).toInt().coerceIn(0, bitmap.height)

          // Add a symmetrical 4% margin around content
          val marginX = ((right - left) * 0.04f).toInt()
          val marginY = ((bottom - top) * 0.04f).toInt()

          return@withContext Rect(
            max(0, left - marginX),
            max(0, top - marginY),
            min(bitmap.width, right + marginX),
            min(bitmap.height, bottom + marginY)
          )
        }
        null
      } catch (e: Exception) {
        Log.e(TAG, "Error in detectWithGeminiVision", e)
        null
      }
    }

  private fun cropBitmapToRect(source: Bitmap, rect: Rect): Bitmap {
    val left = rect.left.coerceIn(0, source.width - 1)
    val top = rect.top.coerceIn(0, source.height - 1)
    val width = rect.width().coerceIn(1, source.width - left)
    val height = rect.height().coerceIn(1, source.height - top)

    return Bitmap.createBitmap(source, left, top, width, height)
  }
}
