package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.PdfDocumentItem
import com.example.model.PdfExportConfig
import com.example.service.PdfConverterService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PdfStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val pdfService = PdfConverterService(application.applicationContext)

    private val _items = MutableStateFlow<List<PdfDocumentItem>>(emptyList())
    val items: StateFlow<List<PdfDocumentItem>> = _items.asStateFlow()

    private val _config = MutableStateFlow(PdfExportConfig())
    val config: StateFlow<PdfExportConfig> = _config.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationProgress = MutableStateFlow(Pair(0, 0))
    val generationProgress: StateFlow<Pair<Int, Int>> = _generationProgress.asStateFlow()

    private val _lastGeneratedUri = MutableStateFlow<Uri?>(null)
    val lastGeneratedUri: StateFlow<Uri?> = _lastGeneratedUri.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun addImages(uris: List<Uri>) {
        val resolver = getApplication<Application>().contentResolver
        val newItems = uris.map { uri ->
            var name = "Image"
            var size = 0L
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: "Image"
                    if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
                }
            }
            PdfDocumentItem(uri = uri, name = name, sizeBytes = size)
        }
        _items.value = _items.value + newItems
    }

    fun removeItem(id: String) {
        _items.value = _items.value.filterNot { it.id == id }
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex in _items.value.indices && toIndex in _items.value.indices) {
            val list = _items.value.toMutableList()
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _items.value = list
        }
    }

    fun clearAll() {
        _items.value = emptyList()
        _lastGeneratedUri.value = null
        _errorMessage.value = null
    }

    fun toggleSpreadSplit(id: String) {
        val current = _items.value
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val item = current[index]
            if (item.isSplitSpread) {
                // Remove the paired split and restore
                _items.value = current.toMutableList().apply {
                    set(index, item.copy(isSplitSpread = false, splitPart = 1))
                }
            } else {
                // Split into two items: left page and right page
                val left = item.copy(isSplitSpread = true, splitPart = 1)
                val right = item.copy(id = java.util.UUID.randomUUID().toString(), isSplitSpread = true, splitPart = 2)
                _items.value = current.toMutableList().apply {
                    set(index, left)
                    add(index + 1, right)
                }
            }
        }
    }

    fun toggleBinarization(id: String) {
        _items.value = _items.value.map {
            if (it.id == id) it.copy(isBinarized = !it.isBinarized) else it
        }
    }

    fun rotateItem(id: String) {
        _items.value = _items.value.map {
            if (it.id == id) it.copy(rotationDegrees = (it.rotationDegrees + 90) % 360) else it
        }
    }

    fun updateConfig(newConfig: PdfExportConfig) {
        _config.value = newConfig
    }

    fun generatePdf() {
        if (_items.value.isEmpty()) return
        viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null
            _lastGeneratedUri.value = null

            val result = pdfService.generatePdf(_items.value, _config.value) { current, total ->
                _generationProgress.value = Pair(current, total)
            }

            result.onSuccess { uri ->
                _lastGeneratedUri.value = uri
            }.onFailure { err ->
                _errorMessage.value = err.localizedMessage ?: "PDF generation failed"
            }
            _isGenerating.value = false
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}
