package com.example.viewmodel

import android.app.Application
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryKind
import com.example.data.HistoryRepository
import com.example.service.PhotoEditService
import com.example.service.RotateSettings
import com.example.service.SocialSettings
import com.example.service.WatermarkSettings
import com.example.util.FileNames
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.util.PhotoMath
import com.example.util.queryUriInfo
import com.example.util.resultOf
import com.example.util.userMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.sqrt

/** Shared plumbing of the batch photo tools: the list, the preview, progress and results. */
abstract class BatchPhotoViewModel<S>(application: Application, initial: S) : AndroidViewModel(application) {

    protected val editor = PhotoEditService(application.applicationContext)

    private val _photos = MutableStateFlow<List<FilterPhoto>>(emptyList())
    val photos: StateFlow<List<FilterPhoto>> = _photos.asStateFlow()

    private val _settings = MutableStateFlow(initial)
    val settings: StateFlow<S> = _settings.asStateFlow()

    private val _previewId = MutableStateFlow<String?>(null)
    val previewId: StateFlow<String?> = _previewId.asStateFlow()

    private val _progress = MutableStateFlow<Float?>(null)
    val progress: StateFlow<Float?> = _progress.asStateFlow()

    private val _savedCount = MutableStateFlow<Int?>(null)
    val savedCount: StateFlow<Int?> = _savedCount.asStateFlow()

    private val _lastSaved = MutableStateFlow<Uri?>(null)
    val lastSaved: StateFlow<Uri?> = _lastSaved.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var job: Job? = null

    protected abstract val toolLabel: String

    protected abstract suspend fun process(photo: FilterPhoto, settings: S): Result<Pair<Uri, Long>>

    /** Reason the current settings can't be applied, or null when they can. */
    open fun blockingReason(settings: S): String? = null

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

    fun updateSettings(transform: (S) -> S) {
        _settings.update(transform)
        _savedCount.value = null
    }

