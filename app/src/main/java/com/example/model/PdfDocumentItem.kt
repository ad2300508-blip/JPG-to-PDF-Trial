package com.example.model

import android.graphics.Bitmap
import android.net.Uri

data class PdfMergeSettings(
    val outputFileName: String = "Merged_Document",
    val enableEncryption: Boolean = false,
    val userPassword: String = "",
    val ownerPassword: String = ""
) {
    val hasValidPassword: Boolean get() = !enableEncryption || userPassword.isNotBlank()
}

data class PdfDocumentItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: Uri? = null,
    val fileName: String = "",
    val name: String = fileName,
    val fileSizeBytes: Long = 0L,
    val sizeBytes: Long = fileSizeBytes,
    val pageCount: Int = 0,
    val pageRange: String = "",
    val thumbnailBitmap: Bitmap? = null,
    val isEncrypted: Boolean = false,
    val isCorrupted: Boolean = false,
    val errorMessage: String? = null,
    val isSplitSpread: Boolean = false,
    val splitPart: Int = 1,
    val rotationDegrees: Int = 0,
    val contrastBoost: Float = 1.0f,
    val isBinarized: Boolean = false,
    val cropRectNormalized: CropRect? = null
) {
    val displayPageCount: String get() = if (pageCount > 0) "$pageCount pages" else "Unknown pages"
    val formattedSize: String
        get() {
            val bytes = if (fileSizeBytes > 0L) fileSizeBytes else sizeBytes
            return when {
                bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
                bytes >= 1024 -> String.format("%d KB", bytes / 1024)
                else -> "$bytes B"
            }
        }
}

data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
)

data class PdfExportConfig(
    val pageSize: String = "A4",
    val orientation: String = "Auto",
    val marginMm: Int = 10,
    val compressionQuality: Int = 85,
    val autoContrastClean: Boolean = true,
    val splitDoublePages: Boolean = false,
    val title: String = "Documento",
    val author: String = "",
    val subject: String = "",
    val keywords: String = "",
    val creator: String = "OmniConvert Studio",
    val creationDate: String = ""
) {
    val hasCustomMetadata: Boolean
        get() = author.isNotBlank() || subject.isNotBlank() || keywords.isNotBlank() || creationDate.isNotBlank()
}
