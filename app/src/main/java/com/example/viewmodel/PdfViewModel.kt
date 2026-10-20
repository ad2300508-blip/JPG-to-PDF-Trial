package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ImageItem
import com.example.model.PdfSettings
import com.example.model.SmartCropMode
import com.example.service.PdfGeneratorService
import com.example.util.DownloadUtils
import com.example.util.ImageEnhancerUtils
import com.example.util.ImageUtils
import com.example.util.OcrUtils
import com.example.util.SmartCropUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface ConversionState {
  object Idle : ConversionState
  data class Processing(val current: Int, val total: Int, val status: String) : ConversionState
  data class Success(
    val result: PdfGeneratorService.GenerationResult,
    val contentUri: Uri,
    val downloadUri: Uri? = null,
    val savedPathDisplay: String = "Downloads/${result.fileName}",
    val autoDownloaded: Boolean = false,
  ) : ConversionState
  data class Error(val message: String) : ConversionState
}

class PdfViewModel : ViewModel() {

  private val _images = MutableStateFlow<List<ImageItem>>(emptyList())
  val images: StateFlow<List<ImageItem>> = _images.asStateFlow()

  private val _settings = MutableStateFlow(
    PdfSettings(
      outputFileName = "Converted_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}"
    )
  )
  val settings: StateFlow<PdfSettings> = _settings.asStateFlow()

  private val _conversionState = MutableStateFlow<ConversionState>(ConversionState.Idle)
  val conversionState: StateFlow<ConversionState> = _conversionState.asStateFlow()

  private val _isReadingImages = MutableStateFlow(false)
  val isReadingImages: StateFlow<Boolean> = _isReadingImages.asStateFlow()

  private val _selectedImageIds = MutableStateFlow<Set<String>>(emptySet())
  val selectedImageIds: StateFlow<Set<String>> = _selectedImageIds.asStateFlow()

  // Batch AI cleaning states
  private val _isBatchCleaning = MutableStateFlow(false)
  val isBatchCleaning: StateFlow<Boolean> = _isBatchCleaning.asStateFlow()

  private val _batchCleanProgress = MutableStateFlow(0f)
  val batchCleanProgress: StateFlow<Float> = _batchCleanProgress.asStateFlow()

  private val _batchCleanStatusText = MutableStateFlow("")
  val batchCleanStatusText: StateFlow<String> = _batchCleanStatusText.asStateFlow()

