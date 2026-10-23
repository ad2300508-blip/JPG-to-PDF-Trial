package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ArchiveFileItem
import com.example.model.ArchiveResult
import com.example.model.ArchiveSettings
import com.example.model.ZipCompressionLevel
import com.example.service.ArchiveService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ArchiveViewModel(application: Application) : AndroidViewModel(application) {

  private val _items = MutableStateFlow<List<ArchiveFileItem>>(emptyList())
  val items: StateFlow<List<ArchiveFileItem>> = _items.asStateFlow()

  private val _settings = MutableStateFlow(ArchiveSettings())
  val settings: StateFlow<ArchiveSettings> = _settings.asStateFlow()

  private val _isProcessing = MutableStateFlow(false)
  val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

  private val _progressFraction = MutableStateFlow(0f)
  val progressFraction: StateFlow<Float> = _progressFraction.asStateFlow()

  private val _statusMessage = MutableStateFlow("")
  val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

  private val _lastResult = MutableStateFlow<ArchiveResult?>(null)
  val lastResult: StateFlow<ArchiveResult?> = _lastResult.asStateFlow()

  private val _exportMessage = MutableStateFlow<String?>(null)
  val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

  fun addUris(uris: List<Uri>) {
    viewModelScope.launch {
      val context = getApplication<Application>()
      val newItems = uris.map { uri ->
        ArchiveService.extractMetadata(context, uri)
      }
      _items.update { current -> current + newItems }
    }
  }

  fun removeItem(id: String) {
    _items.update { current ->
      current.filter { it.id != id }
    }
  }

  fun clearAll() {
    _items.value = emptyList()
    _lastResult.value = null
    _statusMessage.value = ""
  }

  fun updateSettings(updater: (ArchiveSettings) -> ArchiveSettings) {
    _settings.update(updater)
  }

  fun createArchive() {
    val currentItems = _items.value
    if (currentItems.isEmpty() || _isProcessing.value) return

    viewModelScope.launch {
      _isProcessing.value = true
      _progressFraction.value = 0f
      val currentSettings = _settings.value
      val context = getApplication<Application>()

      val result = ArchiveService.createZipArchive(
        context = context,
        items = currentItems,
        settings = currentSettings,
        onProgress = { progress, currentFile ->
          _progressFraction.value = progress
          _statusMessage.value = "Compressing $currentFile..."
        }
      )

      if (result.isSuccess) {
        val archiveRes = result.getOrThrow()
        _lastResult.value = archiveRes
        _statusMessage.value = "Archive created successfully!"

        // Automatically export archive to Downloads/OmniConvert/Archives
        val exportRes = ArchiveService.exportArchiveToDownloads(context, archiveRes)
        if (exportRes.isSuccess) {
          _exportMessage.value = "Saved ${archiveRes.fileName} to Downloads/OmniConvert/Archives"
        }
      } else {
        _statusMessage.value = "Failed: ${result.exceptionOrNull()?.localizedMessage}"
      }

      _isProcessing.value = false
    }
  }

  fun clearExportMessage() {
    _exportMessage.value = null
  }
}
