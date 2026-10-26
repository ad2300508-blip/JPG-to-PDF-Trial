package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryKind
import com.example.data.HistoryRepository
import com.example.service.PdfPageToolsService
import com.example.util.NotesSide
import com.example.util.NotesStyle
import com.example.util.PageMath
import com.example.util.queryUriInfo
import com.example.util.userMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Base for the page tools: a page service, cached thumbnails and the shared "save + record" step. */
abstract class PageToolViewModel(application: Application) : SinglePdfViewModel(application) {

    protected val pageTools = PdfPageToolsService(application.applicationContext)

    private val thumbnails = object : LruCache<Int, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: Int, value: Bitmap): Int = value.byteCount
    }

    override fun onLoaded(pageCount: Int) {
        thumbnails.evictAll()
    }

    override fun clear() {
        super.clear()
        thumbnails.evictAll()
    }

    suspend fun thumbnail(pageIndex: Int, widthPx: Int = 220): Bitmap? {
        thumbnails.get(pageIndex)?.let { return it }
        val uri = source.value?.uri ?: return null
        val bitmap = tools.renderPage(uri, pageIndex, widthPx) ?: return null
        thumbnails.put(pageIndex, bitmap)
        return bitmap
    }

    protected fun finish(result: Result<Uri>, toolLabel: String, sourceName: String, failure: String) {
        result
            .onSuccess { uri ->
                _resultUri.value = uri
                HistoryRepository.record(getApplication<Application>(), uri, toolLabel, HistoryKind.PDF, sourceName, 0L)
            }
            .onFailure { _errorMessage.value = it.userMessage(failure) }
    }
}

/** How dark a page may be and still count as blank. */
enum class BlankSensitivity(val label: String, val maxInk: Float) {
    STRICT("Solo bianche", 0.0008f),
    NORMAL("Normale", 0.003f),
    LOOSE("Anche quasi vuote", 0.008f)
}

class PdfBlankPagesViewModel(application: Application) : PageToolViewModel(application) {

    private val _ink = MutableStateFlow<List<Float>?>(null)
    /** Ink share per page, null until the scan finishes. */
    val ink: StateFlow<List<Float>?> = _ink.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(Pair(0, 0))
    val scanProgress: StateFlow<Pair<Int, Int>> = _scanProgress.asStateFlow()

    private val _sensitivity = MutableStateFlow(BlankSensitivity.NORMAL)
    val sensitivity: StateFlow<BlankSensitivity> = _sensitivity.asStateFlow()

    /** Pages the user flipped by hand (kept although blank, or removed although not). */
    private val _overrides = MutableStateFlow<Set<Int>>(emptySet())
    val overrides: StateFlow<Set<Int>> = _overrides.asStateFlow()

    private var scan: Job? = null

    override fun onLoaded(pageCount: Int) {
        super.onLoaded(pageCount)
        _ink.value = null
        _overrides.value = emptySet()
        val pdf = source.value ?: return
        scan?.cancel()
        scan = viewModelScope.launch {
            _scanning.value = true
            pageTools.scanInk(pdf.uri) { current, total -> _scanProgress.value = Pair(current, total) }
                .onSuccess { _ink.value = it }
                .onFailure { _errorMessage.value = it.userMessage("Non riesco ad analizzare le pagine") }
            _scanning.value = false
        }
    }

    override fun clear() {
        scan?.cancel()
        _scanning.value = false
        super.clear()
        _ink.value = null
        _overrides.value = emptySet()
    }

    fun setSensitivity(value: BlankSensitivity) {
        _sensitivity.value = value
        _overrides.value = emptySet()
    }

    fun toggle(page: Int) {
        val current = _overrides.value
        _overrides.value = if (page in current) current - page else current + page
    }

    /** Pages that will be removed with the current sensitivity and manual choices. */
    fun toRemove(ink: List<Float>, sensitivity: BlankSensitivity, overrides: Set<Int>): Set<Int> =
        ink.indices.filter { (ink[it] <= sensitivity.maxInk) != (it in overrides) }.toSet()