  // Dynamic estimated PDF output size
  val estimatedPdfSize: StateFlow<Long> = combine(_images, _settings) { imgs, currentSettings ->
    ImageUtils.estimatePdfSize(imgs, currentSettings)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

  // Total raw original size
  val totalRawSize: StateFlow<Long> = _images.map { imgs ->
    imgs.sumOf { it.currentSizeBytes }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

  fun initApiKey(context: Context) {
    val prefs = context.getSharedPreferences("pdf_settings", Context.MODE_PRIVATE)
    val savedKey = prefs.getString("gemini_api_key", "") ?: ""
    if (savedKey.isNotBlank()) {
      _settings.value = _settings.value.copy(customGeminiApiKey = savedKey)
    }
  }

  fun saveGeminiApiKey(context: Context, key: String) {
    _settings.value = _settings.value.copy(customGeminiApiKey = key)
    context.getSharedPreferences("pdf_settings", Context.MODE_PRIVATE)
      .edit()
      .putString("gemini_api_key", key)
      .apply()
  }

  fun addImages(context: Context, uris: List<Uri>) {
    if (uris.isEmpty()) return
    viewModelScope.launch {
      _isReadingImages.value = true
      val newItems = mutableListOf<ImageItem>()
      for (uri in uris) {
        try {
          val item = ImageUtils.extractImageItem(context, uri).copy(isOcrScanning = true)
          newItems.add(item)
        } catch (_: Exception) {}
      }
      _images.value = _images.value + newItems
      _isReadingImages.value = false

      val autoClean = _settings.value.autoRemoveBorders || _settings.value.superResolutionEnhancer

      // Background asynchronous OCR extraction using ML Kit without blocking UI
      newItems.forEach { item ->
        launch(Dispatchers.IO) {
          try {
            val ocrResult = OcrUtils.processImageOcr(context, item.activeUri, item.rotationDegrees)
            val pageInfo = OcrUtils.extractBookPageNumber(ocrResult.blocks, item.height, item.width)
            _images.value = _images.value.map { current ->
              if (current.id == item.id) {
                current.copy(
                  ocrText = ocrResult.fullText.ifBlank { null },
                  ocrBlocks = ocrResult.blocks,
                  isOcrScanning = false,
                  detectedPageBadge = pageInfo?.second ?: current.detectedPageBadge
                )
              } else {
                current
              }
            }

            if (autoClean) {
              enhanceImage(context, item.id)
            }
          } catch (_: Exception) {
            _images.value = _images.value.map { current ->
              if (current.id == item.id) current.copy(isOcrScanning = false) else current
            }
          }
        }
      }
    }
  }

  fun triggerOcrScan(context: Context, id: String) {
    val item = _images.value.find { it.id == id } ?: return
    _images.value = _images.value.map {
      if (it.id == id) it.copy(isOcrScanning = true) else it
    }
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val ocrResult = OcrUtils.processImageOcr(context, item.activeUri, item.rotationDegrees)
        val pageInfo = OcrUtils.extractBookPageNumber(ocrResult.blocks, item.height, item.width)
        _images.value = _images.value.map { current ->
          if (current.id == id) {
            current.copy(
              ocrText = ocrResult.fullText.ifBlank { null },
              ocrBlocks = ocrResult.blocks,
              isOcrScanning = false,
              detectedPageBadge = pageInfo?.second ?: current.detectedPageBadge
            )
          } else {
            current
          }
        }
      } catch (_: Exception) {
        _images.value = _images.value.map { current ->
          if (current.id == id) current.copy(isOcrScanning = false) else current
        }
      }
    }
  }

