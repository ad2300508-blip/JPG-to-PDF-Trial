package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryKind
import com.example.data.HistoryRepository
import com.example.service.PdfDetails
import com.example.service.PdfInsightService
import com.example.service.PdfMetadata
import com.example.util.FileNames
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.util.TextPdfWriter
import com.example.util.resultOf
import com.example.util.userMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** "Leggi PDF": pages rendered on demand at screen width, remembered in a bounded cache. */
class PdfReaderViewModel(application: Application) : SinglePdfViewModel(application) {

    private val _currentPage = MutableStateFlow(0)
    /** Page on screen, kept so the reader reopens where it was left. */
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _night = MutableStateFlow(false)
    val night: StateFlow<Boolean> = _night.asStateFlow()

    private val _showThumbnails = MutableStateFlow(false)
    val showThumbnails: StateFlow<Boolean> = _showThumbnails.asStateFlow()

    /** ~40 MB of rendered pages: about three full pages plus many thumbnails. */
    private val cache = object : LruCache<String, Bitmap>(40 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    override fun onLoaded(pageCount: Int) {
        cache.evictAll()
        _currentPage.value = 0
    }

    override fun clear() {
        super.clear()
        cache.evictAll()
        _currentPage.value = 0
    }

    fun setCurrentPage(index: Int) {
        _currentPage.value = index
    }

    fun toggleNight() {
        _night.value = !_night.value
    }

    fun toggleThumbnails() {
        _showThumbnails.value = !_showThumbnails.value
    }

    /** Page [index] rendered [widthPx] wide (widths are bucketed so zoom changes reuse renders). */
    suspend fun page(index: Int, widthPx: Int): Bitmap? {
        val width = (((widthPx + 127) / 128) * 128).coerceIn(128, MAX_PAGE_WIDTH)
        val key = "$index@$width"
        cache.get(key)?.let { return it }
        val uri = source.value?.uri ?: return null
        val bitmap = tools.renderPage(uri, index, width) ?: return null
        cache.put(key, bitmap)
        return bitmap
    }

    private companion object {
        const val MAX_PAGE_WIDTH = 1664
    }
}

/** "Info PDF": details + text statistics, editable metadata saved into a copy. */
class PdfInfoViewModel(application: Application) : SinglePdfViewModel(application) {

    private val insight = PdfInsightService(application.applicationContext)

    private val _details = MutableStateFlow<PdfDetails?>(null)
    val details: StateFlow<PdfDetails?> = _details.asStateFlow()

    private val _analyzing = MutableStateFlow(false)
    val analyzing: StateFlow<Boolean> = _analyzing.asStateFlow()

    private val _metadata = MutableStateFlow(PdfMetadata())
    val metadata: StateFlow<PdfMetadata> = _metadata.asStateFlow()

    /** True when the last save removed every property (changes the result screen wording). */
    private val _strippedAll = MutableStateFlow(false)
    val strippedAll: StateFlow<Boolean> = _strippedAll.asStateFlow()

    private var analysis: Job? = null

    override fun onLoaded(pageCount: Int) {
        val pdf = source.value ?: return
        _details.value = null
        analysis?.cancel()
        analysis = viewModelScope.launch {
            _analyzing.value = true
            insight.details(pdf.uri)
                .onSuccess {
                    _details.value = it
                    _metadata.value = it.metadata
                }
                .onFailure { _errorMessage.value = it.userMessage("Non riesco a leggere le informazioni del PDF") }
            _analyzing.value = false
        }
    }

    override fun clear() {
        analysis?.cancel()
        _analyzing.value = false
        super.clear()
        _details.value = null
        _metadata.value = PdfMetadata()
    }

    fun updateMetadata(transform: (PdfMetadata) -> PdfMetadata) {
        _metadata.value = transform(_metadata.value)
    }

    /** True when the fields differ from what the document already contains. */
    fun hasChanges(): Boolean = _details.value?.metadata?.let { it != _metadata.value } ?: false

    fun save(removeAll: Boolean) {
        val pdf = source.value ?: return
        val meta = if (removeAll) null else _metadata.value
        runJob {
            _progress.value = Pair(0, 1)
            insight.saveMetadata(pdf.uri, pdf.name, meta)
                .onSuccess { uri ->
                    _progress.value = Pair(1, 1)
                    _strippedAll.value = removeAll
                    _resultUri.value = uri
                    HistoryRepository.record(getApplication<Application>(), uri, "Info PDF", HistoryKind.PDF, pdf.name, 0L)
                }
                .onFailure { _errorMessage.value = it.userMessage("Non sono riuscito a salvare il PDF") }
        }
    }
}

/** "Testo in PDF": a title and a text (light Markdown: # titles, - lists) laid out on A4 pages. */
class TextToPdfViewModel(application: Application) : AndroidViewModel(application) {

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _resultUri = MutableStateFlow<Uri?>(null)
    val resultUri: StateFlow<Uri?> = _resultUri.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun setTitle(value: String) {
        _title.value = value
    }

    fun setText(value: String) {
        _text.value = value
    }

    fun appendText(value: String) {
        val current = _text.value
        _text.value = if (current.isBlank()) value else current.trimEnd() + "\n\n" + value
    }

    fun clearText() {
        _text.value = ""
    }

    fun save() {
        val body = _text.value
        if (body.isBlank() || _isSaving.value) return
        val title = _title.value.trim().ifBlank { "Appunti" }
        val app = getApplication<Application>()
        _isSaving.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            resultOf {
                withContext(Dispatchers.IO) {
                    OutputStorage.save(app, FileNames.stamped(title, "Appunti", "pdf"), "application/pdf", OutputFolder.PDFS) { out ->
                        TextPdfWriter.write(body, title, out)
                    }
                }
            }.onSuccess { uri ->
                _resultUri.value = uri
                HistoryRepository.record(app, uri, "Testo in PDF", HistoryKind.PDF, "$title.pdf")
            }.onFailure {
                _errorMessage.value = it.userMessage("Non sono riuscito a creare il PDF")
            }
            _isSaving.value = false
        }
    }

    fun dismissResult() {
        _resultUri.value = null
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun startOver() {
        _resultUri.value = null
        _title.value = ""
        _text.value = ""
    }
}