    fun remove() {
        val pdf = source.value ?: return
        val ink = _ink.value ?: return
        val pages = toRemove(ink, _sensitivity.value, _overrides.value)
        if (pages.isEmpty()) return
        runJob {
            _progress.value = Pair(0, 1)
            finish(pageTools.removePages(pdf.uri, pdf.name, pages), "Togli pagine bianche", pdf.name, "Non sono riuscito a togliere le pagine")
        }
    }
}

class PdfNupViewModel(application: Application) : PageToolViewModel(application) {

    private val _perSheet = MutableStateFlow(2)
    val perSheet: StateFlow<Int> = _perSheet.asStateFlow()

    private val _frames = MutableStateFlow(true)
    val frames: StateFlow<Boolean> = _frames.asStateFlow()

    fun setPerSheet(value: Int) {
        _perSheet.value = value
    }

    fun setFrames(value: Boolean) {
        _frames.value = value
    }

    fun run() {
        val pdf = source.value ?: return
        val perSheet = _perSheet.value
        runJob {
            finish(
                pageTools.nUp(pdf.uri, pdf.name, perSheet, _frames.value) { c, t -> _progress.value = Pair(c, t) },
                "Più pagine per foglio",
                pdf.name,
                "Non sono riuscito a impaginare il PDF"
            )
        }
    }
}

class PdfNotesViewModel(application: Application) : PageToolViewModel(application) {

    private val _side = MutableStateFlow(NotesSide.RIGHT)
    val side: StateFlow<NotesSide> = _side.asStateFlow()

    private val _style = MutableStateFlow(NotesStyle.LINES)
    val style: StateFlow<NotesStyle> = _style.asStateFlow()

    fun setSide(value: NotesSide) {
        _side.value = value
    }

    fun setStyle(value: NotesStyle) {
        _style.value = value
    }

    fun run() {
        val pdf = source.value ?: return
        runJob {
            finish(
                pageTools.notes(pdf.uri, pdf.name, _side.value, _style.value) { c, t -> _progress.value = Pair(c, t) },
                "Spazio per appunti",
                pdf.name,
                "Non sono riuscito a creare il PDF per appunti"
            )
        }
    }
}

enum class DuplexMode(val label: String) { TWO_FILES("Due PDF"), ONE_FILE("Un PDF") }

/** Fronts use the inherited source; backs are a second PDF (two-files mode). */
class PdfDuplexViewModel(application: Application) : PageToolViewModel(application) {

    private val _backs = MutableStateFlow<LoadedPdf?>(null)
    val backs: StateFlow<LoadedPdf?> = _backs.asStateFlow()

    private val _mode = MutableStateFlow(DuplexMode.TWO_FILES)
    val mode: StateFlow<DuplexMode> = _mode.asStateFlow()

    private val _backsReversed = MutableStateFlow(true)
    val backsReversed: StateFlow<Boolean> = _backsReversed.asStateFlow()

    fun setMode(value: DuplexMode) {
        _mode.value = value
    }

    fun setBacksReversed(value: Boolean) {
        _backsReversed.value = value
    }

