package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppPreferences
import com.example.data.HistoryKind
import com.example.data.HistoryRepository
import com.example.model.CropRect
import com.example.model.PdfDocumentItem
import com.example.model.PdfExportConfig
import com.example.service.PdfConverterService
import com.example.util.queryUriInfo
import com.example.util.userMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.util.MediaGallery
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PdfStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val pdfService = PdfConverterService(application.applicationContext)

    private val _items = MutableStateFlow<List<PdfDocumentItem>>(emptyList())
    val items: StateFlow<List<PdfDocumentItem>> = _items.asStateFlow()

    private val _config = MutableStateFlow(
        PdfExportConfig(
            pageSize = AppPreferences.defaultPageSize.value,
            quality = AppPreferences.defaultQuality.value,
            autoContrastClean = AppPreferences.autoClean.value,
            stripMetadata = AppPreferences.stripMetadata.value
        )
    )
    val config: StateFlow<PdfExportConfig> = _config.asStateFlow()

    init {
        // Changing the defaults in Settings applies right away to the PDF being prepared.
        viewModelScope.launch {
            combine(
                AppPreferences.defaultPageSize,
                AppPreferences.defaultQuality,
                AppPreferences.autoClean,
                AppPreferences.stripMetadata
            ) { size, quality, clean, strip -> PdfExportConfig(pageSize = size, quality = quality, autoContrastClean = clean, stripMetadata = strip) }
                .collect { d ->
                    _config.update { it.copy(pageSize = d.pageSize, quality = d.quality, autoContrastClean = d.autoContrastClean, stripMetadata = d.stripMetadata) }
                }
        }
    }

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationProgress = MutableStateFlow(Pair(0, 0))
    val generationProgress: StateFlow<Pair<Int, Int>> = _generationProgress.asStateFlow()

    private val _lastGeneratedUri = MutableStateFlow<Uri?>(null)
    val lastGeneratedUri: StateFlow<Uri?> = _lastGeneratedUri.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Last deleted page, so the UI can offer "Annulla". */
    private val _lastRemoved = MutableStateFlow<Pair<Int, PdfDocumentItem>?>(null)
    val lastRemoved: StateFlow<Pair<Int, PdfDocumentItem>?> = _lastRemoved.asStateFlow()

    private var generationJob: Job? = null

    private val addMutex = Mutex()

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val app = getApplication<Application>()
        viewModelScope.launch {
            // Batches are appended one at a time, in the order they were picked, so a quick second
            // pick can never land before the first one; inside a batch the picker's order is kept.
            addMutex.withLock {
                val newItems = withContext(Dispatchers.IO) {
                    uris.distinct().map { uri ->
                        val info = app.queryUriInfo(uri, "Immagine")
                        PdfDocumentItem(uri = uri, name = info.name, sizeBytes = info.sizeBytes)
                    }
                }
                _items.update { it + newItems }
                _lastGeneratedUri.value = null
            }
        }
    }

    fun removeItem(id: String) {
        val current = _items.value
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) return
        _lastRemoved.value = index to current[index]
        _items.value = current.filterNot { it.id == id }
    }

    fun undoRemove() {
        val (index, item) = _lastRemoved.value ?: return
        _items.update { list -> list.toMutableList().apply { add(index.coerceIn(0, size), item) } }
        _lastRemoved.value = null
    }

    fun dismissUndo() {
        _lastRemoved.value = null
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        val list = _items.value
        if (fromIndex in list.indices && toIndex in list.indices && fromIndex != toIndex) {
            _items.value = list.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        }
    }

    fun reverseOrder() {
        _items.update { it.reversed() }
    }

    /** Oldest capture first (camera date, then EXIF); photos without a date keep their place at the end. */
    fun sortByDateTaken() {
        val app = getApplication<Application>()
        viewModelScope.launch {
            val snapshot = _items.value
            val dates = withContext(Dispatchers.IO) { snapshot.associate { item -> item.id to item.uri?.let { MediaGallery.dateTaken(app, it) } } }
            _items.update { list -> list.sortedWith(compareBy(nullsLast<Long>()) { dates[it.id] }) }
        }
    }

    fun sortByName() {
        _items.update { list -> list.sortedWith(compareBy(NaturalOrder) { it.name }) }
    }

    fun duplicateItem(id: String) {
        _items.update { list ->
            val index = list.indexOfFirst { it.id == id }
            if (index < 0) list else list.toMutableList().apply {
                add(index + 1, list[index].copy(id = java.util.UUID.randomUUID().toString()))
            }
        }
    }

    fun clearAll() {
        generationJob?.cancel()
        _items.value = emptyList()
        _lastGeneratedUri.value = null
        _errorMessage.value = null
        _lastRemoved.value = null
    }

    fun toggleSpreadSplit(id: String) {
        val current = _items.value
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) return
        val item = current[index]
        _items.value = if (item.isSplitSpread) {
            // Merge the two halves back into a single page.
            val partnerIndex = when {
                item.splitPart == 1 && index + 1 < current.size && current[index + 1].isSplitSpread && current[index + 1].uri == item.uri -> index + 1
                item.splitPart == 2 && index > 0 && current[index - 1].isSplitSpread && current[index - 1].uri == item.uri -> index - 1
                else -> -1
            }
            current.toMutableList().apply {
                set(index, item.copy(isSplitSpread = false, splitPart = 1))
                if (partnerIndex >= 0) removeAt(partnerIndex)
            }
        } else {
            val left = item.copy(isSplitSpread = true, splitPart = 1)
            val right = item.copy(id = java.util.UUID.randomUUID().toString(), isSplitSpread = true, splitPart = 2)
            current.toMutableList().apply {
                set(index, left)
                add(index + 1, right)
            }
        }
    }

    fun toggleBinarization(id: String) {
        _items.update { list -> list.map { if (it.id == id) it.copy(isBinarized = !it.isBinarized) else it } }
    }

    fun rotateItem(id: String) {
        _items.update { list -> list.map { if (it.id == id) it.copy(rotationDegrees = (it.rotationDegrees + 90) % 360) else it } }
    }

    fun rotateAll() {
        _items.update { list -> list.map { it.copy(rotationDegrees = (it.rotationDegrees + 90) % 360) } }
    }

    fun setCrop(id: String, crop: CropRect?) {
        val normalized = crop?.takeIf { !it.isFullFrame }
        _items.update { list -> list.map { if (it.id == id) it.copy(cropRectNormalized = normalized) else it } }
    }

    fun updateConfig(newConfig: PdfExportConfig) {
        _config.value = newConfig
    }

    fun generatePdf() {
        if (_items.value.isEmpty() || _isGenerating.value) return
        val items = _items.value
        val config = _config.value
        _generationProgress.value = Pair(0, items.size)
        _isGenerating.value = true
        _errorMessage.value = null
        _lastGeneratedUri.value = null
        generationJob = viewModelScope.launch {
            try {
                pdfService.generatePdf(items, config) { current, total ->
                    _generationProgress.value = Pair(current, total)
                }.onSuccess { uri ->
                    _lastGeneratedUri.value = uri
                    HistoryRepository.record(getApplication<Application>(), uri, "Immagini in PDF", HistoryKind.PDF, "${config.title.ifBlank { "Documento" }}.pdf")
                }.onFailure { err ->
                    _errorMessage.value = err.userMessage("Creazione del PDF non riuscita")
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        generationJob = null
        _isGenerating.value = false
        _generationProgress.value = Pair(0, 0)
    }

    /** Leaves the result screen but keeps the pages so the user can tweak and export again. */
    fun backToEditor() {
        _lastGeneratedUri.value = null
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}

/** "foto2.jpg" before "foto10.jpg". */
object NaturalOrder : Comparator<String> {
    private val chunk = Regex("(\\d+|\\D+)")

    override fun compare(a: String, b: String): Int {
        val ca = chunk.findAll(a.lowercase()).map { it.value }.toList()
        val cb = chunk.findAll(b.lowercase()).map { it.value }.toList()
        for (i in 0 until minOf(ca.size, cb.size)) {
            val x = ca[i]
            val y = cb[i]
            val nx = x.toBigIntegerOrNull()
            val ny = y.toBigIntegerOrNull()
            val result = if (nx != null && ny != null) nx.compareTo(ny) else x.compareTo(y)
            if (result != 0) return result
        }
        return ca.size - cb.size
    }
}
