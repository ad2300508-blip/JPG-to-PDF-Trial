package com.example.viewmodel

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryKind
import com.example.data.HistoryRepository
import com.example.model.AudioConversionSettings
import com.example.model.AudioConvertItem
import com.example.service.AudioConverterService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AudioLabViewModel(application: Application) : AndroidViewModel(application) {

    private val audioService = AudioConverterService(application.applicationContext)

    private val _items = MutableStateFlow<List<AudioConvertItem>>(emptyList())
    val items: StateFlow<List<AudioConvertItem>> = _items.asStateFlow()

    private val _settings = MutableStateFlow(AudioConversionSettings())
    val settings: StateFlow<AudioConversionSettings> = _settings.asStateFlow()

    private val _isTranscoding = MutableStateFlow(false)
    val isTranscoding: StateFlow<Boolean> = _isTranscoding.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun addAudioFiles(uris: List<Uri>) {
        val context = getApplication<Application>()
        val resolver = context.contentResolver
        val newItems = uris.map { uri ->
            var name = "Audio Track"
            var size = 0L
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: "Audio Track"
                    if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
                }
            }

            var duration = 0L
            var sampleRate = 44100
            var channels = 2
            var mime = "audio/*"
            try {
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(context, uri)
                duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                mime = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "audio/*"
                mmr.release()
            } catch (_: Throwable) {}

            AudioConvertItem(
                uri = uri,
                name = name,
                originalSizeBytes = size,
                durationMs = duration,
                sampleRate = sampleRate,
                channels = channels,
                mimeType = mime
            )
        }
        _items.value = _items.value + newItems
    }

    fun removeItem(id: String) {
        _items.value = _items.value.filterNot { it.id == id }
    }

    fun clearAll() {
        _items.value = emptyList()
        _errorMessage.value = null
    }

    fun updateSettings(newSettings: AudioConversionSettings) {
        _settings.value = newSettings
    }

    fun transcodeAll() {
        val currentItems = _items.value
        if (currentItems.isEmpty()) return

        viewModelScope.launch {
            _isTranscoding.value = true
            _errorMessage.value = null
            _progress.value = 0f

            for ((idx, item) in currentItems.withIndex()) {
                _items.value = _items.value.map {
                    if (it.id == item.id) it.copy(isProcessing = true) else it
                }

                val res = audioService.transcodeAudio(item, _settings.value) { p ->
                    val overall = (idx.toFloat() + p) / currentItems.size.toFloat()
                    _progress.value = overall
                }

                res.onSuccess { pair ->
                    HistoryRepository.record(getApplication<Application>(), pair.first, "Converti audio", HistoryKind.AUDIO, item.name, pair.second)
                    _items.value = _items.value.map {
                        if (it.id == item.id) it.copy(isProcessing = false, convertedSizeBytes = pair.second, error = null) else it
                    }
                }.onFailure { err ->
                    _items.value = _items.value.map {
                        if (it.id == item.id) it.copy(isProcessing = false, error = err.localizedMessage) else it
                    }
                }
            }

            _isTranscoding.value = false
            _progress.value = 1f
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}
