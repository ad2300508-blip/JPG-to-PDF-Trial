package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.PdfDocumentItem
import com.example.model.PdfMergeSettings
import com.example.service.PdfMergeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    fun addPdfDocuments(uris: List<Uri>) {
        val resolver = getApplication<Application>().contentResolver
        viewModelScope.launch(Dispatchers.IO) {
            val newItems = uris.map { uri ->
                var name = "Document.pdf"
                var size = 0L
                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: "Document.pdf"
                        if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
                    }
                }

                var pages = 0
                var thumb: Bitmap? = null
                var isCorrupt = false
                var error: String? = null

                try {
                    resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        val renderer = PdfRenderer(pfd)
                        pages = renderer.pageCount
                        if (pages > 0) {
                            val firstPage = renderer.openPage(0)
                            val w = (firstPage.width / 3).coerceIn(120, 240)
                            val h = (firstPage.height / 3).coerceIn(160, 320)
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            firstPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            thumb = bmp
                            firstPage.close()
                        }
                        renderer.close()
                    }
                } catch (t: Throwable) {
                    isCorrupt = true
                    error = t.localizedMessage ?: "Failed to read PDF structure"
                }

                PdfDocumentItem(
                    uri = uri,
                    fileName = name,
                    name = name,
                    fileSizeBytes = size,
                    sizeBytes = size,
                    pageCount = pages,
                    thumbnailBitmap = thumb,
                    isCorrupted = isCorrupt,
                    errorMessage = error
                )
            }

            _documents.value = _documents.value + newItems
        }
    }

    fun removeDocument(id: String) {
        _documents.value = _documents.value.filterNot { it.id == id }
    }

    fun moveDocumentUp(index: Int) {
        if (index > 0) {
            val current = _documents.value.toMutableList()
            val temp = current[index]
            current[index] = current[index - 1]
            current[index - 1] = temp
            _documents.value = current
        }
    }

    fun moveDocumentDown(index: Int) {
        if (index < _documents.value.size - 1) {
            val current = _documents.value.toMutableList()
            val temp = current[index]
            current[index] = current[index + 1]
            current[index + 1] = temp
            _documents.value = current
        }
    }

    fun clearAll() {
        _documents.value = emptyList()
        _lastMergedUri.value = null
        _errorMessage.value = null
    }

    fun updateSettings(newSettings: PdfMergeSettings) {
        _settings.value = newSettings
    }

    fun updatePageRange(id: String, range: String) {
        _documents.value = _documents.value.map {
            if (it.id == id) it.copy(pageRange = range) else it
        }
    }

    fun getTotalPageCount(): Int {
        return _documents.value.sumOf { it.pageCount }
    }

    fun getTotalFileSizeBytes(): Long {
        return _documents.value.sumOf {
            val s = if (it.fileSizeBytes > 0L) it.fileSizeBytes else it.sizeBytes
            s
        }
    }

    fun mergeDocuments() {
        if (_documents.value.isEmpty()) return

        viewModelScope.launch {
            _isMerging.value = true
            _errorMessage.value = null
            _lastMergedUri.value = null

            val result = mergeService.mergePdfs(_documents.value, _settings.value) { current, total ->
                _progress.value = Pair(current, total)
            }

            result.onSuccess { uri ->
                _lastMergedUri.value = uri
            }.onFailure { err ->
                _errorMessage.value = err.localizedMessage ?: "Merging failed"
            }

            _isMerging.value = false
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun dismissSuccess() {
        _lastMergedUri.value = null
    }
}
