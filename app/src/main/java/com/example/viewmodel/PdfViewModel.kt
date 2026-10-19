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
          val item = ImageUtils.extractImageItem(context, uri)
          newItems.add(item)
        } catch (_: Exception) {}
      }
      _images.value = _images.value + newItems
      _isReadingImages.value = false
    }
  }

  fun removeImage(id: String) {
    _images.value = _images.value.filterNot { it.id == id }
    if (_conversionState.value is ConversionState.Success) {
      _conversionState.value = ConversionState.Idle
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
