package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryKind
import com.example.data.HistoryRepository
import com.example.service.PdfCompressionLevel
import com.example.service.PdfImageFormat
import com.example.service.PdfPageEdit
import com.example.service.PdfToolsService
import com.example.util.FileNames
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

/** The PDF currently loaded in one of the single-document PDF tools. */
data class LoadedPdf(val uri: Uri, val name: String, val sizeBytes: Long, val pageCount: Int)

/** Shared plumbing: picking the source PDF, progress, errors and the last output. */
abstract class SinglePdfViewModel(application: Application) : AndroidViewModel(application) {

    protected val tools = PdfToolsService(application.applicationContext)

    private val _source = MutableStateFlow<LoadedPdf?>(null)
    val source: StateFlow<LoadedPdf?> = _source.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    protected val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    protected val _progress = MutableStateFlow(Pair(0, 0))
    val progress: StateFlow<Pair<Int, Int>> = _progress.asStateFlow()

    protected val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    protected val _resultUri = MutableStateFlow<Uri?>(null)
    val resultUri: StateFlow<Uri?> = _resultUri.asStateFlow()

    protected var job: Job? = null

    fun loadPdf(uri: Uri) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _resultUri.value = null
            val info = withContext(Dispatchers.IO) { app.queryUriInfo(uri, "Documento.pdf") }
            tools.readInfo(uri)
                .onSuccess { pdf ->
                    when {
                        pdf.isEncrypted -> _errorMessage.value = "Questo PDF è protetto da password: aprilo e salvalo senza password, poi riprova"
                        pdf.pageCount == 0 -> _errorMessage.value = "Il PDF non contiene pagine"
                        else -> {
                            _source.value = LoadedPdf(uri, info.name, info.sizeBytes, pdf.pageCount)
                            onLoaded(pdf.pageCount)
                        }
                    }
                }
                .onFailure { _errorMessage.value = it.userMessage("Impossibile aprire il PDF") }
            _isLoading.value = false
        }
    }

    protected open fun onLoaded(pageCount: Int) {}

    open fun clear() {
        job?.cancel()
        _isWorking.value = false
        _source.value = null
        _resultUri.value = null
        _errorMessage.value = null
    }

    fun cancel() {
        job?.cancel()
        _isWorking.value = false
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun dismissResult() {
        _resultUri.value = null
    }

    protected fun runJob(block: suspend () -> Unit) {
        if (_isWorking.value) return
        _isWorking.value = true
        _errorMessage.value = null
        _resultUri.value = null
        _progress.value = Pair(0, 0)
        job = viewModelScope.launch {
            try {
                block()
            } finally {
                _isWorking.value = false
            }
        }
    }
}

class PdfCompressViewModel(application: Application) : SinglePdfViewModel(application) {

    private val _level = MutableStateFlow(PdfCompressionLevel.MEDIUM)
    val level: StateFlow<PdfCompressionLevel> = _level.asStateFlow()

    private val _grayscale = MutableStateFlow(false)
    val grayscale: StateFlow<Boolean> = _grayscale.asStateFlow()

    private val _removeMetadata = MutableStateFlow(true)
    val removeMetadata: StateFlow<Boolean> = _removeMetadata.asStateFlow()

    private val _result = MutableStateFlow<PdfToolsService.CompressionResult?>(null)
    val result: StateFlow<PdfToolsService.CompressionResult?> = _result.asStateFlow()

    fun setLevel(level: PdfCompressionLevel) { _level.value = level }
    fun setGrayscale(enabled: Boolean) { _grayscale.value = enabled }
    fun setRemoveMetadata(enabled: Boolean) { _removeMetadata.value = enabled }

    override fun clear() {
        super.clear()
        _result.value = null
    }

    fun compress() {
        val pdf = source.value ?: return
        runJob {
            tools.compress(pdf.uri, pdf.name, pdf.sizeBytes, _level.value, _grayscale.value, _removeMetadata.value) { current, total ->
                _progress.value = Pair(current, total)
            }.onSuccess { res ->
                _result.value = res
                _resultUri.value = res.uri
                HistoryRepository.record(getApplication<Application>(), res.uri, "Comprimi PDF", HistoryKind.PDF, pdf.name, res.outputBytes)
                if (res.imagesRecompressed == 0) {
                    _errorMessage.value = "Il PDF contiene solo testo o immagini già ottimizzate: il peso è cambiato poco"
                }
            }.onFailure { _errorMessage.value = it.userMessage("Compressione non riuscita") }
        }
    }
}

/** Page state in "Organizza PDF". */
data class PageSlot(val sourceIndex: Int, val rotation: Int = 0, val selected: Boolean = false) {
    val key: String get() = "page_$sourceIndex"
}

class PdfPagesViewModel(application: Application) : SinglePdfViewModel(application) {

    private val _pages = MutableStateFlow<List<PageSlot>>(emptyList())
    val pages: StateFlow<List<PageSlot>> = _pages.asStateFlow()

    private val _imageFormat = MutableStateFlow(PdfImageFormat.JPEG)
    val imageFormat: StateFlow<PdfImageFormat> = _imageFormat.asStateFlow()

    private val _dpi = MutableStateFlow(150)
    val dpi: StateFlow<Int> = _dpi.asStateFlow()

