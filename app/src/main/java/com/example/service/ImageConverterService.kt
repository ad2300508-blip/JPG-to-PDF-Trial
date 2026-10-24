package com.example.service

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.ImageConversionSettings
import com.example.model.ImageConvertItem
import com.example.model.ImageFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ImageConverterService(private val context: Context) {

    suspend fun convertImage(
        item: ImageConvertItem,
        settings: ImageConversionSettings
    ): Result<Pair<Uri, Long>> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(item.uri)
                ?: return@withContext Result.failure(Exception("Cannot open image file"))

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
            }
            var bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            if (bitmap == null) {
                return@withContext Result.failure(Exception("Failed to decode image"))
            }

            // Resolution scaling
            if (settings.resolutionScalePercent < 100) {
                val scale = settings.resolutionScalePercent / 100f
                val newW = (bitmap.width * scale).toInt().coerceAtLeast(1)
                val newH = (bitmap.height * scale).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
                if (scaled != bitmap) bitmap.recycle()
                bitmap = scaled
            }

            val compressFormat = when (settings.targetFormat) {
                ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
                ImageFormat.PNG -> Bitmap.CompressFormat.PNG
                ImageFormat.WEBP_LOSSY -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                }
                ImageFormat.WEBP_LOSSLESS -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                }
            }

            val quality = if (settings.targetFormat.isLossless) 100 else settings.qualityPercent.coerceIn(1, 100)
            val byteStream = ByteArrayOutputStream()
            bitmap.compress(compressFormat, quality, byteStream)
            val bytes = byteStream.toByteArray()
            bitmap.recycle()

            val baseName = item.name.substringBeforeLast(".")
            val outFilename = "${baseName}_converted.${settings.targetFormat.extension}"

            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, outFilename)
                put(MediaStore.MediaColumns.MIME_TYPE, settings.targetFormat.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/OmniConvert/Images")
                }
            }

            val outputUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("Could not create MediaStore record"))

            resolver.openOutputStream(outputUri)?.use { out ->
                out.write(bytes)
            }

            Result.success(Pair(outputUri, bytes.size.toLong()))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
