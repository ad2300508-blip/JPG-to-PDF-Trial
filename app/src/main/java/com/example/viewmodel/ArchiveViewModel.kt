package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ArchiveConfig
import com.example.model.ArchiveItem
import com.example.service.ArchiveService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArchiveViewModel(application: Application) : AndroidViewModel(application) {

    private val archiveService = ArchiveService(application.applicationContext)

    private val _items = MutableStateFlow<List<ArchiveItem>>(emptyList())
    val items: StateFlow<List<ArchiveItem>> = _items.asStateFlow()

    private val _config = MutableStateFlow(ArchiveConfig())
    val config: StateFlow<ArchiveConfig> = _config.asStateFlow()

    private val _isArchiving = MutableStateFlow(false)
    val isArchiving: StateFlow<Boolean> = _isArchiving.asStateFlow()

    private val _progress = MutableStateFlow(Pair(0, 0))
    val progress: StateFlow<Pair<Int, Int>> = _progress.asStateFlow()

    private val _lastGeneratedUri = MutableStateFlow<Uri?>(null)
    val lastGeneratedUri: StateFlow<Uri?> = _lastGeneratedUri.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun addFiles(uris: List<Uri>) {
        val resolver = getApplication<Application>().contentResolver
        val newItems = uris.map { uri ->
            var name = "File"
            var size = 0L
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: "File"
                    if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
                }
            }
            ArchiveItem(uri = uri, name = name, sizeBytes = size)
        }
        _items.value = _items.value + newItems
    }

    fun removeItem(id: String) {
        _items.value = _items.value.filterNot { it.id == id }
    }

    fun clearAll() {
        _items.value = emptyList()
        _lastGeneratedUri.value = null
        _errorMessage.value = null
    }

    fun updateConfig(newConfig: ArchiveConfig) {
        _config.value = newConfig
    }

    fun createArchive() {
        if (_items.value.isEmpty()) return
        viewModelScope.launch {
            _isArchiving.value = true
            _errorMessage.value = null
            _lastGeneratedUri.value = null

            val res = archiveService.createZip(_items.value, _config.value) { current, total ->
                _progress.value = Pair(current, total)
            }

            res.onSuccess { pair ->
                _lastGeneratedUri.value = pair.first
            }.onFailure { err ->
                _errorMessage.value = err.localizedMessage ?: "Failed to generate ZIP"
            }

            _isArchiving.value = false
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}