    private val _asZip = MutableStateFlow(false)
    val asZip: StateFlow<Boolean> = _asZip.asStateFlow()

    private val _resultCount = MutableStateFlow(0)
    val resultCount: StateFlow<Int> = _resultCount.asStateFlow()

    private val _resultBytes = MutableStateFlow(0L)
    val resultBytes: StateFlow<Long> = _resultBytes.asStateFlow()

    /** Thumbnails rendered on demand, ~24 MB max so long PDFs never exhaust memory. */
    private val thumbnails = object : LruCache<Int, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: Int, value: Bitmap): Int = value.byteCount
    }

    override fun onLoaded(pageCount: Int) {
        thumbnails.evictAll()
        _pages.value = (0 until pageCount).map { PageSlot(it) }
    }

    override fun clear() {
        super.clear()
        thumbnails.evictAll()
        _pages.value = emptyList()
    }

    suspend fun thumbnail(pageIndex: Int): Bitmap? {
        thumbnails.get(pageIndex)?.let { return it }
        val uri = source.value?.uri ?: return null
        val bitmap = tools.renderPage(uri, pageIndex, THUMB_WIDTH) ?: return null
        thumbnails.put(pageIndex, bitmap)
        return bitmap
    }

    fun toggleSelected(key: String) {
        _pages.update { list -> list.map { if (it.key == key) it.copy(selected = !it.selected) else it } }
    }

    fun selectAll(selected: Boolean) {
        _pages.update { list -> list.map { it.copy(selected = selected) } }
    }

    /** Selects odd or even pages (by original page number). */
    fun selectParity(odd: Boolean) {
        _pages.update { list -> list.map { it.copy(selected = (it.sourceIndex % 2 == 0) == odd) } }
    }

    fun rotateSelected() {
        val anySelected = _pages.value.any { it.selected }
        _pages.update { list -> list.map { if (!anySelected || it.selected) it.copy(rotation = (it.rotation + 90) % 360) else it } }
    }

    fun rotate(key: String) {
        _pages.update { list -> list.map { if (it.key == key) it.copy(rotation = (it.rotation + 90) % 360) else it } }
    }

    fun deleteSelected() {
        _pages.update { list -> list.filterNot { it.selected } }
    }

    fun move(from: Int, to: Int) {
        val list = _pages.value
        if (from in list.indices && to in list.indices && from != to) {
            _pages.value = list.toMutableList().apply { add(to, removeAt(from)) }
        }
    }

    fun reverse() {
        _pages.update { it.reversed() }
    }

    fun resetPages() {
        val count = source.value?.pageCount ?: return
        _pages.value = (0 until count).map { PageSlot(it) }
    }

    fun setImageFormat(format: PdfImageFormat) { _imageFormat.value = format }
    fun setDpi(dpi: Int) { _dpi.value = dpi }
    fun setAsZip(enabled: Boolean) { _asZip.value = enabled }

    /** Saves the current order/rotation; with [onlySelected] only the selected pages are written. */
    fun exportPdf(onlySelected: Boolean) {
        val pdf = source.value ?: return
        val slots = _pages.value.filter { !onlySelected || it.selected }
        if (slots.isEmpty()) {
            _errorMessage.value = "Nessuna pagina da salvare"
            return
        }
        val suffix = if (onlySelected) "estratto" else "organizzato"
        runJob {
            tools.exportPages(pdf.uri, "${FileNames.baseName(pdf.name)}_$suffix", slots.map { PdfPageEdit(it.sourceIndex, it.rotation) }) { current, total ->
                _progress.value = Pair(current, total)
            }.onSuccess { uri ->
                _resultCount.value = slots.size
                _resultUri.value = uri
                HistoryRepository.record(getApplication<Application>(), uri, if (onlySelected) "Estrai pagine" else "Organizza PDF", HistoryKind.PDF, pdf.name)
            }.onFailure { _errorMessage.value = it.userMessage("Salvataggio non riuscito") }
        }
    }

    /** "PDF in immagini": selected pages, or all of them when nothing is selected. */
    fun exportImages() {
        val pdf = source.value ?: return
        val selected = _pages.value.filter { it.selected }
        val indices = (selected.ifEmpty { _pages.value }).map { it.sourceIndex }
        runJob {
            tools.exportImages(pdf.uri, pdf.name, indices, _dpi.value, _imageFormat.value, _asZip.value) { current, total ->
                _progress.value = Pair(current, total)
            }.onSuccess { res ->
                val app = getApplication<Application>()
                _resultCount.value = indices.size
                _resultBytes.value = res.totalBytes
                if (res.zipUri != null) {
                    HistoryRepository.record(app, res.zipUri, "PDF in immagini", HistoryKind.ZIP, "${pdf.name}.zip", res.totalBytes)
                    _resultUri.value = res.zipUri
                } else {
                    res.uris.forEach { HistoryRepository.record(app, it, "PDF in immagini", HistoryKind.IMAGE) }
                    _resultUri.value = res.uris.firstOrNull()
                }
            }.onFailure { _errorMessage.value = it.userMessage("Esportazione non riuscita") }
        }
    }

    private companion object {
        const val THUMB_WIDTH = 220
    }
}
