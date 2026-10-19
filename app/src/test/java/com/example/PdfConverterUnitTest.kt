package com.example

import android.net.Uri
import com.example.model.DownscaleResolution
import com.example.model.ImageItem
import com.example.model.OcrBlock
import com.example.model.PageMargin
import com.example.model.PdfPageSize
import com.example.model.PdfSettings
import com.example.util.ImageUtils
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
}
