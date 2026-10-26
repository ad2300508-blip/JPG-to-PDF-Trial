package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryKind
import com.example.data.HistoryRepository
import com.example.service.CollageService
import com.example.service.CollageSettings
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
import java.util.UUID

data class CollageItem(
    val uri: Uri,
    val name: String,
    /** Width / height as shown (EXIF rotation applied). */
    val aspect: Float,
    val id: String = UUID.randomUUID().toString()
)

data class CollageResult(val uri: Uri, val sizeBytes: Long, val mimeType: String)

class CollageViewModel(application: Application) : AndroidViewModel(application) {

    private val service = CollageService(application.applicationContext)

    private val _items = MutableStateFlow<List<CollageItem>>(emptyList())
    val items: StateFlow<List<CollageItem>> = _items.asStateFlow()

    private val _settings = MutableStateFlow(CollageSettings())
    val settings: StateFlow<CollageSettings> = _settings.asStateFlow()

    private val _progress = MutableStateFlow<Float?>(null)
    val progress: StateFlow<Float?> = _progress.asStateFlow()

    private val _result = MutableStateFlow<CollageResult?>(null)
    val result: StateFlow<CollageResult?> = _result.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var job: Job? = null

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val app = getApplication<Application>()
        viewModelScope.launch {
            val room = MAX_PHOTOS - _items.value.size
            if (room <= 0) {
                _errorMessage.value = "Puoi unire al massimo $MAX_PHOTOS foto"
                return@launch
            }
            val added = withContext(Dispatchers.IO) {
                uris.take(room).mapNotNull { uri ->
                    val bounds = BitmapLoader.readBounds(app, uri) ?: return@mapNotNull null
                    val rotated = BitmapLoader.readExif(app, uri).first % 180 != 0
                    val w = if (rotated) bounds.height else bounds.width
                    val h = if (rotated) bounds.width else bounds.height
                    CollageItem(uri, app.queryUriInfo(uri, "Foto").name, w.toFloat() / h.coerceAtLeast(1))
                }
            }
            if (added.size < uris.size) {
                _errorMessage.value = if (uris.size > room) "Ho aggiunto solo le prime $room foto (massimo $MAX_PHOTOS)" else "Alcuni file non sono immagini leggibili"
            }
            _items.update { it + added }
            _result.value = null
        }
    }

    fun remove(id: String) = edit { list -> list.filterNot { it.id == id } }

    /** Swaps two photos: tap one, then the other. */
    fun swap(a: String, b: String) = edit { list ->
        val i = list.indexOfFirst { it.id == a }
        val j = list.indexOfFirst { it.id == b }
        if (i < 0 || j < 0 || i == j) list else list.toMutableList().apply { this[i] = list[j]; this[j] = list[i] }
    }

    fun shuffle() = edit { list -> if (list.size < 2) list else generateSequence { list.shuffled() }.first { it != list } }

    fun updateSettings(transform: (CollageSettings) -> CollageSettings) {
        _settings.update(transform)
        _result.value = null
    }

    fun save() {
        val list = _items.value
        if (list.isEmpty() || _progress.value != null) return
        val settings = _settings.value
        _errorMessage.value = null
        _progress.value = 0f
        job = viewModelScope.launch {
            try {
                service.render(list.map { it.uri }, list.map { it.aspect }, settings) { _progress.value = it }
                    .onSuccess { (uri, size) ->
                        HistoryRepository.record(getApplication<Application>(), uri, "Collage foto", HistoryKind.IMAGE, "Collage", size)
                        _result.value = CollageResult(uri, size, settings.format.mimeType)
                    }
                    .onFailure { _errorMessage.value = it.userMessage("Non sono riuscito a creare il collage") }
            } finally {
                _progress.value = null
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }

    fun clearAll() {
        job?.cancel()
        _items.value = emptyList()
        _result.value = null
        _errorMessage.value = null
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun dismissResult() {
        _result.value = null
    }

    private fun edit(transform: (List<CollageItem>) -> List<CollageItem>) {
        _items.update(transform)
        _result.value = null
    }

    companion object {
        const val MAX_PHOTOS = 24
    }
}
