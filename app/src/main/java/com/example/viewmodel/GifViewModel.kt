package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryKind
import com.example.data.HistoryRepository
import com.example.service.GifService
import com.example.service.GifSettings
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

data class GifFrameItem(
    val uri: Uri,
    val name: String,
    /** Width / height as shown (EXIF rotation applied). */
    val aspect: Float,
    val id: String = UUID.randomUUID().toString()
)

data class GifResult(val uri: Uri, val sizeBytes: Long)

class GifViewModel(application: Application) : AndroidViewModel(application) {

    private val service = GifService(application.applicationContext)

    private val _frames = MutableStateFlow<List<GifFrameItem>>(emptyList())
    val frames: StateFlow<List<GifFrameItem>> = _frames.asStateFlow()

    private val _settings = MutableStateFlow(GifSettings())
    val settings: StateFlow<GifSettings> = _settings.asStateFlow()

    private val _progress = MutableStateFlow<Float?>(null)
    val progress: StateFlow<Float?> = _progress.asStateFlow()

    private val _result = MutableStateFlow<GifResult?>(null)
    val result: StateFlow<GifResult?> = _result.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var job: Job? = null

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val app = getApplication<Application>()
        viewModelScope.launch {
            val room = MAX_FRAMES - _frames.value.size
            if (room <= 0) {
                _errorMessage.value = "Una GIF può avere al massimo $MAX_FRAMES foto"
                return@launch
            }
            val added = withContext(Dispatchers.IO) {
                uris.take(room).mapNotNull { uri ->
                    val bounds = BitmapLoader.readBounds(app, uri) ?: return@mapNotNull null
                    val rotated = BitmapLoader.readExif(app, uri).first % 180 != 0
                    val w = if (rotated) bounds.height else bounds.width
                    val h = if (rotated) bounds.width else bounds.height
                    GifFrameItem(uri, app.queryUriInfo(uri, "Foto").name, w.toFloat() / h.coerceAtLeast(1))
                }
            }
            if (added.size < uris.size) {
                _errorMessage.value = if (uris.size > room) "Ho aggiunto solo le prime $room foto (massimo $MAX_FRAMES)" else "Alcuni file non sono immagini leggibili"
            }
            _frames.update { it + added }
            _result.value = null
        }
    }

    fun remove(id: String) = edit { list -> list.filterNot { it.id == id } }

    fun reverse() = edit { it.reversed() }

    /** Moves a frame one step earlier (-1) or later (+1). */
    fun nudge(id: String, step: Int) = edit { list ->
        val i = list.indexOfFirst { it.id == id }
        val j = i + step
        if (i < 0 || j !in list.indices) list else list.toMutableList().apply { add(j, removeAt(i)) }
    }

    fun updateSettings(transform: (GifSettings) -> GifSettings) {
        _settings.update(transform)
        _result.value = null
    }

    fun save() {
        val list = _frames.value
        if (list.size < 2 || _progress.value != null) return
        val settings = _settings.value
        _errorMessage.value = null
        _progress.value = 0f
        job = viewModelScope.launch {
            try {
                service.render(list.map { it.uri }, list.first().aspect, settings) { _progress.value = it }
                    .onSuccess { (uri, size) ->
                        HistoryRepository.record(getApplication<Application>(), uri, "GIF animata", HistoryKind.IMAGE, "Animazione.gif", size)
                        _result.value = GifResult(uri, size)
                    }
                    .onFailure { _errorMessage.value = it.userMessage("Non sono riuscito a creare la GIF") }
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
        _frames.value = emptyList()
        _result.value = null
        _errorMessage.value = null
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun dismissResult() {
        _result.value = null
    }

    private fun edit(transform: (List<GifFrameItem>) -> List<GifFrameItem>) {
        _frames.update(transform)
        _result.value = null
    }

    companion object {
        const val MAX_FRAMES = 40
    }
}
