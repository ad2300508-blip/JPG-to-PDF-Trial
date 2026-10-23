package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.CompiledMarkdownResult
import com.example.model.LlmPromptWrapper
import com.example.model.MarkdownExportSettings
import com.example.model.MarkdownSourceItem
import com.example.model.SourceFileType
import com.example.service.MarkdownExporterService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class MarkdownExporterViewModel(application: Application) : AndroidViewModel(application) {

  private val _items = MutableStateFlow<List<MarkdownSourceItem>>(emptyList())
  val items: StateFlow<List<MarkdownSourceItem>> = _items.asStateFlow()

  private val _settings = MutableStateFlow(MarkdownExportSettings())
  val settings: StateFlow<MarkdownExportSettings> = _settings.asStateFlow()

  private val _isExtracting = MutableStateFlow(false)
  val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()

  private val _extractionProgress = MutableStateFlow(0f)
  val extractionProgress: StateFlow<Float> = _extractionProgress.asStateFlow()

  private val _extractionStatusText = MutableStateFlow("")
  val extractionStatusText: StateFlow<String> = _extractionStatusText.asStateFlow()

  private val _isCompiling = MutableStateFlow(false)
  val isCompiling: StateFlow<Boolean> = _isCompiling.asStateFlow()

  private val _compiledResult = MutableStateFlow<CompiledMarkdownResult?>(null)
  val compiledResult: StateFlow<CompiledMarkdownResult?> = _compiledResult.asStateFlow()

  private val _errorNotice = MutableStateFlow<String?>(null)
  val errorNotice: StateFlow<String?> = _errorNotice.asStateFlow()

  private val _clipboardMessage = MutableStateFlow<String?>(null)
  val clipboardMessage: StateFlow<String?> = _clipboardMessage.asStateFlow()

  private val _exportMessage = MutableStateFlow<String?>(null)
  val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

  private var extractionJob: Job? = null
  private var compileJob: Job? = null

  fun addUris(uris: List<Uri>) {
    if (uris.isEmpty()) return
    extractionJob?.cancel()
    extractionJob = viewModelScope.launch(Dispatchers.IO) {
      _isExtracting.value = true
      _extractionProgress.value = 0f
      _extractionStatusText.value = "Starting document import..."
      val context = getApplication<Application>()
      val newItems = mutableListOf<MarkdownSourceItem>()
      val total = uris.size

      for (i in 0 until total) {
        val uri = uris[i]
        val baseProgress = i.toFloat() / total.toFloat()

        val item = try {
          MarkdownExporterService.extractSource(context, uri) { subProgress, status ->
            _extractionProgress.value = (baseProgress + (subProgress / total.toFloat())).coerceIn(0f, 1f)
            _extractionStatusText.value = status
          }
        } catch (t: Throwable) {
          Log.e("MarkdownExporterVM", "Uncaught extraction error for $uri: ${t.localizedMessage}", t)
          System.gc()
          MarkdownSourceItem(
            id = UUID.randomUUID().toString(),
            uri = uri,
            fileName = "document_${System.currentTimeMillis()}",
            fileSizeBytes = 0L,
            fileType = SourceFileType.TEXT_DOCUMENT,
            extractedRawText = "Extraction failed",
            structuredMarkdown = "> ⚠️ Failed to parse document: Insufficient memory or unsupported format",
            tokenEstimate = 0,
            error = "Insufficient memory or unsupported format"
          )
        }

        if (item.error != null) {
          _errorNotice.value = "Document '${item.fileName}' could not be parsed: ${item.error}"
        }

        newItems.add(item)
      }

      _items.update { current -> current + newItems }
      _extractionProgress.value = 1f
      _extractionStatusText.value = "Extraction complete"
      _isExtracting.value = false
      recompile()
    }
  }

  fun cancelExtraction() {
    extractionJob?.cancel()
    _isExtracting.value = false
    _extractionStatusText.value = "Import cancelled"
  }

  fun removeItem(id: String) {
    _items.update { current -> current.filter { it.id != id } }
    recompile()
  }

  fun clearAll() {
    extractionJob?.cancel()
    _items.value = emptyList()
    _compiledResult.value = null
    _isExtracting.value = false
    _extractionProgress.value = 0f
    _extractionStatusText.value = ""
    _errorNotice.value = null
  }

  fun updateSettings(updater: (MarkdownExportSettings) -> MarkdownExportSettings) {
    _settings.update(updater)
    recompile()
  }

  fun recompile() {
    val currentItems = _items.value
    if (currentItems.isEmpty()) {
      _compiledResult.value = null
      return
    }
    compileJob?.cancel()
    compileJob = viewModelScope.launch(Dispatchers.IO) {
      _isCompiling.value = true
      try {
        val currentSettings = _settings.value
        val res = MarkdownExporterService.compileBundle(currentItems, currentSettings)
        _compiledResult.value = res
      } catch (t: Throwable) {
        Log.e("MarkdownExporterVM", "Recompile error: ${t.localizedMessage}", t)
        System.gc()
        _errorNotice.value = "Failed to assemble Markdown bundle: ${t.localizedMessage}"
      } finally {
        _isCompiling.value = false
      }
    }
  }

  fun copyToClipboard() {
    val result = _compiledResult.value ?: return
    val context = getApplication<Application>()
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("OmniConvert LLM Markdown", result.compiledMarkdown)
    clipboard.setPrimaryClip(clip)
    _clipboardMessage.value = "Copied to Clipboard (~${result.estimatedTokens} tokens ready for LLM prompt!)"
  }

  fun exportToDownloads() {
    val result = _compiledResult.value ?: return
    viewModelScope.launch(Dispatchers.IO) {
      val context = getApplication<Application>()
      val saveRes = MarkdownExporterService.exportMarkdownFile(context, result)
      if (saveRes.isSuccess) {
        val downloadInfo = saveRes.getOrThrow()
        _exportMessage.value = "Saved ${downloadInfo.fileName} to Downloads/OmniConvert/Markdown"
      } else {
        _exportMessage.value = "Export failed: ${saveRes.exceptionOrNull()?.localizedMessage}"
      }
    }
  }

  fun dismissErrorNotice() {
    _errorNotice.value = null
  }

  fun clearClipboardMessage() {
    _clipboardMessage.value = null
  }

  fun clearExportMessage() {
    _exportMessage.value = null
  }
}
