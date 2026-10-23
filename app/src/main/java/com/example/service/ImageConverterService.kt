package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.example.model.CompressionMode
import com.example.model.ImageLabItem
import com.example.model.ImageLabSettings
import com.example.model.ImageOutputFormat
import com.example.model.ScaleOption
import com.example.util.DownloadUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ImageConverterService {

  suspend fun extractMetadata(context: Context, uri: Uri): ImageLabItem = withContext(Dispatchers.IO) {
    var fileName = "image_${System.currentTimeMillis()}"
    var fileSize = 0L

    try {
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
          if (nameIndex != -1) {
            val name = cursor.getString(nameIndex)
            if (!name.isNullOrBlank()) fileName = name
          }
          if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
            fileSize = cursor.getLong(sizeIndex)
          }
        }
      }
    } catch (_: Exception) { }

    val mimeType = context.contentResolver.getType(uri) ?: "image/*"

    var width = 0
    var height = 0
    try {
      context.contentResolver.openInputStream(uri)?.use { stream ->
        val options = BitmapFactory.Options().apply {
          inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(stream, null, options)
        width = options.outWidth
        height = options.outHeight
      }
    } catch (_: Exception) { }

    ImageLabItem(
      id = UUID.randomUUID().toString(),
      uri = uri,
      fileName = fileName,
      originalSizeBytes = fileSize,
      originalWidth = width,
      originalHeight = height,
      originalMimeType = mimeType
    )
  }

  suspend fun convertImage(
    context: Context,
    item: ImageLabItem,
    settings: ImageLabSettings
  ): Result<ImageLabItem> = withContext(Dispatchers.IO) {
    var rawBitmap: Bitmap? = null
    var transformedBitmap: Bitmap? = null
    try {
      val orientation = getExifOrientation(context, item.uri)

      // Decode full bitmap
      val pfd = context.contentResolver.openFileDescriptor(item.uri, "r")
        ?: throw IllegalStateException("Cannot open input descriptor for ${item.fileName}")

      pfd.use { descriptor ->
        rawBitmap = BitmapFactory.decodeFileDescriptor(descriptor.fileDescriptor)
      }

      val source = rawBitmap ?: throw IllegalStateException("Failed to decode bitmap from ${item.fileName}")

      // Compute target dimensions based on scale option
      val originalW = source.width
      val originalH = source.height

      var scale = when {
        settings.scaleOption.maxDimension != null -> {
          val maxDim = settings.scaleOption.maxDimension
          val currentMax = maxOf(originalW, originalH)
          if (currentMax > maxDim) {
            maxDim.toFloat() / currentMax.toFloat()
          } else {
            1.0f
          }
        }
        else -> settings.scaleOption.factor
      }
      if (scale <= 0f) scale = 1.0f

      val targetW = (originalW * scale).toInt().coerceAtLeast(1)
      val targetH = (originalH * scale).toInt().coerceAtLeast(1)

      val matrix = Matrix()
      if (targetW != originalW || targetH != originalH) {
        matrix.postScale(scale, scale)
      }

      // Apply EXIF orientation rotation
      when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
      }

      transformedBitmap = if (!matrix.isIdentity) {
        Bitmap.createBitmap(source, 0, 0, originalW, originalH, matrix, true)
      } else {
        source
      }

      val format = settings.activeFormat
      val compressFormat = when (format) {
        ImageOutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
        ImageOutputFormat.WEBP_LOSSY -> {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
          } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
          }
        }
        ImageOutputFormat.PNG -> Bitmap.CompressFormat.PNG
        ImageOutputFormat.WEBP_LOSSLESS -> {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSLESS
          } else {
            Bitmap.CompressFormat.PNG
          }
        }
      }

      val quality = if (format.isLossless) 100 else settings.quality.coerceIn(1, 100)

      val baseName = item.fileName.substringBeforeLast(".")
      val outputFile = File(context.cacheDir, "omni_${baseName}_${System.currentTimeMillis()}.${format.extension}")

      FileOutputStream(outputFile).use { out ->
        transformedBitmap.compress(compressFormat, quality, out)
        out.flush()
      }

      val finalSizeBytes = outputFile.length()
      val finalW = transformedBitmap.width
      val finalH = transformedBitmap.height

      Result.success(
        item.copy(
          convertedFile = outputFile,
          convertedSizeBytes = finalSizeBytes,
          convertedWidth = finalW,
          convertedHeight = finalH,
          isProcessing = false,
          error = null
        )
      )
    } catch (e: Exception) {
      Result.failure(e)
    } finally {
      if (transformedBitmap != null && transformedBitmap != rawBitmap && !transformedBitmap.isRecycled) {
        transformedBitmap.recycle()
      }
      if (rawBitmap != null && !rawBitmap.isRecycled) {
        rawBitmap.recycle()
      }
    }
  }

  private fun getExifOrientation(context: Context, uri: Uri): Int {
    return try {
      context.contentResolver.openInputStream(uri)?.use { stream ->
        val exif = ExifInterface(stream)
        exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
      } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (_: Exception) {
      ExifInterface.ORIENTATION_NORMAL
    }
  }

  suspend fun exportBatchToDownloads(
    context: Context,
    items: List<ImageLabItem>
  ): List<DownloadUtils.DownloadResult> = withContext(Dispatchers.IO) {
    val results = mutableListOf<DownloadUtils.DownloadResult>()
    for (item in items) {
      val file = item.convertedFile ?: continue
      val mime = when (file.extension.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> "image/*"
      }
      val desiredName = file.name
      val saveResult = DownloadUtils.saveFileToDownloads(
        context = context,
        sourceFile = file,
        desiredFileName = desiredName,
        mimeType = mime,
        subfolder = "OmniConvert/Images"
      )
      saveResult.getOrNull()?.let { results.add(it) }
    }
    results
  }
}