  fun enhanceImage(context: Context, id: String) {
    val item = _images.value.find { it.id == id } ?: return
    viewModelScope.launch {
      _images.value = _images.value.map {
        if (it.id == id) it.copy(isEnhancing = true) else it
      }
      try {
        val originalBitmap = ImageUtils.loadBitmap(context, item.originalUri)
        if (originalBitmap != null) {
          val cropResult = SmartCropUtils.autoCropBookScreenshot(
            bitmap = originalBitmap,
            ocrBlocks = item.ocrBlocks,
            cropMode = _settings.value.cropMode,
            customGeminiApiKey = _settings.value.customGeminiApiKey,
          )

          val enhancedResult = ImageEnhancerUtils.enhanceBookImage(
            source = cropResult.croppedBitmap,
            enableUpscale = true,
            enableSharpening = true,
            enableBackgroundCleaning = true,
          )

          val cacheFile = File(context.cacheDir, "enhanced_${item.id}_${System.currentTimeMillis()}.png")
          withContext(Dispatchers.IO) {
            FileOutputStream(cacheFile).use { fos ->
              enhancedResult.enhancedBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos)
              fos.flush()
            }
          }

          val enhancedUri = Uri.fromFile(cacheFile)

          _images.value = _images.value.map {
            if (it.id == id) {
              it.copy(
                enhancedUri = enhancedUri,
                enhancedWidth = enhancedResult.enhancedBitmap.width,
                enhancedHeight = enhancedResult.enhancedBitmap.height,
                enhancedSizeBytes = cacheFile.length(),
                isAiEnhanced = true,
                isEnhancing = false,
                enhancementDetails = "${cropResult.methodUsed} • ${enhancedResult.details}"
              )
            } else it
          }

          triggerOcrScan(context, id)
        } else {
          _images.value = _images.value.map {
            if (it.id == id) it.copy(isEnhancing = false) else it
          }
        }
      } catch (_: Exception) {
        _images.value = _images.value.map {
          if (it.id == id) it.copy(isEnhancing = false) else it
        }
      }
    }
  }

  fun revertImageToOriginal(id: String) {
    _images.value = _images.value.map {
      if (it.id == id) it.copy(isAiEnhanced = false) else it
    }
  }

  fun toggleImageAiEnhancement(context: Context, id: String) {
    val item = _images.value.find { it.id == id } ?: return
    if (item.isAiEnhanced) {
      revertImageToOriginal(id)
    } else if (item.enhancedUri != null) {
      _images.value = _images.value.map {
        if (it.id == id) it.copy(isAiEnhanced = true) else it
      }
    } else {
      enhanceImage(context, id)
    }
  }

  fun batchCleanAllPages(context: Context) {
    val currentImages = _images.value
    if (currentImages.isEmpty()) return
    viewModelScope.launch(Dispatchers.IO) {
      _isBatchCleaning.value = true
      val total = currentImages.size
      for (i in 0 until total) {
        val item = _images.value.getOrNull(i) ?: continue
        withContext(Dispatchers.Main) {
          _batchCleanProgress.value = (i + 1).toFloat() / total
          _batchCleanStatusText.value = "Cleaning screenshot ${i + 1} of $total..."
        }

        var originalBitmap: Bitmap? = null
        var croppedBitmap: Bitmap? = null
        var enhancedBitmap: Bitmap? = null

        try {
          originalBitmap = ImageUtils.loadBitmap(context, item.originalUri)
          if (originalBitmap != null) {
            val cropResult = SmartCropUtils.autoCropBookScreenshot(
              bitmap = originalBitmap,
              ocrBlocks = item.ocrBlocks,
              cropMode = _settings.value.cropMode,
              customGeminiApiKey = _settings.value.customGeminiApiKey,
            )
            croppedBitmap = cropResult.croppedBitmap

            val enhancedResult = ImageEnhancerUtils.enhanceBookImage(
              source = croppedBitmap,
              enableUpscale = true,
              enableSharpening = true,
              enableBackgroundCleaning = true,
              enableOverlayErasing = true,
            )
            enhancedBitmap = enhancedResult.enhancedBitmap

            val cacheFile = File(context.cacheDir, "enhanced_${item.id}_${System.currentTimeMillis()}.png")
            FileOutputStream(cacheFile).use { fos ->
              enhancedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
              fos.flush()
            }
            val enhancedUri = Uri.fromFile(cacheFile)

            withContext(Dispatchers.Main) {
              _images.value = _images.value.map {
                if (it.id == item.id) {
                  it.copy(
                    enhancedUri = enhancedUri,
                    enhancedWidth = enhancedResult.enhancedBitmap.width,
                    enhancedHeight = enhancedResult.enhancedBitmap.height,
                    enhancedSizeBytes = cacheFile.length(),
                    isAiEnhanced = true,
                    isEnhancing = false,
                    enhancementDetails = "${cropResult.methodUsed} • ${enhancedResult.details}"
                  )
                } else it
              }
            }
          }
        } catch (_: Exception) {
        } finally {
          // Immediately recycle intermediate bitmaps to guarantee zero OOM
          originalBitmap?.recycle()
          if (croppedBitmap != null && croppedBitmap != originalBitmap) {
            croppedBitmap.recycle()
          }
          if (enhancedBitmap != null && enhancedBitmap != croppedBitmap) {
            enhancedBitmap.recycle()
          }
        }
      }
      withContext(Dispatchers.Main) {
        _isBatchCleaning.value = false
        _batchCleanStatusText.value = "All screenshots cleaned & enhanced!"
      }
    }
  }

  fun removeImage(id: String) {
    _images.value = _images.value.filter { it.id != id }
    _selectedImageIds.value = _selectedImageIds.value - id
  }

  fun removeSelectedImages() {
    val selected = _selectedImageIds.value
    if (selected.isEmpty()) return
    _images.value = _images.value.filter { it.id !in selected }
    _selectedImageIds.value = emptySet()
  }

  fun toggleImageSelection(id: String) {
    _selectedImageIds.value = if (_selectedImageIds.value.contains(id)) {
      _selectedImageIds.value - id
    } else {
      _selectedImageIds.value + id
    }
  }

  fun selectAll() {
    _selectedImageIds.value = _images.value.map { it.id }.toSet()
  }

  fun clearSelection() {
    _selectedImageIds.value = emptySet()
  }

  fun reverseOrder() {
    _images.value = _images.value.reversed()
  }

  fun moveImage(fromIndex: Int, toIndex: Int) {
    if (fromIndex !in _images.value.indices || toIndex !in _images.value.indices) return
    val current = _images.value.toMutableList()
    val item = current.removeAt(fromIndex)
    current.add(toIndex, item)
    _images.value = current
  }

  fun rotateImage(context: Context, id: String, degreesClockwise: Int = 90) {
    _images.value = _images.value.map { item ->
      if (item.id == id) {
        val newDeg = (item.rotationDegrees + degreesClockwise) % 360
        item.copy(rotationDegrees = if (newDeg < 0) newDeg + 360 else newDeg)
      } else {
        item
      }
    }
    triggerOcrScan(context, id)
  }

  fun rotateSelected(context: Context, degreesClockwise: Int = 90) {
    val selected = _selectedImageIds.value
    if (selected.isEmpty()) return
    _images.value = _images.value.map { item ->
      if (item.id in selected) {
        val newDeg = (item.rotationDegrees + degreesClockwise) % 360
        item.copy(rotationDegrees = if (newDeg < 0) newDeg + 360 else newDeg)
      } else {
        item
      }
    }
    selected.forEach { triggerOcrScan(context, it) }
  }

  fun rotateImage(context: Context, id: String, clockwise: Boolean) {
    rotateImage(context, id, if (clockwise) 90 else -90)
  }

  fun rotateSelected(context: Context, clockwise: Boolean) {
    rotateSelected(context, if (clockwise) 90 else -90)
  }

  fun toggleSelectImage(id: String) = toggleImageSelection(id)
  fun deselectAll() = clearSelection()
  fun deleteSelected() = removeSelectedImages()
  fun clearAll() = clearAllImages()
  fun moveUp(index: Int) { if (index > 0) moveImage(index, index - 1) }
  fun moveDown(index: Int) { if (index < _images.value.size - 1) moveImage(index, index + 1) }
  fun sortByName(ascending: Boolean = true) {
    _images.value = if (ascending) {
      _images.value.sortedBy { it.fileName.lowercase() }
    } else {
      _images.value.sortedByDescending { it.fileName.lowercase() }
    }
  }

  /**
   * Smart Auto-Sort by Book Page Number (OCR):
   * Scans bottom corners / page regions to extract printed page numbers (e.g. 366, 368, 370),
   * falling back to filename or timestamp if not recognized, strictly sorting ascending.
   */
  fun sortByPageNumber(context: Context, onSorted: (String) -> Unit = {}) {
    val current = _images.value
    if (current.isEmpty()) return

    viewModelScope.launch(Dispatchers.IO) {
      // Ensure all images have page numbers analyzed
      val itemsWithPages = current.map { item ->
        val pageNumber = if (item.detectedPageBadge != null) {
          item.detectedPageBadge.filter { it.isDigit() }.toIntOrNull()
        } else if (item.ocrBlocks.isNotEmpty()) {
          val extracted = OcrUtils.extractBookPageNumber(item.ocrBlocks, item.height, item.width)
          extracted?.first
        } else {
          try {
            val ocr = OcrUtils.processImageOcr(context, item.activeUri, item.rotationDegrees)
            val extracted = OcrUtils.extractBookPageNumber(ocr.blocks, item.height, item.width)
            extracted?.first
          } catch (_: Exception) {
            null
          }
        }
        item to pageNumber
      }

      // Check if we found page numbers for at least some images
      val detectedCount = itemsWithPages.count { it.second != null }

      val sorted = itemsWithPages.sortedWith(
        compareBy<Pair<ImageItem, Int?>> { it.second ?: Int.MAX_VALUE }
          .thenBy { it.first.fileName.lowercase() }
      ).map { (item, page) ->
        if (page != null && item.detectedPageBadge == null) {
          item.copy(detectedPageBadge = "p. $page")
        } else {
          item
        }
      }

      withContext(Dispatchers.Main) {
        _images.value = sorted
        val msg = if (detectedCount > 0) {
          "Sorted $detectedCount images by book page number!"
        } else {
          "Sorted images by filename (no page numbers found)"
        }
        onSorted(msg)
      }
    }
  }

  fun clearAllImages() {
    _images.value = emptyList()
    _selectedImageIds.value = emptySet()
  }

  fun updateSettings(newSettings: PdfSettings) {
    _settings.value = newSettings
  }

  fun updateSettings(updater: (PdfSettings) -> PdfSettings) {
    _settings.value = updater(_settings.value)
  }

  fun updateOutputFileName(name: String) {
    _settings.value = _settings.value.copy(outputFileName = name.trim())
  }

  fun updatePasswordSecurity(enable: Boolean, userPass: String, ownerPass: String) {
    _settings.value = _settings.value.copy(
      enableEncryption = enable,
      userPassword = userPass,
      ownerPassword = ownerPass
    )
  }

  fun startConversion(context: Context) {
    val currentImages = _images.value
    if (currentImages.isEmpty()) return

    val currentSettings = _settings.value

    viewModelScope.launch {
      _conversionState.value = ConversionState.Processing(
        current = 0,
        total = currentImages.size,
        status = "Initializing PDF document engine..."
      )

      try {
        val result = PdfGeneratorService.generatePdf(
          context = context,
          images = currentImages,
          settings = currentSettings,
          onProgress = { current, total, status ->
            _conversionState.value = ConversionState.Processing(current, total, status)
          }
        )

        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, result.file)

        var finalDownloadUri: Uri? = null
        var displayPath = "Downloads/${result.fileName}"
        var autoSaved = false

        try {
          val autoSaveResult = DownloadUtils.savePdfToDownloads(
            context = context,
            sourceFile = result.file,
            desiredFileName = result.fileName
          )
          autoSaveResult.onSuccess { dl ->
            finalDownloadUri = dl.uri
            displayPath = dl.displayPath
            autoSaved = true
          }
        } catch (_: Exception) {}

        _conversionState.value = ConversionState.Success(
          result = result,
          contentUri = contentUri,
          downloadUri = finalDownloadUri,
          savedPathDisplay = displayPath,
          autoDownloaded = autoSaved,
        )
      } catch (e: Exception) {
        _conversionState.value = ConversionState.Error(
          e.message ?: "Failed to generate PDF document. Please try again."
        )
      }
    }
  }

  fun downloadToDownloads(context: Context, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
    val currentState = _conversionState.value
    if (currentState !is ConversionState.Success) {
      onError("No generated PDF ready to download")
      return
    }

    viewModelScope.launch {
      val res = DownloadUtils.savePdfToDownloads(context, currentState.result.file, currentState.result.fileName)
      res.fold(
        onSuccess = { downloadResult ->
          _conversionState.value = currentState.copy(
            downloadUri = downloadResult.uri,
            savedPathDisplay = downloadResult.displayPath,
            autoDownloaded = true
          )
          onSuccess("PDF successfully downloaded to ${downloadResult.displayPath}!")
        },
        onFailure = { ex ->
          onError(ex.message ?: "Failed to save PDF to Downloads")
        }
      )
    }
  }

  fun saveToDestination(context: Context, destinationUri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val currentState = _conversionState.value
    if (currentState !is ConversionState.Success) {
      onError("No generated PDF available to save")
      return
    }

    viewModelScope.launch(Dispatchers.IO) {
      try {
        val sourceFile = currentState.result.file
        context.contentResolver.openOutputStream(destinationUri, "w")?.use { outputStream ->
          FileInputStream(sourceFile).use { inputStream ->
            inputStream.copyTo(outputStream)
          }
          outputStream.flush()
        } ?: throw IllegalStateException("Could not open destination stream")

        withContext(Dispatchers.Main) {
          onSuccess()
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          onError(e.message ?: "Failed to save PDF to selected location")
        }
      }
    }
  }

  fun dismissDialog() {
    if (_conversionState.value is ConversionState.Error) {
      _conversionState.value = ConversionState.Idle
    }
  }

  fun resetSuccess() {
    _conversionState.value = ConversionState.Idle
  }
}
