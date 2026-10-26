package com.example.viewmodel

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryKind
import com.example.data.HistoryRepository
import com.example.model.AudioConversionSettings
import com.example.model.AudioConvertItem
import com.example.service.AudioConverterService
import com.example.util.queryUriInfo
import com.example.util.userMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private val _currentName = MutableStateFlow("")
    val currentName: StateFlow<String> = _currentName.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var job: Job? = null

    fun addAudioFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val app = getApplication<Application>()
        viewModelScope.launch {
            val newItems = withContext(Dispatchers.IO) {
                uris.map { uri ->
                    val info = app.queryUriInfo(uri, "Audio")
                    var duration = 0L
                    var sampleRate = 0
                    var bitrate = 0
                    var mime = info.mimeType
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(app, uri)
                        duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                        bitrate = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0) / 1000
                        mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: mime
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            sampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull() ?: 0
                        }
                    } catch (_: Throwable) {
                    } finally {
                        runCatching { retriever.release() }
                    }
                    AudioConvertItem(
                        uri = uri,
                        name = info.name,
                        originalSizeBytes = info.sizeBytes,
                        durationMs = duration,
                        sampleRate = sampleRate,
                        bitrateKbps = bitrate,
                        mimeType = mime,
                        error = if (duration <= 0L) "Durata non leggibile: il file potrebbe non essere audio" else null
                    )
                }
            }
            _items.update { it + newItems }
        }
    }

    fun removeItem(id: String) {
        _items.update { list -> list.filterNot { it.id == id } }
    }

    fun setTrim(id: String, startMs: Long?, endMs: Long?) {
        setItem(id) { item ->
            val start = startMs?.takeIf { it > 0 }
            val end = endMs?.takeIf { it < item.durationMs }
            item.copy(trimStartMs = start, trimEndMs = end, convertedSizeBytes = null, outputUri = null)
        }
    }

    fun clearAll() {
        job?.cancel()
        _isTranscoding.value = false
        _items.value = emptyList()
        _errorMessage.value = null
    }

    fun updateSettings(newSettings: AudioConversionSettings) {
        _settings.value = newSettings
        if (!_isTranscoding.value) {
            _items.update { list -> list.map { it.copy(convertedSizeBytes = null, outputUri = null) } }
        }
    }

    fun transcodeAll() {
        val queue = _items.value.filter { it.convertedSizeBytes == null && it.durationMs > 0 }
        if (queue.isEmpty() || _isTranscoding.value) return
        val settings = _settings.value

        _isTranscoding.value = true
        _errorMessage.value = null
        _progress.value = 0f
        job = viewModelScope.launch {
            try {
                var failures = 0
                for ((index, item) in queue.withIndex()) {
                    _currentName.value = item.name
                    setItem(item.id) { it.copy(isProcessing = true, progress = 0f, error = null) }
                    var lastReported = -1f
                    audioService.transcodeAudio(item, settings) { p ->
                        // The decoder reports every few ms: only publish visible changes.
                        if (p - lastReported >= 0.01f || p >= 1f) {
                            lastReported = p
                            _progress.value = (index + p) / queue.size
                            setItem(item.id) { it.copy(progress = p) }
                        }
                    }.onSuccess { (uri, size) ->
                        HistoryRepository.record(getApplication<Application>(), uri, "Converti audio", HistoryKind.AUDIO, item.name, size)
                        setItem(item.id) { it.copy(isProcessing = false, progress = 1f, convertedSizeBytes = size, outputUri = uri) }
                    }.onFailure { err ->
                        failures++
                        setItem(item.id) { it.copy(isProcessing = false, error = err.userMessage("Conversione non riuscita")) }
                    }
                }
                if (failures > 0) _errorMessage.value = if (failures == 1) "1 file non è stato convertito" else "$failures file non sono stati convertiti"
                _progress.value = 1f
            } finally {
                _isTranscoding.value = false
                _items.update { list -> list.map { if (it.isProcessing) it.copy(isProcessing = false) else it } }
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }

    private fun setItem(id: String, transform: (AudioConvertItem) -> AudioConvertItem) {
        _items.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}
