package com.example

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.example.model.DownscaleResolution
import com.example.model.ImageItem
import com.example.model.OcrBlock
import com.example.model.PageMargin
import com.example.model.PdfPageSize
import com.example.model.PdfSettings
import com.example.model.SmartCropMode
import com.example.util.ImageEnhancerUtils
import com.example.util.ImageUtils
import com.example.util.SmartCropUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PdfConverterUnitTest {

  @Test
  fun testCalculateInSampleSize() {
    // When maxLongEdge is null, sample size should be 1
    assertEquals(1, ImageUtils.calculateInSampleSize(4000, 3000, null))

    // When maxLongEdge is 1920 and image is 4000, inSampleSize should be 2 (4000 / 2 = 2000 >= 1920)
    assertEquals(2, ImageUtils.calculateInSampleSize(4000, 3000, 1920))

    // When maxLongEdge is 1280 and image is 5120, inSampleSize should be 4
    assertEquals(4, ImageUtils.calculateInSampleSize(5120, 2880, 1280))

    // When image is smaller than target, inSampleSize should be 1
    assertEquals(1, ImageUtils.calculateInSampleSize(1000, 800, 1920))
  }

  @Test
  fun testFormatBytes() {
    assertEquals("0 B", ImageItem.formatBytes(0))
    assertEquals("500 B", ImageItem.formatBytes(500))
    assertEquals("2.0 KB", ImageItem.formatBytes(2048))
    assertEquals("1.50 MB", ImageItem.formatBytes(1572864))
  }

  @Test
  fun testRotationEffectiveDimensions() {
    val unrotated = ImageItem(
      id = "1",
      uri = Uri.parse("content://media/1"),
      fileName = "photo.jpg",
      sizeBytes = 1000,
      width = 4000,
      height = 3000,
      mimeType = "image/jpeg",
      rotationDegrees = 0
    )
    assertEquals(4000, unrotated.effectiveWidth)
    assertEquals(3000, unrotated.effectiveHeight)

    val rotated90 = unrotated.copy(rotationDegrees = 90)
    assertEquals(3000, rotated90.effectiveWidth)
    assertEquals(4000, rotated90.effectiveHeight)

    val rotated180 = unrotated.copy(rotationDegrees = 180)
    assertEquals(4000, rotated180.effectiveWidth)
    assertEquals(3000, rotated180.effectiveHeight)
  }

  @Test
  fun testPdfSettingsDefaultsAndSecurity() {
    val settings = PdfSettings()
    assertTrue(settings.enableCompression)
    assertEquals(80, settings.jpegQuality)
    assertEquals(DownscaleResolution.MEDIUM, settings.downscaleResolution)
    assertEquals(PdfPageSize.FIT_IMAGE, settings.pageSize)
    assertEquals(PageMargin.NONE, settings.pageMargin)
    assertEquals("Balanced (Recommended)", settings.qualityDescription)
    assertFalse(settings.enableEncryption)
    assertTrue(settings.hasValidPassword)

    // When encryption is enabled without password, hasValidPassword should be false
    val invalidSecSettings = settings.copy(enableEncryption = true, userPassword = "", ownerPassword = "")
    assertFalse(invalidSecSettings.hasValidPassword)

    // When encryption has password, hasValidPassword should be true
    val validSecSettings = settings.copy(enableEncryption = true, userPassword = "MyPassword123")
    assertTrue(validSecSettings.hasValidPassword)
    assertTrue(validSecSettings.embedSearchableText)

    // AI Book & Screenshot Studio settings
    assertFalse(settings.autoRemoveBorders)
    assertFalse(settings.superResolutionEnhancer)
    assertFalse(settings.splitDualPageSpread)
    assertEquals(0.0f, settings.masterCropTop)
  }

  @Test
  fun testOcrWordCountAndResult() {
    val imageWithoutOcr = ImageItem(
      id = "1",
      uri = Uri.parse("content://media/1"),
      fileName = "doc.jpg",
      sizeBytes = 5000,
      width = 1200,
      height = 800,
      mimeType = "image/jpeg"
    )
    assertFalse(imageWithoutOcr.hasOcrResult)
    assertEquals(0, imageWithoutOcr.ocrWordCount)

    val imageWithOcr = imageWithoutOcr.copy(
      ocrText = "Invoice Number: 98421 Total: $149.99 Due Immediately",
      ocrBlocks = listOf(
        OcrBlock(text = "Invoice Number: 98421"),
        OcrBlock(text = "Total: $149.99 Due Immediately")
      )
    )
    assertTrue(imageWithOcr.hasOcrResult)
    assertEquals(7, imageWithOcr.ocrWordCount)
    assertEquals(2, imageWithOcr.ocrBlocks.size)
  }

  @Test
  fun testEstimatePdfSize() {
    val dummyImages = listOf(
      ImageItem(
        id = "1",
        uri = Uri.parse("content://media/1"),
        fileName = "photo1.jpg",
        sizeBytes = 4_000_000L,
        width = 4000,
        height = 3000,
        mimeType = "image/jpeg"
      ),
      ImageItem(
        id = "2",
        uri = Uri.parse("content://media/2"),
        fileName = "photo2.png",
        sizeBytes = 6_000_000L,
        width = 4000,
        height = 3000,
        mimeType = "image/png"
      )
    )

    val compressedSettings = PdfSettings(
      enableCompression = true,
      jpegQuality = 80,
      downscaleResolution = DownscaleResolution.MEDIUM
    )

    val estimatedSize = ImageUtils.estimatePdfSize(dummyImages, compressedSettings)
    val rawTotal = dummyImages.sumOf { it.sizeBytes }

    // Compressed estimate should be significantly smaller than raw total
    assertTrue("Estimated size ($estimatedSize) should be less than raw total ($rawTotal)", estimatedSize < rawTotal)

    val uncompressedSettings = PdfSettings(enableCompression = false)
    val uncompressedEstimated = ImageUtils.estimatePdfSize(dummyImages, uncompressedSettings)
    assertTrue(uncompressedEstimated >= rawTotal)
  }

  @Test
  fun testSmartCropLocalBookDetector() {
    // Create a simulated tablet screenshot (width: 1000, height: 1600)
    // Top 80px: Black status bar (#000000)
    // Middle 1400px: Light paper book page (#FAF8F5)
    // Bottom 120px: Black navigation bar (#000000)
    val bitmap = Bitmap.createBitmap(1000, 1600, Bitmap.Config.ARGB_8888)
    for (y in 0 until 1600) {
      val color = when {
        y < 80 -> Color.BLACK
        y >= 1480 -> Color.BLACK
        else -> Color.rgb(250, 248, 245)
      }
      for (x in 0 until 1000) {
        bitmap.setPixel(x, y, color)
      }
    }

    val insets = SmartCropUtils.detectSafeContentInsets(bitmap, emptyList())
    val cropRect = SmartCropUtils.calculatePixelRect(bitmap.width, bitmap.height, insets)
    // Ensure top status bar was cropped (cropRect.top > 0)
    assertTrue("Detected top (${cropRect.top}) should strip status bar", cropRect.top >= 20)
    // Ensure bottom nav bar was cropped (cropRect.bottom < 1600)
    assertTrue("Detected bottom (${cropRect.bottom}) should strip nav bar", cropRect.bottom <= 1580)
  }

  @Test
  fun testImageEnhancerPipeline() {
    val smallBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    // Set near-white background
    for (y in 0 until 100) {
      for (x in 0 until 100) {
        smallBitmap.setPixel(x, y, Color.rgb(230, 230, 230))
      }
    }
    // Set some dark text pixels in center
    for (y in 45..55) {
      for (x in 45..55) {
        smallBitmap.setPixel(x, y, Color.rgb(30, 30, 30))
      }
    }

    // 1. Test 2x Upscaling
    val upscaled = ImageEnhancerUtils.upscale2x(smallBitmap)
    assertEquals(200, upscaled.width)
    assertEquals(200, upscaled.height)

    // 2. Test 3x3 Sharpening
    val sharpened = ImageEnhancerUtils.applySharpeningFilter(smallBitmap)
    assertEquals(100, sharpened.width)
    assertEquals(100, sharpened.height)

    // 3. Test Adaptive Background Cleaning (pushes 230, 230, 230 to pure white 255, 255, 255)
    val cleaned = ImageEnhancerUtils.cleanPaperBackground(smallBitmap)
    val cornerPixel = cleaned.getPixel(0, 0)
    assertEquals(255, Color.red(cornerPixel))
    assertEquals(255, Color.green(cornerPixel))
    assertEquals(255, Color.blue(cornerPixel))
  }

  @Test
  fun testPdfDocumentItemAndMergeSettings() {
    val pdfItem = com.example.model.PdfDocumentItem(
      id = "doc1",
      uri = Uri.parse("content://docs/sample.pdf"),
      fileName = "Chapter1.pdf",
      fileSizeBytes = 2_500_000L,
      pageCount = 14
    )
    assertEquals("Chapter1.pdf", pdfItem.fileName)
    assertEquals("14 pages", pdfItem.displayPageCount)
    assertEquals("2.4 MB", pdfItem.formattedSize)

    val singlePageItem = pdfItem.copy(pageCount = 1)
    assertEquals("1 page", singlePageItem.displayPageCount)

    val encryptedItem = pdfItem.copy(isEncrypted = true)
    assertEquals("Password protected", encryptedItem.displayPageCount)

    val settings = com.example.model.PdfMergeSettings()
    assertTrue(settings.hasValidPassword)

    val protectedSettings = settings.copy(enableEncryption = true, userPassword = "")
    assertFalse(protectedSettings.hasValidPassword)

    val validProtectedSettings = settings.copy(enableEncryption = true, userPassword = "SecretPassword123")
    assertTrue(validProtectedSettings.hasValidPassword)
  }

  @Test
  fun testPdfMergePageRangeParsing() {
    // "1-3, 5" on a 10-page document should yield 0, 1, 2, 4 (0-indexed)
    val parsed1 = com.example.service.PdfMergeService.parsePageRanges("1-3, 5", 10)
    assertEquals(listOf(0, 1, 2, 4), parsed1)

    // "4-2" (reversed range) should handle safely
    val parsed2 = com.example.service.PdfMergeService.parsePageRanges("4-2", 10)
    assertEquals(listOf(1, 2, 3), parsed2)

    // Single page
    val parsed3 = com.example.service.PdfMergeService.parsePageRanges("7", 10)
    assertEquals(listOf(6), parsed3)

    // Out of bounds pages clamped safely
    val parsed4 = com.example.service.PdfMergeService.parsePageRanges("8-15", 10)
    assertEquals(listOf(7, 8, 9), parsed4)
  }

  @Test
  fun testImageLabModelsAndSavings() {
    val item = com.example.model.ImageLabItem(
      id = "img1",
      uri = Uri.parse("content://media/img1"),
      fileName = "photo.png",
      originalSizeBytes = 4_000_000L,
      originalWidth = 3840,
      originalHeight = 2160,
      originalMimeType = "image/png",
      convertedSizeBytes = 1_000_000L,
      convertedWidth = 1920,
      convertedHeight = 1080
    )

    assertEquals("3.8 MB", item.displayOriginalSize)
    assertEquals("977 KB", item.displayConvertedSize)
    assertEquals(75, item.savingsPercentage)

    val settings = com.example.model.ImageLabSettings()
    assertEquals(com.example.model.CompressionMode.LOSSY, settings.mode)
    assertEquals(com.example.model.ImageOutputFormat.JPEG, settings.activeFormat)
    assertEquals(82, settings.quality)
    assertTrue(settings.stripExif)

    val losslessSettings = settings.copy(
      mode = com.example.model.CompressionMode.LOSSLESS,
      losslessFormat = com.example.model.ImageOutputFormat.WEBP_LOSSLESS
    )
    assertEquals(com.example.model.ImageOutputFormat.WEBP_LOSSLESS, losslessSettings.activeFormat)
    assertTrue(losslessSettings.activeFormat.isLossless)
  }

  @Test
  fun testAudioLabModelsAndDuration() {
    val item = com.example.model.AudioLabItem(
      id = "audio1",
      uri = Uri.parse("content://media/audio1"),
      fileName = "lecture.wav",
      durationMs = 215_000L, // 3 mins 35 secs
      sizeBytes = 38_000_000L,
      sampleRate = 44100,
      channels = 2,
      mimeType = "audio/wav",
      convertedSizeBytes = 3_800_000L
    )

    assertEquals("03:35", item.displayDuration)
    assertEquals("36.2 MB", item.displayOriginalSize)
    assertEquals("3.6 MB", item.displayConvertedSize)
    assertEquals(90, item.savingsPercentage)

    val settings = com.example.model.AudioLabSettings()
    assertEquals(com.example.model.AudioOutputMode.LOSSY_M4A_AAC, settings.mode)
    assertEquals(com.example.model.AudioBitrate.KBPS_128, settings.bitrate)
    assertFalse(settings.forceMono)
  }

  @Test
  fun testArchiveSettingsAndCompressionLevels() {
    val fileItem = com.example.model.ArchiveFileItem(
      id = "f1",
      uri = Uri.parse("content://files/report.pdf"),
      fileName = "report.pdf",
      sizeBytes = 1_500_000L,
      mimeType = "application/pdf"
    )
    assertEquals("1.4 MB", fileItem.displaySize)

    val settings = com.example.model.ArchiveSettings()
    assertEquals(com.example.model.ZipCompressionLevel.BALANCED_5, settings.compressionLevel)

    val archiveResult = com.example.model.ArchiveResult(
      file = java.io.File("/tmp/test.zip"),
      fileName = "bundle.zip",
      originalTotalBytes = 10_000_000L,
      compressedBytes = 4_000_000L
    )
    assertEquals(60, archiveResult.savingsPercentage)
  }
}

