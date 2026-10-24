package com.example.model

import android.net.Uri

data class PdfDocumentItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: Uri? = null,
    val name: String,
    val sizeBytes: Long = 0,
    val isSplitSpread: Boolean = false,
    val splitPart: Int = 1, // 1 for left page, 2 for right page
    val rotationDegrees: Int = 0,
    val contrastBoost: Float = 1.0f,
    val isBinarized: Boolean = false,
    val cropRectNormalized: CropRect? = null
)

data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
)

data class PdfExportConfig(
    val pageSize: String = "A4", // "A4", "Letter", "Original"
    val marginMm: Int = 10,
    val compressionQuality: Int = 85,
    val autoContrastClean: Boolean = true,
    val splitDoublePages: Boolean = false,
    val title: String = "Document"
)
