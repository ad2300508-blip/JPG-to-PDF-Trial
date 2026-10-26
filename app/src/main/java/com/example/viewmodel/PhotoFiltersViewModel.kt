package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryKind
import com.example.data.HistoryRepository
import com.example.service.PhotoFilterService
import com.example.util.PhotoFilter
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

data class FilterPhoto(
    val uri: Uri,
    val name: String,
    val id: String = UUID.randomUUID().toString()
)

class PhotoFiltersViewModel(application: Application) : AndroidViewModel(application) {

    private val service = PhotoFilterService(application.applicationContext)

    private val _photos = MutableStateFlow<List<FilterPhoto>>(emptyList())
    val photos: StateFlow<List<FilterPhoto>> = _photos.asStateFlow()

    private val _filter = MutableStateFlow(PhotoFilter.VIVID)
    val filter: StateFlow<PhotoFilter> = _filter.asStateFlow()

    private val _intensity = MutableStateFlow(1f)
    val intensity: StateFlow<Float> = _intensity.asStateFlow()

    /** Photo shown in the big before/after preview. */
    private val _previewId = MutableStateFlow<String?>(null)
    val previewId: StateFlow<String?> = _previewId.asStateFlow()

    private val _progress = MutableStateFlow<Float?>(null)
    val progress: StateFlow<Float?> = _progress.asStateFlow()

    /** Number of photos saved by the last run, null when nothing new was saved. */
    private val _savedCount = MutableStateFlow<Int?>(null)
    val savedCount: StateFlow<Int?> = _savedCount.asStateFlow()

    private val _lastSaved = MutableStateFlow<Uri?>(null)
    val lastSaved: StateFlow<Uri?> = _lastSaved.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var job: Job? = null

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val app = getApplication<Application>()
        viewModelScope.launch {
            val added = withContext(Dispatchers.IO) { uris.map { FilterPhoto(it, app.queryUriInfo(it, "Foto").name) } }
            _photos.update { it + added }
            if (_previewId.value == null) _previewId.value = added.firstOrNull()?.id
            _savedCount.value = null
        }
    }

    fun remove(id: String) {
        _photos.update { list -> list.filterNot { it.id == id } }
        if (_previewId.value == id) _previewId.value = _photos.value.firstOrNull()?.id
    }

    fun showPreview(id: String) {
        _previewId.value = id
    }

    fun selectFilter(filter: PhotoFilter) {
        _filter.value = filter
        _savedCount.value = null
    }

    fun setIntensity(value: Float) {
        _intensity.value = value.coerceIn(0f, 1f)
        _savedCount.value = null
    }

    fun applyAll() {
        val list = _photos.value
        if (list.isEmpty() || _progress.value != null) return
        val filter = _filter.value
        val intensity = _intensity.value
        _errorMessage.value = null
        _progress.value = 0f
        job = viewModelScope.launch {
            var saved = 0
            try {
                list.forEachIndexed { index, photo ->
                    service.apply(photo.uri, photo.name, filter, intensity)
                        .onSuccess { (uri, size) ->
                            saved++
                            _lastSaved.value = uri
                            HistoryRepository.record(getApplication<Application>(), uri, "Filtri foto", HistoryKind.IMAGE, photo.name, size)
                        }
                        .onFailure { _errorMessage.value = it.userMessage("Non sono riuscito a salvare ${photo.name}") }
                    _progress.value = (index + 1f) / list.size
                }
            } finally {
                _progress.value = null
                _savedCount.value = saved.takeIf { it > 0 }
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }

    fun clearAll() {
        job?.cancel()
        _photos.value = emptyList()
        _previewId.value = null
        _savedCount.value = null
        _errorMessage.value = null
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun dismissSaved() {
        _savedCount.value = null
    }
}
