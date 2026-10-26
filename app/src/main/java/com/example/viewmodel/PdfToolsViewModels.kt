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
import com.example.service.PdfSecurityInfo
import com.example.service.PdfStampService
import com.example.service.ProtectOptions
import com.example.service.StampConfig
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
                        pdf.isEncrypted && acceptsEncrypted -> {
                            _source.value = LoadedPdf(uri, info.name, info.sizeBytes, 0)
                            onLoaded(0)
                        }
                        pdf.isEncrypted -> _errorMessage.value = "Questo PDF è protetto da password: sbloccalo prima con «Proteggi PDF» › Sblocca"
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

    /** Password-protected PDFs are rejected unless the tool can unlock them. */
    protected open val acceptsEncrypted: Boolean = false

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

class PdfStampViewModel(application: Application) : SinglePdfViewModel(application) {

    private val stamper = PdfStampService(application.applicationContext)

    private val _config = MutableStateFlow(StampConfig(text = "BOZZA"))
    val config: StateFlow<StampConfig> = _config.asStateFlow()

    fun update(transform: (StampConfig) -> StampConfig) {
        _config.update(transform)
    }

    /** First page rendered for the live preview. */
    suspend fun previewPage(widthPx: Int): Bitmap? {
        val uri = source.value?.uri ?: return null
        return tools.renderPage(uri, 0, widthPx)
    }

    fun apply() {
        val pdf = source.value ?: return
        val config = _config.value
        runJob {
            stamper.stamp(pdf.uri, pdf.name, config) { current, total -> _progress.value = Pair(current, total) }
                .onSuccess { uri ->
                    _resultUri.value = uri
                    val label = if (config.hasWatermark) "Filigrana PDF" else "Numera pagine"
                    HistoryRepository.record(getApplication<Application>(), uri, label, HistoryKind.PDF, pdf.name, 0L)
                }
                .onFailure { _errorMessage.value = it.userMessage("Non sono riuscito a modificare il PDF") }
        }
    }
}

enum class SecureMode(val label: String) { PROTECT("Proteggi"), UNLOCK("Sblocca") }

class PdfSecureViewModel(application: Application) : SinglePdfViewModel(application) {

    private val stamper = PdfStampService(application.applicationContext)

    override val acceptsEncrypted: Boolean = true

    private val _security = MutableStateFlow<PdfSecurityInfo?>(null)
    val security: StateFlow<PdfSecurityInfo?> = _security.asStateFlow()

    private val _mode = MutableStateFlow(SecureMode.PROTECT)
    val mode: StateFlow<SecureMode> = _mode.asStateFlow()

    private val _options = MutableStateFlow(ProtectOptions(password = ""))
    val options: StateFlow<ProtectOptions> = _options.asStateFlow()

    private val _confirm = MutableStateFlow("")
    val confirm: StateFlow<String> = _confirm.asStateFlow()

    /** Password of the source document (needed to unlock it or to change its password). */
    private val _currentPassword = MutableStateFlow("")
    val currentPassword: StateFlow<String> = _currentPassword.asStateFlow()

    override fun onLoaded(pageCount: Int) {
        _security.value = null
        _currentPassword.value = ""
        val uri = source.value?.uri ?: return
        viewModelScope.launch {
            stamper.inspect(uri)
                .onSuccess { info ->
                    _security.value = info
                    _mode.value = if (info.isEncrypted) SecureMode.UNLOCK else SecureMode.PROTECT
                }
                .onFailure { _errorMessage.value = it.userMessage("Impossibile leggere il PDF") }
        }
    }

    override fun clear() {
        super.clear()
        _security.value = null
        _currentPassword.value = ""
        _confirm.value = ""
    }

    fun setMode(mode: SecureMode) { _mode.value = mode }
    fun setPassword(value: String) { _options.update { it.copy(password = value) } }
    fun setConfirm(value: String) { _confirm.value = value }
    fun setCurrentPassword(value: String) { _currentPassword.value = value }
    fun setAllowPrinting(value: Boolean) { _options.update { it.copy(allowPrinting = value) } }
    fun setAllowCopying(value: Boolean) { _options.update { it.copy(allowCopying = value) } }
    fun setAllowEditing(value: Boolean) { _options.update { it.copy(allowEditing = value) } }

    /** Why the main button is disabled, or null when the job can start. */
    fun blockingReason(): String? {
        val info = _security.value ?: return "Controllo la protezione del PDF…"
        val needsCurrent = info.needsPassword && _currentPassword.value.isEmpty()
        return when (_mode.value) {
            SecureMode.UNLOCK -> when {
                !info.isEncrypted -> "Il PDF non è protetto"
                needsCurrent -> "Inserisci la password del PDF"
                else -> null
            }
            SecureMode.PROTECT -> when {
                needsCurrent -> "Inserisci prima la password attuale"
                _options.value.password.length < 4 -> "La password deve avere almeno 4 caratteri"
                _options.value.password != _confirm.value -> "Le due password non coincidono"
                else -> null
            }
        }
    }

    fun run() {
        val pdf = source.value ?: return
        if (blockingReason() != null) return
        val mode = _mode.value
        val options = _options.value
        val current = _currentPassword.value.ifEmpty { null }
        runJob {
            _progress.value = Pair(0, 1)
            val result = when (mode) {
                SecureMode.PROTECT -> stamper.protect(pdf.uri, pdf.name, current, options)
                SecureMode.UNLOCK -> stamper.unlock(pdf.uri, pdf.name, current)
            }
            result
                .onSuccess { uri ->
                    _progress.value = Pair(1, 1)
                    _resultUri.value = uri
                    val label = if (mode == SecureMode.PROTECT) "Proteggi PDF" else "Sblocca PDF"
                    HistoryRepository.record(getApplication<Application>(), uri, label, HistoryKind.PDF, pdf.name, 0L)
                }
                .onFailure { _errorMessage.value = it.userMessage("Operazione non riuscita") }
        }
    }
}
