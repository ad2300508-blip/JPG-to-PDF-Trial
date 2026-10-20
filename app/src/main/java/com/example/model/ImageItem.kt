package com.example.model

import android.net.Uri

data class ImageItem(
  val id: String,
  val uri: Uri,
  val fileName: String,
  val sizeBytes: Long,
  val width: Int,
  val height: Int,
  val mimeType: String,
  val rotationDegrees: Int = 0,
  val ocrText: String? = null,
  val ocrBlocks: List<OcrBlock> = emptyList(),
  val isOcrScanning: Boolean = false,
  // AI Smart Book Crop & Super-Resolution Enhancer properties
  val originalUri: Uri = uri,
  val enhancedUri: Uri? = null,
  val enhancedWidth: Int? = null,
  val enhancedHeight: Int? = null,
  val enhancedSizeBytes: Long? = null,
  val isAiEnhanced: Boolean = false,
  val isEnhancing: Boolean = false,
  val enhancementDetails: String? = null,
  val detectedPageBadge: String? = null,
) {
  val activeUri: Uri
    get() = if (isAiEnhanced && enhancedUri != null) enhancedUri else uri

  val currentWidth: Int
    get() = if (isAiEnhanced && enhancedWidth != null && enhancedWidth > 0) enhancedWidth else width

  val currentHeight: Int
    get() = if (isAiEnhanced && enhancedHeight != null && enhancedHeight > 0) enhancedHeight else height

  val currentSizeBytes: Long
    get() = if (isAiEnhanced && enhancedSizeBytes != null && enhancedSizeBytes > 0) enhancedSizeBytes else sizeBytes

  val formattedSize: String
    get() = formatBytes(currentSizeBytes)

  val effectiveWidth: Int
    get() = if (rotationDegrees % 180 != 0) currentHeight else currentWidth

  val effectiveHeight: Int
    get() = if (rotationDegrees % 180 != 0) currentWidth else currentHeight

  val dimensionsText: String
    get() = if (effectiveWidth > 0 && effectiveHeight > 0) "${effectiveWidth} × ${effectiveHeight}" else "Unknown"

  val hasOcrResult: Boolean
    get() = !ocrText.isNullOrBlank()

  val ocrWordCount: Int
    get() = ocrText?.split("\\s+".toRegex())?.count { it.isNotBlank() } ?: 0

  companion object {
    fun formatBytes(bytes: Long): String {
      if (bytes <= 0) return "0 B"
      val kb = bytes / 1024.0
      val mb = kb / 1024.0
      val gb = mb / 1024.0
      return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.2f MB", mb)
        kb >= 1.0 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
      }
    }
  }
}
