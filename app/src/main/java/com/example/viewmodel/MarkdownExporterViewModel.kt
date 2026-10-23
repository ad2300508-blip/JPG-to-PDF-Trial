package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.CompiledMarkdownResult
import com.example.model.LlmPromptWrapper
import com.example.model.MarkdownExportSettings
import com.example.model.MarkdownSourceItem
import com.example.service.MarkdownExporterService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MarkdownExporterViewModel(application: Application) : AndroidViewModel(application) {

  private val _items = MutableStateFlow<List<MarkdownSourceItem>>(emptyList())
  val items: StateFlow<List<MarkdownSourceItem>> = _items.asStateFlow()

  private val _settings = MutableStateFlow(MarkdownExportSettings())
  val settings: StateFlow<MarkdownExportSettings> = _settings.asStateFlow()

  private val _isExtracting = MutableStateFlow(false)
  val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()

  private val _compiledResult = MutableStateFlow<CompiledMarkdownResult?>(null)
  val compiledResult: StateFlow<CompiledMarkdownResult?> = _compiledResult.asStateFlow()

  private val _clipboardMessage = MutableStateFlow<String?>(null)
  val clipboardMessage: StateFlow<String?> = _clipboardMessage.asStateFlow()

  private val _exportMessage = MutableStateFlow<String?>(null)
  val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

  fun addUris(uris: List<Uri>) {
    if (uris.isEmpty()) return
    viewModelScope.launch {
      _isExtracting.value = true
      val context = getApplication<Application>()
      val newItems = mutableListOf<MarkdownSourceItem>()

      for (uri in uris) {
        val item = MarkdownExporterService.extractSource(context, uri)
        newItems.add(item)
      }

      _items.update { current -> current + newItems }
      _isExtracting.value = false
      recompile()
    }
  }

  fun removeItem(id: String) {
    _items.update { current -> current.filter { it.id != id } }
    recompile()
  }

  fun clearAll() {
    _items.value = emptyList()
    _compiledResult.value = null
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
    val currentSettings = _settings.value
    val res = MarkdownExporterService.compileBundle(currentItems, currentSettings)
    _compiledResult.value = res
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
    viewModelScope.launch {
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

  fun clearClipboardMessage() {
    _clipboardMessage.value = null
  }

  fun clearExportMessage() {
    _exportMessage.value = null
  }
}
