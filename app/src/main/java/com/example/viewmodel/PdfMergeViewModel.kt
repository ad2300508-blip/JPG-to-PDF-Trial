package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryKind
import com.example.data.HistoryRepository
import com.example.model.PdfDocumentItem
import com.example.model.PdfMergeSettings
import com.example.service.PdfMergeService
import com.example.util.PageRange
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

class PdfMergeViewModel(application: Application) : AndroidViewModel(application) {

    private val mergeService = PdfMergeService(application.applicationContext)

    private val _documents = MutableStateFlow<List<PdfDocumentItem>>(emptyList())
    val documents: StateFlow<List<PdfDocumentItem>> = _documents.asStateFlow()

    private val _settings: MutableStateFlow<PdfMergeSettings> = MutableStateFlow(PdfMergeSettings())
    val settings: StateFlow<PdfMergeSettings> = _settings.asStateFlow()

    private val _isMerging = MutableStateFlow(false)
    val isMerging: StateFlow<Boolean> = _isMerging.asStateFlow()

    private val _progress = MutableStateFlow(Pair(0, 0))
    val progress: StateFlow<Pair<Int, Int>> = _progress.asStateFlow()

    private val _lastMergedUri = MutableStateFlow<Uri?>(null)
    val lastMergedUri: StateFlow<Uri?> = _lastMergedUri.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var mergeJob: Job? = null

    fun addPdfDocuments(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val app = getApplication<Application>()
        viewModelScope.launch {
            _isLoading.value = true
            val newItems = withContext(Dispatchers.IO) { uris.map { uri -> inspect(app, uri) } }
            _documents.update { it + newItems }
            _lastMergedUri.value = null
            _isLoading.value = false
        }
    }

    private fun inspect(app: Application, uri: Uri): PdfDocumentItem {
        val info = app.queryUriInfo(uri, "Documento.pdf")
        var pages = 0
        var thumb: Bitmap? = null
        var encrypted = false
        var corrupt = false
        var error: String? = null
        try {
            app.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    pages = renderer.pageCount
                    if (pages > 0) {
                        renderer.openPage(0).use { page ->
                            val width = 180
                            val height = (width * page.height.toFloat() / page.width).toInt().coerceIn(120, 320)
                            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            thumb = bmp
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            encrypted = true
            error = "Protetto da password"
        } catch (t: Throwable) {
            corrupt = true
            error = "File PDF danneggiato o non valido"
        }
        return PdfDocumentItem(
            uri = uri,
            fileName = info.name,
            name = info.name,
            fileSizeBytes = info.sizeBytes,
            sizeBytes = info.sizeBytes,
            pageCount = pages,
            thumbnailBitmap = thumb,
            isEncrypted = encrypted,
            isCorrupted = corrupt,
            errorMessage = error
        )
    }

    /** Unlocks a protected PDF: verifies the password with PDFBox and reads its page count. */
    fun unlockDocument(id: String, password: String) {
        val doc = _documents.value.firstOrNull { it.id == id } ?: return
        val uri = doc.uri ?: return
        viewModelScope.launch {
            mergeService.readPageCount(uri, password)
                .onSuccess { count ->
                    _documents.update { list ->
                        list.map { if (it.id == id) it.copy(password = password, pageCount = count, errorMessage = null) else it }
                    }
                }
                .onFailure { err ->
                    _errorMessage.value = err.userMessage("Password errata")
                }
        }
    }

    fun removeDocument(id: String) {
        _documents.update { list -> list.filterNot { it.id == id } }
    }

    fun moveDocumentUp(index: Int) = moveDocument(index, index - 1)

    fun moveDocumentDown(index: Int) = moveDocument(index, index + 1)

    fun moveDocument(from: Int, to: Int) {
        val list = _documents.value
        if (from in list.indices && to in list.indices && from != to) {
            _documents.value = list.toMutableList().apply { add(to, removeAt(from)) }
        }
    }

    fun sortByName() {
        _documents.update { list -> list.sortedWith(compareBy(NaturalOrder) { it.name }) }
    }

    fun clearAll() {
        mergeJob?.cancel()
        _documents.value = emptyList()
        _lastMergedUri.value = null
        _errorMessage.value = null
    }

    fun updateSettings(newSettings: PdfMergeSettings) {
        _settings.value = newSettings
    }

    fun updatePageRange(id: String, range: String) {
        _documents.update { list -> list.map { if (it.id == id) it.copy(pageRange = range) else it } }
    }

    /** Pages that will actually end up in the merged file (page ranges applied). */
    fun selectedPageCount(docs: List<PdfDocumentItem> = _documents.value): Int =
        docs.filter { !it.isCorrupted }.sumOf { doc ->
            if (doc.pageCount <= 0) 0 else PageRange.parse(doc.pageRange, doc.pageCount).size
        }

    fun getTotalPageCount(): Int = _documents.value.sumOf { it.pageCount }

    fun getTotalFileSizeBytes(): Long = _documents.value.sumOf { if (it.fileSizeBytes > 0L) it.fileSizeBytes else it.sizeBytes }

    /** Why merging is not possible right now, or null when it is. */
    fun blockingReason(docs: List<PdfDocumentItem>, settings: PdfMergeSettings): String? = when {
        docs.count { !it.isCorrupted } < 2 -> "Aggiungi almeno due PDF validi"
        docs.any { it.isEncrypted && it.password.isEmpty() } -> "Sblocca i PDF protetti con la loro password"
        docs.any { it.pageCount > 0 && !PageRange.isValid(it.pageRange, it.pageCount) } -> "Correggi gli intervalli di pagine evidenziati"
        !settings.hasValidPassword -> "Scegli una password per proteggere il PDF"
        else -> null
    }

    fun mergeDocuments() {
        val docs = _documents.value
        if (blockingReason(docs, _settings.value) != null || _isMerging.value) return
        val settings = _settings.value
        _isMerging.value = true
        _errorMessage.value = null
        _lastMergedUri.value = null
        _progress.value = Pair(0, docs.size)

        mergeJob = viewModelScope.launch {
            try {
                mergeService.mergePdfs(docs, settings) { current, total ->
                    _progress.value = Pair(current, total)
                }.onSuccess { uri ->
                    _lastMergedUri.value = uri
                    HistoryRepository.record(getApplication<Application>(), uri, "Unisci PDF", HistoryKind.PDF, "${settings.outputFileName}.pdf")
                }.onFailure { err ->
                    _errorMessage.value = err.userMessage("Unione non riuscita")
                }
            } finally {
                _isMerging.value = false
            }
        }
    }

    fun cancelMerge() {
        mergeJob?.cancel()
        _isMerging.value = false
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun dismissSuccess() {
        _lastMergedUri.value = null
    }
}
