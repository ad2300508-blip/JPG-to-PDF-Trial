package com.example.model

import android.net.Uri
import java.io.File
import java.util.Locale

enum class CompressionMode {
  LOSSY,
  LOSSLESS
}

enum class ImageOutputFormat(val extension: String, val mimeType: String, val isLossless: Boolean) {
  JPEG("jpg", "image/jpeg", false),
  WEBP_LOSSY("webp", "image/webp", false),
  PNG("png", "image/png", true),
  WEBP_LOSSLESS("webp", "image/webp", true);

  val displayName: String
    get() = when (this) {
      JPEG -> "JPEG (.jpg)"
      WEBP_LOSSY -> "WEBP Lossy (.webp)"
      PNG -> "PNG Lossless (.png)"
      WEBP_LOSSLESS -> "WEBP Lossless (.webp)"
    }
}

enum class ScaleOption(val label: String, val factor: Float, val maxDimension: Int? = null) {
  ORIGINAL("100% (Original)", 1.0f),
  SCALE_75("75%", 0.75f),
  SCALE_50("50%", 0.50f),
  MAX_1080P("Max 1080p", 1.0f, 1920),
  MAX_2048P("Max 2048p", 1.0f, 2048);
}

data class ImageLabSettings(
  val mode: CompressionMode = CompressionMode.LOSSY,
  val lossyFormat: ImageOutputFormat = ImageOutputFormat.JPEG,
  val losslessFormat: ImageOutputFormat = ImageOutputFormat.WEBP_LOSSLESS,
  val quality: Int = 82, // 1 - 100
  val scaleOption: ScaleOption = ScaleOption.ORIGINAL,
  val stripExif: Boolean = true
) {
  val activeFormat: ImageOutputFormat
    get() = if (mode == CompressionMode.LOSSY) lossyFormat else losslessFormat
}

data class ImageLabItem(
  val id: String,
  val uri: Uri,
  val fileName: String,
  val originalSizeBytes: Long,
  val originalWidth: Int,
  val originalHeight: Int,
  val originalMimeType: String,
  val convertedFile: File? = null,
  val convertedSizeBytes: Long = 0L,
  val convertedWidth: Int = 0,
  val convertedHeight: Int = 0,
  val isProcessing: Boolean = false,
  val error: String? = null
) {
  val displayOriginalSize: String
    get() = formatBytes(originalSizeBytes)

  val displayConvertedSize: String
    get() = if (convertedSizeBytes > 0) formatBytes(convertedSizeBytes) else ""

  val savingsPercentage: Int
    get() = if (originalSizeBytes > 0 && convertedSizeBytes > 0) {
      val diff = originalSizeBytes - convertedSizeBytes
      ((diff.toDouble() / originalSizeBytes.toDouble()) * 100).toInt()
    } else 0

  companion object {
    fun formatBytes(bytes: Long): String {
      if (bytes <= 0) return "0 B"
      val kb = bytes / 1024.0
      val mb = kb / 1024.0
      val gb = mb / 1024.0
      return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
      }
    }
  }
}
