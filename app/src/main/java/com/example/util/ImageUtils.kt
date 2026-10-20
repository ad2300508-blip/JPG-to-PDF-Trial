package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.DownscaleResolution
import com.example.model.ImageItem
import com.example.model.PdfSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID

object ImageUtils {

  data class ProcessedImage(
    val bitmap: Bitmap,
    val compressedSizeBytes: Long,
    val finalWidth: Int,
    val finalHeight: Int,
  )

  /**
   * Fast query of image metadata without loading entire image into RAM.
   */
  suspend fun extractImageItem(context: Context, uri: Uri): ImageItem = withContext(Dispatchers.IO) {
    var displayName = "Image_${System.currentTimeMillis()}"
    var fileSize = 0L

    // Query content resolver for display name and file size
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
    } catch (_: Exception) {
      // Fallback
    }

    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"

    // If file size wasn't returned by content resolver, count stream bytes
    if (fileSize <= 0L) {
      try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
          fileSize = stream.available().toLong()
        }
      } catch (_: Exception) {}
    }

    // Read bounds and EXIF orientation
    var rawWidth = 0
    var rawHeight = 0
    try {
      val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
        rawWidth = options.outWidth
        rawHeight = options.outHeight
      }
    } catch (_: Exception) {}

    // Check EXIF rotation to correct raw display dimensions
    val exifRotation = getExifOrientationDegrees(context, uri)
    val (effectiveWidth, effectiveHeight) = if (exifRotation == 90 || exifRotation == 270) {
      rawHeight to rawWidth
    } else {
      rawWidth to rawHeight
    }

    // Read modification timestamp from ContentResolver or EXIF
    var timestampMs = 0L
    try {
      // 1. Try DocumentsContract or MediaStore columns if available
      context.contentResolver.query(uri, arrayOf("last_modified", "date_modified", "datetaken"), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          for (col in arrayOf("last_modified", "date_modified", "datetaken")) {
            val idx = cursor.getColumnIndex(col)
            if (idx != -1 && !cursor.isNull(idx)) {
              val raw = cursor.getLong(idx)
              // Handle seconds vs milliseconds
              timestampMs = if (raw < 10_000_000_000L) raw * 1000L else raw
              if (timestampMs > 0) break
            }
          }
        }
      }
    } catch (_: Exception) {}

    if (timestampMs <= 0L) {
      // 2. Try EXIF DateTimeOriginal / DateTime
      try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
          val exifInterface = ExifInterface(stream)
          val dateStr = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
          if (!dateStr.isNullOrBlank()) {
            val format = java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US)
            timestampMs = format.parse(dateStr)?.time ?: 0L
          }
        }
      } catch (_: Exception) {}
    }

    ImageItem(
      id = UUID.randomUUID().toString(),
      uri = uri,
      fileName = displayName,
      sizeBytes = fileSize,
      width = effectiveWidth,
      height = effectiveHeight,
      mimeType = mimeType,
      timestampMs = timestampMs,
    )
  }

  /**
   * Natural alphanumeric sorting comparator (e.g., page_1, page_2, ..., page_10, page_20).
   * Also accounts for EXIF / file modification timestamps when filenames are non-numeric or identical.
   */
  fun naturalSortImages(items: List<ImageItem>): List<ImageItem> {
    return items.sortedWith(NaturalImageComparator)
  }

  object NaturalImageComparator : Comparator<ImageItem> {
    private val chunkPattern = java.util.regex.Pattern.compile("(\\d+)|(\\D+)")

    override fun compare(o1: ImageItem, o2: ImageItem): Int {
      val name1 = o1.fileName
      val name2 = o2.fileName

      val m1 = chunkPattern.matcher(name1)
      val m2 = chunkPattern.matcher(name2)

      while (m1.find() && m2.find()) {
        val chunk1 = m1.group()
        val chunk2 = m2.group()

        val isDigit1 = chunk1[0].isDigit()
        val isDigit2 = chunk2[0].isDigit()

        if (isDigit1 && isDigit2) {
          try {
            val b1 = java.math.BigInteger(chunk1)
            val b2 = java.math.BigInteger(chunk2)
            val cmp = b1.compareTo(b2)
            if (cmp != 0) return cmp
          } catch (_: Exception) {
            val cmp = chunk1.compareTo(chunk2)
            if (cmp != 0) return cmp
          }
        } else {
          val cmp = chunk1.compareTo(chunk2, ignoreCase = true)
          if (cmp != 0) return cmp
        }
      }

      // If one string has more chunks remaining
      val hasMore1 = m1.find()
      val hasMore2 = m2.find()
      if (hasMore1 && !hasMore2) return 1
      if (!hasMore1 && hasMore2) return -1

      // Fallback to timestamp if available
      if (o1.timestampMs > 0 && o2.timestampMs > 0) {
        val timeCmp = o1.timestampMs.compareTo(o2.timestampMs)
        if (timeCmp != 0) return timeCmp
      }

      return name1.compareTo(name2, ignoreCase = true)
    }
  }

  /**
   * Extracts EXIF rotation in degrees (0, 90, 180, 270)
   */
  fun getExifOrientationDegrees(context: Context, uri: Uri): Int {
    return try {
      context.contentResolver.openInputStream(uri)?.use { stream ->
        val exifInterface = ExifInterface(stream)
        when (exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
          ExifInterface.ORIENTATION_ROTATE_90 -> 90
          ExifInterface.ORIENTATION_ROTATE_180 -> 180
          ExifInterface.ORIENTATION_ROTATE_270 -> 270
          ExifInterface.ORIENTATION_TRANSPOSE -> 90
          ExifInterface.ORIENTATION_TRANSVERSE -> 270
          else -> 0
        }
      } ?: 0
    } catch (_: Exception) {
      0
    }
  }

  /**
   * Calculates optimal inSampleSize for power-of-2 downsampling during decoding.
   */
  fun calculateInSampleSize(width: Int, height: Int, maxLongEdge: Int?): Int {
    if (maxLongEdge == null || maxLongEdge <= 0) return 1
    val currentLongEdge = maxOf(width, height)
    var inSampleSize = 1
    while ((currentLongEdge / (inSampleSize * 2)) >= maxLongEdge) {
      inSampleSize *= 2
    }
    return inSampleSize
  }

  /**
   * Loads a full bitmap from uri.
   */
  suspend fun loadBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream)
      }
    } catch (_: Exception) {
      null
    }
  }

  /**
   * Decodes, corrects EXIF orientation, applies downscaling, compresses into JPEG byte stream,
   * and decodes back the compressed bitmap so PDF renderer receives genuinely compressed image.
   */
  suspend fun processImage(
    context: Context,
    uri: Uri,
    userRotation: Int = 0,
    settings: PdfSettings,
  ): ProcessedImage = withContext(Dispatchers.IO) {
    // 1. Decode bounds
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(stream, null, boundsOptions)
    }

    val origWidth = boundsOptions.outWidth
    val origHeight = boundsOptions.outHeight
    val maxLongEdge = if (settings.enableCompression) settings.downscaleResolution.maxLongEdge else 4096

    // 2. Compute inSampleSize
    val inSampleSize = calculateInSampleSize(origWidth, origHeight, maxLongEdge)

    // 3. Decode bitmap with inSampleSize
    val decodeOptions = BitmapFactory.Options().apply {
      this.inSampleSize = inSampleSize
      inPreferredConfig = Bitmap.Config.ARGB_8888
    }

    var decodedBitmap: Bitmap? = context.contentResolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(stream, null, decodeOptions)
    } ?: throw IllegalStateException("Unable to decode image from $uri")

    // 4. Apply combined EXIF and user orientation
    val exifRotation = getExifOrientationDegrees(context, uri)
    val totalRotation = (exifRotation + userRotation) % 360
    if (totalRotation != 0) {
      val matrix = Matrix().apply { postRotate(totalRotation.toFloat()) }
      val rotatedBitmap = Bitmap.createBitmap(
        decodedBitmap!!,
        0,
        0,
        decodedBitmap.width,
        decodedBitmap.height,
        matrix,
        true,
      )
      if (rotatedBitmap != decodedBitmap) {
        decodedBitmap.recycle()
        decodedBitmap = rotatedBitmap
      }
    }

    // 5. Downscale if requested and necessary
    if (settings.enableCompression && maxLongEdge != null) {
      val currentLongEdge = maxOf(decodedBitmap!!.width, decodedBitmap.height)
      if (currentLongEdge > maxLongEdge) {
        val scale = maxLongEdge.toFloat() / currentLongEdge.toFloat()
        val targetWidth = (decodedBitmap.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (decodedBitmap.height * scale).toInt().coerceAtLeast(1)

        val scaledBitmap = Bitmap.createScaledBitmap(decodedBitmap, targetWidth, targetHeight, true)
        if (scaledBitmap != decodedBitmap) {
          decodedBitmap.recycle()
          decodedBitmap = scaledBitmap
        }
      }
    }

    // 6. Compress with JPEG quality if compression is enabled
    if (settings.enableCompression) {
      val quality = settings.jpegQuality.coerceIn(10, 100)
      val outputStream = ByteArrayOutputStream()
      decodedBitmap!!.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
      val compressedBytes = outputStream.toByteArray()
      val compressedSize = compressedBytes.size.toLong()

      // Recycle the uncompressed decoded bitmap
      decodedBitmap.recycle()

      // Decode the compressed bytes back into a bitmap
      val finalBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)
        ?: throw IllegalStateException("Failed to decode compressed image buffer")

      ProcessedImage(
        bitmap = finalBitmap,
        compressedSizeBytes = compressedSize,
        finalWidth = finalBitmap.width,
        finalHeight = finalBitmap.height,
      )
    } else {
      // No compression: direct bitmap
      val rawBytesEstimate = (decodedBitmap!!.width * decodedBitmap.height * 3).toLong()
      ProcessedImage(
        bitmap = decodedBitmap,
        compressedSizeBytes = rawBytesEstimate,
        finalWidth = decodedBitmap.width,
        finalHeight = decodedBitmap.height,
      )
    }
  }

  /**
   * Fast mathematical estimation of total output PDF file size
   */
  fun estimatePdfSize(images: List<ImageItem>, settings: PdfSettings): Long {
    if (images.isEmpty()) return 0L
    var estimatedBytes = 0L

    for (img in images) {
      if (!settings.enableCompression) {
        estimatedBytes += if (img.sizeBytes > 0) img.sizeBytes else 1_500_000L
      } else {
        // Compute downscale factor
        val origLongEdge = maxOf(img.width, img.height).takeIf { it > 0 } ?: 2000
        val targetLongEdge = settings.downscaleResolution.maxLongEdge ?: origLongEdge
        val scaleRatio = minOf(1.0, targetLongEdge.toDouble() / origLongEdge.toDouble())
        val pixelRatio = scaleRatio * scaleRatio

        // Quality scaling heuristic (80% is baseline ~0.4 of uncompressed / ~0.5 of standard camera JPEG)
        // JPEG quality factor roughly scales non-linearly
        val qualityRatio = (settings.jpegQuality / 100.0).let { q ->
          0.15 + (q * q * 0.85)
        }

        val baseSize = if (img.sizeBytes > 0) img.sizeBytes else (img.width * img.height * 0.25).toLong()
        val estimatedImageBytes = (baseSize * pixelRatio * qualityRatio).toLong().coerceAtLeast(15_000L)
        estimatedBytes += estimatedImageBytes
      }
    }

    // Add minimal PDF structure overhead (~3KB per page + 4KB catalog/xref)
    val pdfOverhead = (images.size * 3000L) + 4096L
    return estimatedBytes + pdfOverhead
  }
}