    fun loadBacks(uri: Uri) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            _errorMessage.value = null
            val info = withContext(Dispatchers.IO) { app.queryUriInfo(uri, "Retro.pdf") }
            tools.readInfo(uri)
                .onSuccess { pdf ->
                    when {
                        pdf.isEncrypted -> _errorMessage.value = "Il PDF dei retro è protetto da password: sbloccalo prima"
                        pdf.pageCount == 0 -> _errorMessage.value = "Il PDF dei retro non contiene pagine"
                        else -> _backs.value = LoadedPdf(uri, info.name, info.sizeBytes, pdf.pageCount)
                    }
                }
                .onFailure { _errorMessage.value = it.userMessage("Impossibile aprire il PDF dei retro") }
        }
    }

    fun clearBacks() {
        _backs.value = null
    }

    override fun clear() {
        super.clear()
        _backs.value = null
    }

    /** (side, page) order of the result for the current inputs, or empty when something is missing. */
    fun order(): List<Pair<Int, Int>> {
        val fronts = source.value?.pageCount ?: return emptyList()
        return when (_mode.value) {
            DuplexMode.TWO_FILES -> _backs.value?.let { PageMath.duplexOrder(fronts, it.pageCount, _backsReversed.value) } ?: emptyList()
            DuplexMode.ONE_FILE -> if (fronts < 2) emptyList() else PageMath.duplexOrder((fronts + 1) / 2, fronts / 2, _backsReversed.value)
        }
    }

    fun run() {
        val pdf = source.value ?: return
        val backs = if (_mode.value == DuplexMode.TWO_FILES) (_backs.value ?: return).uri else null
        runJob {
            finish(
                pageTools.duplex(pdf.uri, backs, pdf.name, _backsReversed.value) { c, t -> _progress.value = Pair(c, t) },
                "Unisci fronte/retro",
                pdf.name,
                "Non sono riuscito a unire fronti e retro"
            )
        }
    }
}

class PdfCropViewModel(application: Application) : PageToolViewModel(application) {

    private val _margins = MutableStateFlow(floatArrayOf(0.05f, 0.05f, 0.05f, 0.05f))
    /** Left, top, right, bottom as fractions of the page as shown. */
    val margins: StateFlow<FloatArray> = _margins.asStateFlow()

    private val _linked = MutableStateFlow(true)
    val linked: StateFlow<Boolean> = _linked.asStateFlow()

    private val _autoMessage = MutableStateFlow<String?>(null)
    val autoMessage: StateFlow<String?> = _autoMessage.asStateFlow()

    override fun onLoaded(pageCount: Int) {
        super.onLoaded(pageCount)
        _margins.value = floatArrayOf(0.05f, 0.05f, 0.05f, 0.05f)
        _autoMessage.value = null
    }

    fun setLinked(value: Boolean) {
        _linked.value = value
        if (value) {
            val m = _margins.value
            val avg = m.average().toFloat()
            _margins.value = floatArrayOf(avg, avg, avg, avg)
        }
    }

    fun setMargin(side: Int, value: Float) {
        val v = value.coerceIn(0f, MAX_MARGIN)
        _margins.value = if (_linked.value) floatArrayOf(v, v, v, v) else _margins.value.copyOf().also { it[side] = v }
        _autoMessage.value = null
    }

    /** Detects the white border around the first page's content and crops just outside it. */
    fun auto() {
        viewModelScope.launch {
            val page = thumbnail(0, 400) ?: return@launch
            val found = withContext(Dispatchers.Default) {
                val pixels = IntArray(page.width * page.height)
                page.getPixels(pixels, 0, page.width, 0, 0, page.width, page.height)
                PageMath.contentMargins(pixels, page.width, page.height)
            }
            if (found == null) {
                _autoMessage.value = "La prima pagina sembra vuota: regola i margini a mano"
            } else {
                _linked.value = false
                _margins.value = FloatArray(4) { (found[it] - AUTO_PADDING).coerceIn(0f, MAX_MARGIN) }
                _autoMessage.value = "Margini trovati sulla prima pagina"
            }
        }
    }

    fun run() {
        val pdf = source.value ?: return
        val m = _margins.value.copyOf()
        runJob {
            finish(
                pageTools.crop(pdf.uri, pdf.name, m) { c, t -> _progress.value = Pair(c, t) },
                "Ritaglia margini",
                pdf.name,
                "Non sono riuscito a ritagliare il PDF"
            )
        }
    }

    companion object {
        const val MAX_MARGIN = 0.35f
        private const val AUTO_PADDING = 0.015f
    }
}