    fun applyAll() {
        val list = _photos.value
        val settings = _settings.value
        if (list.isEmpty() || _progress.value != null || blockingReason(settings) != null) return
        _errorMessage.value = null
        _progress.value = 0f
        job = viewModelScope.launch {
            var saved = 0
            try {
                list.forEachIndexed { index, photo ->
                    process(photo, settings)
                        .onSuccess { (uri, size) ->
                            saved++
                            _lastSaved.value = uri
                            HistoryRepository.record(getApplication<Application>(), uri, toolLabel, HistoryKind.IMAGE, photo.name, size)
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

class SocialFormatViewModel(application: Application) : BatchPhotoViewModel<SocialSettings>(application, SocialSettings()) {
    override val toolLabel = "Formato social"
    override suspend fun process(photo: FilterPhoto, settings: SocialSettings) = editor.social(photo.uri, photo.name, settings)
}

class PhotoWatermarkViewModel(application: Application) : BatchPhotoViewModel<WatermarkSettings>(application, WatermarkSettings()) {
    override val toolLabel = "Filigrana foto"
    override suspend fun process(photo: FilterPhoto, settings: WatermarkSettings) = editor.watermark(photo.uri, photo.name, settings)
    override fun blockingReason(settings: WatermarkSettings): String? = if (settings.text.isBlank()) "Scrivi il testo della filigrana" else null
}

class RotatePhotosViewModel(application: Application) : BatchPhotoViewModel<RotateSettings>(application, RotateSettings()) {
    override val toolLabel = "Ruota e specchia"
    override suspend fun process(photo: FilterPhoto, settings: RotateSettings) = editor.rotate(photo.uri, photo.name, settings)
    override fun blockingReason(settings: RotateSettings): String? = if (settings.isIdentity) "Ruota o specchia almeno una volta" else null

    fun turn(clockwise: Boolean) = updateSettings { it.copy(degrees = it.degrees + if (clockwise) 90 else -90) }
    fun flip(horizontal: Boolean) = updateSettings {
        if (horizontal) it.copy(flipHorizontal = !it.flipHorizontal) else it.copy(flipVertical = !it.flipVertical)
    }
    fun reset() = updateSettings { RotateSettings() }
}

/** "Palette colori": dominant colours of one photo. */
class PaletteViewModel(application: Application) : AndroidViewModel(application) {

    private val editor = PhotoEditService(application.applicationContext)

    private val _photo = MutableStateFlow<FilterPhoto?>(null)
    val photo: StateFlow<FilterPhoto?> = _photo.asStateFlow()

    private val _colors = MutableStateFlow<List<Pair<Int, Float>>>(emptyList())
    /** ARGB colour and its share of the photo, largest first. */
    val colors: StateFlow<List<Pair<Int, Float>>> = _colors.asStateFlow()

    private val _count = MutableStateFlow(6)
    val count: StateFlow<Int> = _count.asStateFlow()

    private val _analyzing = MutableStateFlow(false)
    val analyzing: StateFlow<Boolean> = _analyzing.asStateFlow()

    private val _savedUri = MutableStateFlow<Uri?>(null)
    val savedUri: StateFlow<Uri?> = _savedUri.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var pixels: IntArray? = null
    private var job: Job? = null

    fun load(uri: Uri) {
        val app = getApplication<Application>()
        job?.cancel()
        job = viewModelScope.launch {
            _analyzing.value = true
            _savedUri.value = null
            _colors.value = emptyList()
            val name = withContext(Dispatchers.IO) { app.queryUriInfo(uri, "Foto").name }
            _photo.value = FilterPhoto(uri, name)
            val sampled = editor.samplePixels(uri)
            if (sampled == null) {
                _errorMessage.value = "Immagine non leggibile"
                _photo.value = null
            } else {
                pixels = sampled
                extract()
            }
            _analyzing.value = false
        }
    }

    fun setCount(value: Int) {
        _count.value = value
        viewModelScope.launch { extract() }
    }

    private suspend fun extract() {
        val source = pixels ?: return
        val n = _count.value
        _colors.value = withContext(Dispatchers.Default) { PhotoMath.palette(source, n) }
    }

    fun save() {
        val photo = _photo.value ?: return
        val colors = _colors.value.map { it.first }
        viewModelScope.launch {
            editor.savePalette(colors, photo.name)
                .onSuccess { (uri, size) ->
                    _savedUri.value = uri
                    HistoryRepository.record(getApplication<Application>(), uri, "Palette colori", HistoryKind.IMAGE, photo.name, size)
                }
                .onFailure { _errorMessage.value = it.userMessage("Non sono riuscito a salvare la palette") }
        }
    }

    fun clear() {
        job?.cancel()
        _photo.value = null
        _colors.value = emptyList()
        _savedUri.value = null
        pixels = null
        _analyzing.value = false
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun dismissSaved() {
        _savedUri.value = null
    }
}

enum class RecorderState { IDLE, RECORDING, PAUSED }

/** "Registra audio": AAC voice recorder with a live level meter, saved in Download/Morph/Audio. */
class RecorderViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(RecorderState.IDLE)
    val state: StateFlow<RecorderState> = _state.asStateFlow()

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    private val _levels = MutableStateFlow<List<Float>>(emptyList())
    /** Recent input levels (0..1), newest last, for the waveform. */
    val levels: StateFlow<List<Float>> = _levels.asStateFlow()

    private val _savedUri = MutableStateFlow<Uri?>(null)
    val savedUri: StateFlow<Uri?> = _savedUri.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var meter: Job? = null
    private var segmentStart = 0L
    private var accumulated = 0L

    fun start() {
        if (_state.value != RecorderState.IDLE) return
        val app = getApplication<Application>()
        val target = File(app.cacheDir, "registrazione_${System.currentTimeMillis()}.m4a")
        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(app) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(128_000)
            mr.setAudioSamplingRate(44_100)
            mr.setAudioChannels(1)
            mr.setOutputFile(target.absolutePath)
            mr.prepare()
            mr.start()
        } catch (e: Exception) {
            runCatching { mr.release() }
            target.delete()
            _errorMessage.value = "Il microfono non è disponibile: controlla il permesso o chiudi le altre app che registrano"
            return
        }
        recorder = mr
        file = target
        accumulated = 0L
        segmentStart = SystemClock.elapsedRealtime()
        _elapsedMs.value = 0L
        _levels.value = emptyList()
        _savedUri.value = null
        _state.value = RecorderState.RECORDING
        startMeter()
    }

    fun pause() {
        val mr = recorder ?: return
        if (_state.value != RecorderState.RECORDING) return
        runCatching { mr.pause() }
        accumulated += SystemClock.elapsedRealtime() - segmentStart
        _state.value = RecorderState.PAUSED
    }

    fun resume() {
        val mr = recorder ?: return
        if (_state.value != RecorderState.PAUSED) return
        runCatching { mr.resume() }
        segmentStart = SystemClock.elapsedRealtime()
        _state.value = RecorderState.RECORDING
    }

    /** Stops and saves the recording (too short ones, under half a second, are discarded). */
    fun stop() {
        val mr = recorder ?: return
        val recorded = file
        meter?.cancel()
        if (_state.value == RecorderState.RECORDING) accumulated += SystemClock.elapsedRealtime() - segmentStart
        val ok = runCatching { mr.stop() }.isSuccess
        runCatching { mr.release() }
        recorder = null
        _state.value = RecorderState.IDLE
        if (!ok || recorded == null || accumulated < 500) {
            recorded?.delete()
            _errorMessage.value = "Registrazione troppo breve"
            return
        }
        val app = getApplication<Application>()
        _saving.value = true
        viewModelScope.launch {
            resultOf {
                withContext(Dispatchers.IO) {
                    val name = FileNames.stamped("Registrazione", "Registrazione", "m4a")
                    OutputStorage.save(app, name, "audio/mp4", OutputFolder.AUDIO) { out ->
                        recorded.inputStream().use { it.copyTo(out) }
                    }
                }
            }.onSuccess { uri ->
                _savedUri.value = uri
                HistoryRepository.record(app, uri, "Registra audio", HistoryKind.AUDIO, "Registrazione.m4a", recorded.length())
            }.onFailure {
                _errorMessage.value = it.userMessage("Non sono riuscito a salvare la registrazione")
            }
            recorded.delete()
            _saving.value = false
        }
    }

    /** Throws away the current recording. */
    fun discard() {
        meter?.cancel()
        recorder?.let { mr ->
            runCatching { mr.stop() }
            runCatching { mr.release() }
        }
        recorder = null
        file?.delete()
        file = null
        _state.value = RecorderState.IDLE
        _elapsedMs.value = 0L
        _levels.value = emptyList()
    }

    private fun startMeter() {
        meter?.cancel()
        meter = viewModelScope.launch {
            while (isActive) {
                val mr = recorder ?: break
                if (_state.value == RecorderState.RECORDING) {
                    val amplitude = runCatching { mr.maxAmplitude }.getOrDefault(0)
                    // Square root makes quiet speech visible without clipping loud parts.
                    val level = sqrt((amplitude / 32767f).coerceIn(0f, 1f))
                    _levels.update { (it + level).takeLast(LEVELS) }
                    _elapsedMs.value = accumulated + SystemClock.elapsedRealtime() - segmentStart
                }
                delay(70)
            }
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun dismissSaved() {
        _savedUri.value = null
        _elapsedMs.value = 0L
        _levels.value = emptyList()
    }

    override fun onCleared() {
        discard()
        super.onCleared()
    }

    companion object {
        const val LEVELS = 48
    }
}
