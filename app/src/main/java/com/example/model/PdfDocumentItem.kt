package com.example.model

import android.graphics.Bitmap
import android.net.Uri

data class PdfMergeSettings(
    val outputFileName: String = "PDF_unito",
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
    val cropRectNormalized: CropRect? = null,
    /** Password typed by the user for a protected PDF (merge tool). */
    val password: String = ""
) {
    val displayPageCount: String get() = if (pageCount == 1) "1 pagina" else if (pageCount > 0) "$pageCount pagine" else "Pagine sconosciute"
    val formattedSize: String
        get() {
            val bytes = if (fileSizeBytes > 0L) fileSizeBytes else sizeBytes
            return when {
                bytes >= 1024 * 1024 -> String.format(java.util.Locale.ITALY, "%.1f MB", bytes.toDouble() / (1024 * 1024))
                bytes >= 1024 -> String.format(java.util.Locale.ITALY, "%d KB", bytes / 1024)
                else -> "$bytes B"
            }
        }
    val isCropped: Boolean get() = cropRectNormalized?.isFullFrame == false
}

data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    val isFullFrame: Boolean get() = left <= 0.001f && top <= 0.001f && right >= 0.999f && bottom >= 0.999f
}

/** Image quality / weight trade-off used when building a PDF from images. */
enum class PdfQuality(val label: String, val description: String, val maxDimension: Int, val jpegQuality: Int, val bytesPerPageCap: Long) {
    HIGH("Alta", "Massima nitidezza, file più pesante", 3000, 90, 1_100_000L),
    BALANCED("Bilanciata", "Ottima per studiare e condividere", 2000, 80, 450_000L),
    LIGHT("Leggera", "File piccolissimo, ideale per email e WhatsApp", 1400, 62, 180_000L)
}

data class PdfExportConfig(
    val pageSize: String = "A4",
    val orientation: String = "Auto",
    val marginMm: Int = 10,
    val quality: PdfQuality = PdfQuality.BALANCED,
    val autoContrastClean: Boolean = true,
    val grayscale: Boolean = false,
    val pageNumbers: Boolean = false,
    val password: String = "",
    val splitDoublePages: Boolean = false,
    val title: String = "Documento",
    val author: String = "",
    val subject: String = "",
    val keywords: String = "",
    val creator: String = "Morph",
    val creationDate: String = "",
    val stripMetadata: Boolean = false
) {
    val hasCustomMetadata: Boolean
        get() = author.isNotBlank() || subject.isNotBlank() || keywords.isNotBlank() || creationDate.isNotBlank()
}
