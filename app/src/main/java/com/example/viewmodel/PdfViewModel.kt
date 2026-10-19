package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ImageItem
import com.example.model.PdfSettings
import com.example.service.PdfGeneratorService
import com.example.util.DownloadUtils
import com.example.util.ImageUtils
import com.example.util.OcrUtils
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

  // Dynamic estimated PDF output size
  val estimatedPdfSize: StateFlow<Long> = combine(_images, _settings) { imgs, currentSettings ->
    ImageUtils.estimatePdfSize(imgs, currentSettings)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

  // Total raw original size
  val totalRawSize: StateFlow<Long> = _images.map { imgs ->
    imgs.sumOf { it.sizeBytes }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

  fun addImages(context: Context, uris: List<Uri>) {
    if (uris.isEmpty()) return
    viewModelScope.launch {
      _isReadingImages.value = true
      val newItems = mutableListOf<ImageItem>()
      for (uri in uris) {
        try {
          // Initialize with isOcrScanning = true so user gets immediate visual feedback
          val item = ImageUtils.extractImageItem(context, uri).copy(isOcrScanning = true)
          newItems.add(item)
        } catch (_: Exception) {}
      }
      _images.value = _images.value + newItems
      _isReadingImages.value = false

      // Background asynchronous OCR extraction using ML Kit without blocking UI
      newItems.forEach { item ->
        launch(Dispatchers.IO) {
          try {
            val ocrResult = OcrUtils.processImageOcr(context, item.uri, item.rotationDegrees)
            _images.value = _images.value.map { current ->
              if (current.id == item.id) {
                current.copy(
                  ocrText = ocrResult.fullText.ifBlank { null },
                  ocrBlocks = ocrResult.blocks,
                  isOcrScanning = false
                )
              } else {
                current
              }
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
        val ocrResult = OcrUtils.processImageOcr(context, item.uri, item.rotationDegrees)
        _images.value = _images.value.map { current ->
          if (current.id == id) {
            current.copy(
              ocrText = ocrResult.fullText.ifBlank { null },
              ocrBlocks = ocrResult.blocks,
              isOcrScanning = false
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

  fun removeImage(id: String) {
    _images.value = _images.value.filterNot { it.id == id }
    _selectedImageIds.value = _selectedImageIds.value - id
    if (_conversionState.value is ConversionState.Success) {
      _conversionState.value = ConversionState.Idle
    }
  }

  fun toggleSelectImage(id: String) {
    val current = _selectedImageIds.value
    _selectedImageIds.value = if (id in current) current - id else current + id
  }

  fun selectAll() {
    _selectedImageIds.value = _images.value.map { it.id }.toSet()
  }

  fun deselectAll() {
    _selectedImageIds.value = emptySet()
  }

  fun deleteSelected() {
    val selected = _selectedImageIds.value
    if (selected.isEmpty()) return
    _images.value = _images.value.filterNot { it.id in selected }
    _selectedImageIds.value = emptySet()
    if (_conversionState.value is ConversionState.Success) {
      _conversionState.value = ConversionState.Idle
    }
  }

  fun reverseOrder() {
    _images.value = _images.value.reversed()
    if (_conversionState.value is ConversionState.Success) {
      _conversionState.value = ConversionState.Idle
    }
  }

  fun rotateImage(context: Context, id: String, clockwise: Boolean = true) {
    val delta = if (clockwise) 90 else -90
    var targetItem: ImageItem? = null
    _images.value = _images.value.map { item ->
      if (item.id == id) {
        val newRot = (item.rotationDegrees + delta + 360) % 360
        val updated = item.copy(rotationDegrees = newRot, isOcrScanning = true)
        targetItem = updated
        updated
      } else {
        item
      }
    }
    if (_conversionState.value is ConversionState.Success) {
      _conversionState.value = ConversionState.Idle
    }

    targetItem?.let { item ->
      viewModelScope.launch(Dispatchers.IO) {
        try {
          val ocrResult = OcrUtils.processImageOcr(context, item.uri, item.rotationDegrees)
          _images.value = _images.value.map { current ->
            if (current.id == item.id) {
              current.copy(
                ocrText = ocrResult.fullText.ifBlank { null },
                ocrBlocks = ocrResult.blocks,
                isOcrScanning = false
              )
            } else {
              current
            }
          }
        } catch (_: Exception) {
          _images.value = _images.value.map { current ->
            if (current.id == item.id) current.copy(isOcrScanning = false) else current
          }
        }
      }
    }
  }

  fun rotateSelected(context: Context, clockwise: Boolean = true) {
    val selected = _selectedImageIds.value
    if (selected.isEmpty()) return
    val delta = if (clockwise) 90 else -90
    val updatedList = mutableListOf<ImageItem>()

    _images.value = _images.value.map { item ->
      if (item.id in selected) {
        val newRot = (item.rotationDegrees + delta + 360) % 360
        val updated = item.copy(rotationDegrees = newRot, isOcrScanning = true)
        updatedList.add(updated)
        updated
      } else {
        item
      }
    }
    if (_conversionState.value is ConversionState.Success) {
      _conversionState.value = ConversionState.Idle
    }

    updatedList.forEach { item ->
      viewModelScope.launch(Dispatchers.IO) {
        try {
          val ocrResult = OcrUtils.processImageOcr(context, item.uri, item.rotationDegrees)
          _images.value = _images.value.map { current ->
            if (current.id == item.id) {
              current.copy(
                ocrText = ocrResult.fullText.ifBlank { null },
                ocrBlocks = ocrResult.blocks,
                isOcrScanning = false
              )
            } else {
              current
            }
          }
        } catch (_: Exception) {
          _images.value = _images.value.map { current ->
            if (current.id == item.id) current.copy(isOcrScanning = false) else current
          }
        }
      }
    }
  }

  fun sortByName(ascending: Boolean = true) {
    _images.value = if (ascending) {
      _images.value.sortedBy { it.fileName.lowercase() }
    } else {
      _images.value.sortedByDescending { it.fileName.lowercase() }
    }
  }

  fun moveUp(index: Int) {
    if (index <= 0 || index >= _images.value.size) return
    val list = _images.value.toMutableList()
    val item = list.removeAt(index)
    list.add(index - 1, item)
    _images.value = list
  }

  fun moveDown(index: Int) {
    if (index < 0 || index >= _images.value.size - 1) return
    val list = _images.value.toMutableList()
    val item = list.removeAt(index)
    list.add(index + 1, item)
    _images.value = list
  }

  fun clearAll() {
    _images.value = emptyList()
    _selectedImageIds.value = emptySet()
    _conversionState.value = ConversionState.Idle
  }

  fun updateSettings(updater: (PdfSettings) -> PdfSettings) {
    _settings.value = updater(_settings.value)
    if (_conversionState.value is ConversionState.Success) {
      _conversionState.value = ConversionState.Idle
    }
  }

  fun startConversion(context: Context) {
    val currentImages = _images.value
    if (currentImages.isEmpty()) return

    viewModelScope.launch {
      _conversionState.value = ConversionState.Processing(0, currentImages.size, "Preparing images...")
      try {
        val result = PdfGeneratorService.generatePdf(
          context = context,
          images = currentImages,
          settings = _settings.value,
          onProgress = { cur, tot, status ->
            _conversionState.value = ConversionState.Processing(cur, tot, status)
          }
        )

        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, result.file)

        // Automatically save directly to public Downloads folder
        val downloadRes = DownloadUtils.savePdfToDownloads(context, result.file, result.fileName)
        val downloadUri = downloadRes.getOrNull()?.uri
        val displayPath = downloadRes.getOrNull()?.displayPath ?: "Downloads/${result.fileName}"
        val autoDownloaded = downloadRes.isSuccess

        _conversionState.value = ConversionState.Success(
          result = result,
          contentUri = contentUri,
          downloadUri = downloadUri,
          savedPathDisplay = displayPath,
          autoDownloaded = autoDownloaded,
        )
      } catch (e: Exception) {
        _conversionState.value = ConversionState.Error(e.message ?: "Failed to generate PDF")
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
