package com.example.model

import android.graphics.Bitmap
import android.net.Uri
import java.util.Locale
import java.util.UUID

data class PdfDocumentItem(
  val id: String = UUID.randomUUID().toString(),
  val uri: Uri,
  val fileName: String,
  val fileSizeBytes: Long = 0L,
  val pageCount: Int = 0,
  val thumbnailBitmap: Bitmap? = null,
  val timestampMs: Long = System.currentTimeMillis(),
  val pageRange: String = "", // e.g. "" = All pages, "1-3", "2,4,6-8"
  val isEncrypted: Boolean = false,
  val isCorrupted: Boolean = false,
  val errorMessage: String? = null,
) {
  val formattedSize: String
    get() {
      val kb = fileSizeBytes / 1024.0
      val mb = kb / 1024.0
      return when {
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
        fileSizeBytes > 0 -> "$fileSizeBytes B"
        else -> "Unknown size"
      }
    }

  val displayPageCount: String
    get() = when {
      isEncrypted -> "Password protected"
      isCorrupted -> "Unsupported format"
      pageCount == 1 -> "1 page"
      pageCount > 1 -> "$pageCount pages"
      else -> "Reading..."
    }
}

data class PdfMergeSettings(
  val outputFileName: String = "",
  val enableEncryption: Boolean = false,
  val userPassword: String = "",
  val ownerPassword: String = "",
) {
  val hasValidPassword: Boolean
    get() = !enableEncryption || userPassword.isNotBlank() || ownerPassword.isNotBlank()
}
