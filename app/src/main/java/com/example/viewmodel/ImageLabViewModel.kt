package com.example.viewmodel

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryKind
import com.example.data.HistoryRepository
import com.example.model.ImageConversionSettings
import com.example.model.ImageConvertItem
import com.example.service.ImageConverterService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ImageLabViewModel(application: Application) : AndroidViewModel(application) {

    private val imageService = ImageConverterService(application.applicationContext)

    private val _items = MutableStateFlow<List<ImageConvertItem>>(emptyList())
    val items: StateFlow<List<ImageConvertItem>> = _items.asStateFlow()

    private val _settings = MutableStateFlow(ImageConversionSettings())
    val settings: StateFlow<ImageConversionSettings> = _settings.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

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

            var w = 0
            var h = 0
            try {
                resolver.openInputStream(uri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(stream, null, opts)
                    w = opts.outWidth
                    h = opts.outHeight
                }
            } catch (_: Throwable) {}

            ImageConvertItem(
                uri = uri,
                name = name,
                originalSizeBytes = size,
                originalWidth = w,
                originalHeight = h
            )
        }
        _items.value = _items.value + newItems
    }

    fun removeItem(id: String) {
        _items.value = _items.value.filterNot { it.id == id }
    }

    fun clearAll() {
        _items.value = emptyList()
        _errorMessage.value = null
    }

    fun updateSettings(newSettings: ImageConversionSettings) {
        _settings.value = newSettings
    }

    fun convertAll() {
        val currentItems = _items.value
        if (currentItems.isEmpty()) return

        viewModelScope.launch {
            _isProcessing.value = true
            _errorMessage.value = null

            for (item in currentItems) {
                // Update processing status
                _items.value = _items.value.map {
                    if (it.id == item.id) it.copy(isProcessing = true) else it
                }

                val res = imageService.convertImage(item, _settings.value)
                res.onSuccess { pair ->
                    HistoryRepository.record(getApplication<Application>(), pair.first, "Converti immagini", HistoryKind.IMAGE, item.name, pair.second)
                    _items.value = _items.value.map {
                        if (it.id == item.id) it.copy(isProcessing = false, convertedSizeBytes = pair.second, error = null) else it
                    }
                }.onFailure { err ->
                    _items.value = _items.value.map {
                        if (it.id == item.id) it.copy(isProcessing = false, error = err.localizedMessage) else it
                    }
                }
            }
            _isProcessing.value = false
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}
