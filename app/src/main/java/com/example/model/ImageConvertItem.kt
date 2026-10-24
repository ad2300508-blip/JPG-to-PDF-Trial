package com.example.model

import android.net.Uri

enum class ImageFormat(val extension: String, val mimeType: String, val isLossless: Boolean) {
    JPEG("jpg", "image/jpeg", false),
    WEBP_LOSSY("webp", "image/webp", false),
    PNG("png", "image/png", true),
    WEBP_LOSSLESS("webp", "image/webp", true)
}

data class ImageConvertItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String,
    val originalSizeBytes: Long = 0,
    val originalWidth: Int = 0,
    val originalHeight: Int = 0,
    val convertedSizeBytes: Long? = null,
    val isProcessing: Boolean = false,
    val error: String? = null
)

data class ImageConversionSettings(
    val targetFormat: ImageFormat = ImageFormat.JPEG,
    val qualityPercent: Int = 85,
    val resolutionScalePercent: Int = 100, // 100, 75, 50
    val stripExif: Boolean = true
)
