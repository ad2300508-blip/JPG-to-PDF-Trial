package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryKind
import com.example.data.HistoryRepository
import com.example.model.ArchiveConfig
import com.example.model.ArchiveItem
import com.example.service.ArchiveService
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

enum class ArchiveMode { CREATE, EXTRACT }

class ArchiveViewModel(application: Application) : AndroidViewModel(application) {

    private val archiveService = ArchiveService(application.applicationContext)

    private val _mode = MutableStateFlow(ArchiveMode.CREATE)
    val mode: StateFlow<ArchiveMode> = _mode.asStateFlow()

    private val _items = MutableStateFlow<List<ArchiveItem>>(emptyList())
    val items: StateFlow<List<ArchiveItem>> = _items.asStateFlow()

    private val _config = MutableStateFlow(ArchiveConfig())
    val config: StateFlow<ArchiveConfig> = _config.asStateFlow()

    private val _isArchiving = MutableStateFlow(false)
    val isArchiving: StateFlow<Boolean> = _isArchiving.asStateFlow()

    private val _progress = MutableStateFlow(Pair(0, 0))
    val progress: StateFlow<Pair<Int, Int>> = _progress.asStateFlow()

    private val _progressLabel = MutableStateFlow("")
    val progressLabel: StateFlow<String> = _progressLabel.asStateFlow()

    private val _lastGeneratedUri = MutableStateFlow<Uri?>(null)
    val lastGeneratedUri: StateFlow<Uri?> = _lastGeneratedUri.asStateFlow()

    private val _lastGeneratedSize = MutableStateFlow(0L)
    val lastGeneratedSize: StateFlow<Long> = _lastGeneratedSize.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** ZIP chosen for extraction and its entries (name, size). */
    private val _zipToExtract = MutableStateFlow<ArchiveItem?>(null)
    val zipToExtract: StateFlow<ArchiveItem?> = _zipToExtract.asStateFlow()

    private val _zipEntries = MutableStateFlow<List<Pair<String, Long>>>(emptyList())
    val zipEntries: StateFlow<List<Pair<String, Long>>> = _zipEntries.asStateFlow()

    private val _extracted = MutableStateFlow<List<ArchiveService.ExtractedFile>>(emptyList())
    val extracted: StateFlow<List<ArchiveService.ExtractedFile>> = _extracted.asStateFlow()

    private var job: Job? = null

    fun setMode(mode: ArchiveMode) {
        _mode.value = mode
        _errorMessage.value = null
    }

    fun addFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val app = getApplication<Application>()
        viewModelScope.launch {
            val newItems = withContext(Dispatchers.IO) {
                uris.map { uri ->
                    val info = app.queryUriInfo(uri, "File")
                    ArchiveItem(uri = uri, name = info.name, sizeBytes = info.sizeBytes, mimeType = info.mimeType)
                }
            }
            // Picking the same file twice is almost always a mistake.
            _items.update { current -> current + newItems.filter { n -> current.none { it.uri == n.uri } } }
        }
    }

    fun removeItem(id: String) {
        _items.update { list -> list.filterNot { it.id == id } }
    }

    fun clearAll() {
        job?.cancel()
        _isArchiving.value = false
        _items.value = emptyList()
        _lastGeneratedUri.value = null
        _errorMessage.value = null
    }

    fun updateConfig(newConfig: ArchiveConfig) {
        _config.value = newConfig
    }

    fun createArchive() {
        val items = _items.value
        if (items.isEmpty() || _isArchiving.value) return
        val config = _config.value
        _isArchiving.value = true
        _errorMessage.value = null
        _lastGeneratedUri.value = null
        _progress.value = Pair(0, items.size)
        job = viewModelScope.launch {
            try {
                archiveService.createZip(items, config) { current, total ->
                    _progress.value = Pair(current, total)
                    _progressLabel.value = items.getOrNull(current - 1)?.name.orEmpty()
                }.onSuccess { (uri, size) ->
                    _lastGeneratedUri.value = uri
                    _lastGeneratedSize.value = size
                    HistoryRepository.record(getApplication<Application>(), uri, "Archivio ZIP", HistoryKind.ZIP, "${config.zipFileName}.zip", size)
                }.onFailure { err ->
                    _errorMessage.value = err.userMessage("Creazione dello ZIP non riuscita")
                }
            } finally {
                _isArchiving.value = false
            }
        }
    }

    fun selectZipToExtract(uri: Uri) {
        val app = getApplication<Application>()
        _extracted.value = emptyList()
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) { app.queryUriInfo(uri, "Archivio.zip") }
            _zipToExtract.value = ArchiveItem(uri = uri, name = info.name, sizeBytes = info.sizeBytes, mimeType = info.mimeType)
            _zipEntries.value = emptyList()
            archiveService.listZip(uri)
                .onSuccess { _zipEntries.value = it }
                .onFailure { err ->
                    _zipToExtract.value = null
                    _errorMessage.value = err.userMessage("Archivio non valido")
                }
        }
    }

    fun clearZipToExtract() {
        _zipToExtract.value = null
        _zipEntries.value = emptyList()
        _extracted.value = emptyList()
    }

    fun extractZip() {
        val zip = _zipToExtract.value ?: return
        if (_isArchiving.value) return
        val total = _zipEntries.value.size
        _isArchiving.value = true
        _errorMessage.value = null
        _progress.value = Pair(0, total)
        job = viewModelScope.launch {
            try {
                archiveService.extractZip(zip.uri) { index, name ->
                    _progress.value = Pair(index, total)
                    _progressLabel.value = name
                }.onSuccess { files ->
                    _extracted.value = files
                    val app = getApplication<Application>()
                    files.forEach { file ->
                        HistoryRepository.record(app, file.uri, "Estrai ZIP", com.example.data.kindForFileName(file.name), file.name, file.sizeBytes)
                    }
                }.onFailure { err ->
                    _errorMessage.value = err.userMessage("Estrazione non riuscita")
                }
            } finally {
                _isArchiving.value = false
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }

    fun dismissSuccess() {
        _lastGeneratedUri.value = null
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}
