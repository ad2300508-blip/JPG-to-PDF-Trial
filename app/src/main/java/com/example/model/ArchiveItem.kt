package com.example.model

import android.net.Uri
import java.io.File
import java.util.Locale
import java.util.zip.Deflater

enum class ZipCompressionLevel(val title: String, val subtitle: String, val level: Int) {
  STORE_0(
    "Level 0 — Store Only",
    "Instant packing with 0% CPU compression overhead",
    Deflater.NO_COMPRESSION
  ),
  BALANCED_5(
    "Level 5 — Balanced Deflate",
    "Fast lossless compression suitable for general documents & logs",
    5
  ),
  MAXIMUM_9(
    "Level 9 — Ultra Lossless Compression",
    "Maximum mathematical deflate compression for smallest archive size",
    Deflater.BEST_COMPRESSION
  );
}

data class ArchiveSettings(
  val archiveName: String = "OmniArchive_${System.currentTimeMillis() / 1000}",
  val compressionLevel: ZipCompressionLevel = ZipCompressionLevel.BALANCED_5
)

data class ArchiveFileItem(
  val id: String,
  val uri: Uri,
  val fileName: String,
  val sizeBytes: Long,
  val mimeType: String
) {
  val displaySize: String
    get() = formatBytes(sizeBytes)

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

data class ArchiveResult(
  val file: File,
  val fileName: String,
  val originalTotalBytes: Long,
  val compressedBytes: Long
) {
  val savingsPercentage: Int
    get() = if (originalTotalBytes > 0 && compressedBytes > 0) {
      val diff = originalTotalBytes - compressedBytes
      ((diff.toDouble() / originalTotalBytes.toDouble()) * 100).toInt()
    } else 0
}
