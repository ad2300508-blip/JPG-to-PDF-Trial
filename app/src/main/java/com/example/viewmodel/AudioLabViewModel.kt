package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AudioBitrate
import com.example.model.AudioLabItem
import com.example.model.AudioLabSettings
import com.example.model.AudioOutputMode
import com.example.service.AudioConverterService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AudioLabViewModel(application: Application) : AndroidViewModel(application) {

  private val _items = MutableStateFlow<List<AudioLabItem>>(emptyList())
  val items: StateFlow<List<AudioLabItem>> = _items.asStateFlow()

  private val _settings = MutableStateFlow(AudioLabSettings())
  val settings: StateFlow<AudioLabSettings> = _settings.asStateFlow()

  private val _isProcessing = MutableStateFlow(false)
  val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

  private val _progressFraction = MutableStateFlow(0f)
  val progressFraction: StateFlow<Float> = _progressFraction.asStateFlow()

  private val _statusMessage = MutableStateFlow("")
  val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

  private val _exportMessage = MutableStateFlow<String?>(null)
  val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

  fun addUris(uris: List<Uri>) {
    viewModelScope.launch {
      val context = getApplication<Application>()
      val newItems = uris.map { uri ->
        AudioConverterService.extractMetadata(context, uri)
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
    _statusMessage.value = ""
  }

  fun updateSettings(updater: (AudioLabSettings) -> AudioLabSettings) {
    _settings.update(updater)
  }

  fun transcodeAll() {
    val currentItems = _items.value
    if (currentItems.isEmpty() || _isProcessing.value) return

    viewModelScope.launch {
      _isProcessing.value = true
      _progressFraction.value = 0f
      val currentSettings = _settings.value
      val context = getApplication<Application>()
      val total = currentItems.size

      val updatedList = currentItems.toMutableList()

      for (i in 0 until total) {
        val item = updatedList[i]
        _statusMessage.value = "Transcoding ${i + 1} of $total: ${item.fileName}..."
        _progressFraction.value = i.toFloat() / total.toFloat()

        updatedList[i] = item.copy(isProcessing = true)
        _items.value = updatedList.toList()

        val result = AudioConverterService.transcodeAudio(context, item, currentSettings)
        if (result.isSuccess) {
          updatedList[i] = result.getOrThrow()
        } else {
          updatedList[i] = item.copy(
            isProcessing = false,
            error = result.exceptionOrNull()?.localizedMessage ?: "Transcoding failed"
          )
        }
        _items.value = updatedList.toList()
      }

      _progressFraction.value = 1f
      _statusMessage.value = "Audio transcoding complete!"
      _isProcessing.value = false
    }
  }

  fun exportAllToDownloads() {
    val currentItems = _items.value.filter { it.convertedFile != null }
    if (currentItems.isEmpty()) return

    viewModelScope.launch {
      val context = getApplication<Application>()
      val results = AudioConverterService.exportBatchToDownloads(context, currentItems)
      if (results.isNotEmpty()) {
        _exportMessage.value = "Saved ${results.size} audio files to Downloads/OmniConvert/Audio"
      } else {
        _exportMessage.value = "Failed to export audio files"
      }
    }
  }

  fun clearExportMessage() {
    _exportMessage.value = null
  }
}
