package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.CompiledMarkdownResult
import com.example.model.MarkdownExportSettings
import com.example.model.MarkdownSourceItem
import com.example.service.MarkdownExporterService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MarkdownExporterViewModel(application: Application) : AndroidViewModel(application) {

    private val markdownService = MarkdownExporterService(application.applicationContext)

    private val _items = MutableStateFlow<List<MarkdownSourceItem>>(emptyList())
    val items: StateFlow<List<MarkdownSourceItem>> = _items.asStateFlow()

    private val _settings = MutableStateFlow(MarkdownExportSettings())
    val settings: StateFlow<MarkdownExportSettings> = _settings.asStateFlow()

    private val _isIngesting = MutableStateFlow(false)
    val isIngesting: StateFlow<Boolean> = _isIngesting.asStateFlow()

    private val _extractionProgress = MutableStateFlow(0f)
    val extractionProgress: StateFlow<Float> = _extractionProgress.asStateFlow()

    private val _extractionStatusText = MutableStateFlow("")
    val extractionStatusText: StateFlow<String> = _extractionStatusText.asStateFlow()

    private val _compiledResult = MutableStateFlow<CompiledMarkdownResult?>(null)
    val compiledResult: StateFlow<CompiledMarkdownResult?> = _compiledResult.asStateFlow()

    private val _savedUri = MutableStateFlow<Uri?>(null)
    val savedUri: StateFlow<Uri?> = _savedUri.asStateFlow()

    private val _errorNotice = MutableStateFlow<String?>(null)
    val errorNotice: StateFlow<String?> = _errorNotice.asStateFlow()

    private val _copySuccessMessage = MutableStateFlow<String?>(null)
    val copySuccessMessage: StateFlow<String?> = _copySuccessMessage.asStateFlow()

    private var extractionJob: Job? = null

    fun addFiles(uris: List<Uri>) {
        val resolver = getApplication<Application>().contentResolver
        extractionJob?.cancel()

        extractionJob = viewModelScope.launch(Dispatchers.IO) {
            _isIngesting.value = true
            _errorNotice.value = null
            _extractionProgress.value = 0f

            val totalUris = uris.size
            for ((index, uri) in uris.withIndex()) {
                var name = "Document"
                var size = 0L
                var mime = resolver.getType(uri) ?: "application/octet-stream"

                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: "Document"
                        if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
                    }
                }

                _extractionStatusText.value = "Processing ${index + 1}/$totalUris: $name"

                val item = markdownService.extractTextFromSource(
                    uri = uri,
                    name = name,
                    mimeType = mime,
                    sizeBytes = size,
                    parseAllPages = _settings.value.parseAllPages
                ) { fileProgress, status ->
                    val overall = (index.toFloat() + fileProgress) / totalUris.toFloat()
                    _extractionProgress.value = overall
                    _extractionStatusText.value = "$status (${index + 1}/$totalUris)"
                }

                if (item.error != null) {
                    _errorNotice.value = "${item.name}: ${item.error}"
                }

                _items.value = _items.value + item
                recompileInternal()
            }

            _isIngesting.value = false
            _extractionProgress.value = 1f
            _extractionStatusText.value = "All documents parsed"
        }
    }

    fun toggleParseAllPages(enabled: Boolean) {
        _settings.value = _settings.value.copy(parseAllPages = enabled)
        if (enabled && _items.value.any { it.hasMorePages }) {
            parseAllDocumentsRemaining()
        }
    }

    fun parseAllDocumentsRemaining() {
        val itemsToParse = _items.value.filter { it.hasMorePages }
        if (itemsToParse.isEmpty()) return

        extractionJob?.cancel()
        extractionJob = viewModelScope.launch(Dispatchers.IO) {
            _isIngesting.value = true
            _extractionProgress.value = 0f

            for ((idx, doc) in itemsToParse.withIndex()) {
                _extractionStatusText.value = "Parsing remaining pages for ${doc.name} (${idx + 1}/${itemsToParse.size})..."
                val updated = markdownService.extractRemainingPages(doc) { progress, status ->
                    val overall = (idx.toFloat() + progress) / itemsToParse.size.toFloat()
                    _extractionProgress.value = overall
                    _extractionStatusText.value = "$status (${idx + 1}/${itemsToParse.size})"
                }
                _items.value = _items.value.map {
                    if (it.id == doc.id) updated else it
                }
                recompileInternal()
            }
            _isIngesting.value = false
            _extractionProgress.value = 1f
            _extractionStatusText.value = "All remaining pages parsed"
        }
    }

    fun removeItem(id: String) {
        _items.value = _items.value.filterNot { it.id == id }
        recompile()
    }

    fun clearAll() {
        extractionJob?.cancel()
        _items.value = emptyList()
        _compiledResult.value = null
        _savedUri.value = null
        _errorNotice.value = null
        _isIngesting.value = false
    }

    fun updateSettings(newSettings: MarkdownExportSettings) {
        _settings.value = newSettings
        recompile()
    }

    fun cancelExtraction() {
        extractionJob?.cancel()
        _isIngesting.value = false
        _extractionStatusText.value = "Extraction cancelled"
    }

    fun dismissErrorNotice() {
        _errorNotice.value = null
    }

    fun dismissCopySuccess() {
        _copySuccessMessage.value = null
    }

    fun parseRemainingPages(id: String) {
        val currentItem = _items.value.find { it.id == id } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isIngesting.value = true
            _extractionProgress.value = 0f
            _extractionStatusText.value = "Parsing remaining pages for ${currentItem.name}..."

            val updated = markdownService.extractRemainingPages(currentItem) { progress, status ->
                _extractionProgress.value = progress
                _extractionStatusText.value = status
            }

            _items.value = _items.value.map {
                if (it.id == id) updated else it
            }
            recompileInternal()
            _isIngesting.value = false
            _extractionStatusText.value = "Remaining pages parsed"
        }
    }

    fun recompile() {
        viewModelScope.launch(Dispatchers.IO) {
            recompileInternal()
        }
    }

    private fun recompileInternal() {
        val result = markdownService.compileMarkdown(_items.value, _settings.value)
        _compiledResult.value = result
    }

    fun copyPromptToClipboard() {
        val prompt = _compiledResult.value?.wrappedPrompt ?: return
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("OmniConvert LLM Markdown", prompt)
        clipboard.setPrimaryClip(clip)
        _copySuccessMessage.value = "Prompt copied to clipboard (~${_compiledResult.value?.totalTokens} tokens)"
    }

    fun saveAsMarkdown(title: String) {
        val content = _compiledResult.value?.wrappedPrompt ?: return
        viewModelScope.launch {
            val res = markdownService.saveMarkdownFile(content, title)
            res.onSuccess { uri ->
                _savedUri.value = uri
            }.onFailure { err ->
                _errorNotice.value = "Failed to save: ${err.localizedMessage}"
            }
        }
    }
}
