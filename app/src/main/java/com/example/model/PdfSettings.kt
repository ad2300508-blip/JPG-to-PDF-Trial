package com.example.model

enum class DownscaleResolution(val label: String, val description: String, val maxLongEdge: Int?) {
  ORIGINAL("Original", "Keep original pixels", null),
  LARGE("Large", "Max 2560px (Ultra-HD)", 2560),
  MEDIUM("Medium", "Max 1920px (Full HD)", 1920),
  SMALL("Small", "Max 1280px (Compact)", 1280),
}

enum class PdfPageSize(val label: String, val description: String) {
  FIT_IMAGE("Fit to Image", "Page matches each image aspect ratio"),
  A4("Standard A4", "595 × 842 pt page, centered content"),
}

enum class PageMargin(val label: String, val points: Float) {
  NONE("None", 0f),
  SMALL("Small", 18f), // 0.25 inch at 72dpi
  MEDIUM("Medium", 36f), // 0.5 inch at 72dpi
}

enum class SmartCropMode(val label: String, val shortLabel: String, val description: String) {
  OFFLINE_OCR_EDGE("Smart Offline (OCR + Edge)", "Smart Offline", "Fast on-device luminance edge & OCR detection"),
  GEMINI_VISION("Gemini AI Vision", "Gemini AI Vision", "Cloud AI multimodal vision for exact book page boundaries"),
}

data class PdfSettings(
  val enableCompression: Boolean = true,
  val jpegQuality: Int = 80, // 10..100
  val downscaleResolution: DownscaleResolution = DownscaleResolution.MEDIUM,
  val pageSize: PdfPageSize = PdfPageSize.FIT_IMAGE,
  val pageMargin: PageMargin = PageMargin.NONE,
  val outputFileName: String = "",
  // Enterprise Security & Password Encryption
  val enableEncryption: Boolean = false,
  val userPassword: String = "",
  val ownerPassword: String = "",
  // Searchable PDF with OCR layer
  val embedSearchableText: Boolean = true,
  // Universal Smart Screenshot Cleaner & Master Crop
  val autoRemoveBorders: Boolean = false,
  val superResolutionEnhancer: Boolean = false,
  val splitDualPageSpread: Boolean = false,
  val masterCropTop: Float = 0.0f,
  val masterCropBottom: Float = 0.0f,
  val masterCropLeft: Float = 0.0f,
  val masterCropRight: Float = 0.0f,
) {
  val qualityDescription: String
    get() = when {
      jpegQuality >= 90 -> "Maximum Quality (Larger file)"
      jpegQuality >= 75 -> "Balanced (Recommended)"
      jpegQuality >= 50 -> "High Compression"
      else -> "Maximum Compression (Smallest file)"
    }

  val hasValidPassword: Boolean
    get() = !enableEncryption || userPassword.isNotBlank() || ownerPassword.isNotBlank()
}
