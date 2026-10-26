package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryKind
import com.example.data.HistoryRepository
import com.example.model.ImageConversionSettings
import com.example.model.ImageConvertItem
import com.example.service.ImageConverterService
import com.example.util.BitmapLoader
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

class ImageLabViewModel(application: Application) : AndroidViewModel(application) {

    private val imageService = ImageConverterService(application.applicationContext)

    private val _items = MutableStateFlow<List<ImageConvertItem>>(emptyList())
    val items: StateFlow<List<ImageConvertItem>> = _items.asStateFlow()

    private val _settings = MutableStateFlow(ImageConversionSettings())
    val settings: StateFlow<ImageConversionSettings> = _settings.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var job: Job? = null

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val app = getApplication<Application>()
        viewModelScope.launch {
            val newItems = withContext(Dispatchers.IO) {
                uris.map { uri ->
                    val info = app.queryUriInfo(uri, "Immagine")
                    val bounds = BitmapLoader.readBounds(app, uri)
                    ImageConvertItem(
                        uri = uri,
                        name = info.name,
                        originalSizeBytes = info.sizeBytes,
                        originalWidth = bounds?.width ?: 0,
                        originalHeight = bounds?.height ?: 0,
                        error = if (bounds == null) "Formato non riconosciuto" else null
                    )
                }
            }
            _items.update { it + newItems }
        }
    }

    fun removeItem(id: String) {
        _items.update { list -> list.filterNot { it.id == id } }
    }

    fun clearAll() {
        job?.cancel()
        _isProcessing.value = false
        _items.value = emptyList()
        _errorMessage.value = null
    }

    fun updateSettings(newSettings: ImageConversionSettings) {
        _settings.value = newSettings
        // Changing options makes previous results stale: allow converting again.
        if (!_isProcessing.value) {
            _items.update { list -> list.map { it.copy(convertedSizeBytes = null, outputUri = null, error = if (it.originalWidth == 0) it.error else null) } }
        }
    }

    fun convertAll() {
        val queue = _items.value.filter { it.convertedSizeBytes == null && it.originalWidth > 0 }
        if (queue.isEmpty() || _isProcessing.value) return
        val settings = _settings.value

        _isProcessing.value = true
        _errorMessage.value = null
        job = viewModelScope.launch {
            try {
                var failures = 0
                for (item in queue) {
                    setItem(item.id) { it.copy(isProcessing = true, error = null) }
                    imageService.convertImage(item, settings)
                        .onSuccess { (uri, size) ->
                            HistoryRepository.record(getApplication<Application>(), uri, "Converti immagini", HistoryKind.IMAGE, item.name, size)
                            setItem(item.id) { it.copy(isProcessing = false, convertedSizeBytes = size, outputUri = uri) }
                        }
                        .onFailure { err ->
                            failures++
                            setItem(item.id) { it.copy(isProcessing = false, error = err.userMessage("Conversione non riuscita")) }
                        }
                }
                if (failures > 0) _errorMessage.value = if (failures == 1) "1 immagine non è stata convertita" else "$failures immagini non sono state convertite"
            } finally {
                _isProcessing.value = false
                _items.update { list -> list.map { if (it.isProcessing) it.copy(isProcessing = false) else it } }
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }

    private fun setItem(id: String, transform: (ImageConvertItem) -> ImageConvertItem) {
        _items.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}
