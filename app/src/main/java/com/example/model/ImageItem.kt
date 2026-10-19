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
) {
  val formattedSize: String
    get() = formatBytes(sizeBytes)

  val effectiveWidth: Int
    get() = if (rotationDegrees % 180 != 0) height else width

  val effectiveHeight: Int
    get() = if (rotationDegrees % 180 != 0) width else height

  val dimensionsText: String
    get() = if (effectiveWidth > 0 && effectiveHeight > 0) "${effectiveWidth} × ${effectiveHeight}" else "Unknown"

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
