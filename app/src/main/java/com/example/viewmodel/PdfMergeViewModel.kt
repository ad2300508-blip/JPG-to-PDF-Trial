package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.PdfDocumentItem
import com.example.model.PdfMergeSettings
import com.example.service.PdfMergeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

sealed class MergeState {
  object Idle : MergeState()
  data class Merging(val current: Int, val total: Int, val currentFileName: String) : MergeState()
  data class Success(val mergedFile: File, val totalPages: Int, val fileSizeBytes: Long) : MergeState()
  data class Error(val message: String) : MergeState()
}

class PdfMergeViewModel : ViewModel() {

  private val _pdfDocuments = MutableStateFlow<List<PdfDocumentItem>>(emptyList())
  val pdfDocuments: StateFlow<List<PdfDocumentItem>> = _pdfDocuments.asStateFlow()

  private val _settings = MutableStateFlow(PdfMergeSettings())
  val settings: StateFlow<PdfMergeSettings> = _settings.asStateFlow()

  private val _mergeState = MutableStateFlow<MergeState>(MergeState.Idle)
  val mergeState: StateFlow<MergeState> = _mergeState.asStateFlow()

  private val _isReadingFiles = MutableStateFlow(false)
  val isReadingFiles: StateFlow<Boolean> = _isReadingFiles.asStateFlow()

  val totalCombinedPages: StateFlow<Int> = _pdfDocuments.map { list ->
    list.sumOf { it.pageCount }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val totalInputSizeBytes: StateFlow<Long> = _pdfDocuments.map { list ->
    list.sumOf { it.fileSizeBytes }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

  val formattedTotalInputSize: StateFlow<String> = totalInputSizeBytes.map { bytes ->
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    when {
      mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
      kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
      bytes > 0 -> "$bytes B"
      else -> "0 B"
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0 B")

  fun addPdfs(context: Context, uris: List<Uri>) {
    if (uris.isEmpty()) return
    viewModelScope.launch {
      _isReadingFiles.value = true
      val newItems = mutableListOf<PdfDocumentItem>()
      for (uri in uris) {
        try {
          val item = PdfMergeService.extractPdfItem(context, uri)
          newItems.add(item)
        } catch (_: Exception) {}
      }
      _pdfDocuments.value = _pdfDocuments.value + newItems
      _isReadingFiles.value = false
    }
  }

  fun removePdf(id: String) {
    _pdfDocuments.value = _pdfDocuments.value.filter { it.id != id }
  }

  fun moveUp(index: Int) {
    if (index <= 0 || index >= _pdfDocuments.value.size) return
    val current = _pdfDocuments.value.toMutableList()
    val item = current.removeAt(index)
    current.add(index - 1, item)
    _pdfDocuments.value = current
  }

  fun moveDown(index: Int) {
    if (index < 0 || index >= _pdfDocuments.value.size - 1) return
    val current = _pdfDocuments.value.toMutableList()
    val item = current.removeAt(index)
    current.add(index + 1, item)
    _pdfDocuments.value = current
  }

  fun clearAll() {
    _pdfDocuments.value = emptyList()
    _mergeState.value = MergeState.Idle
  }

  fun updatePageRange(id: String, range: String) {
    _pdfDocuments.value = _pdfDocuments.value.map { item ->
      if (item.id == id) item.copy(pageRange = range) else item
    }
  }

  fun updateSettings(updater: (PdfMergeSettings) -> PdfMergeSettings) {
    _settings.value = updater(_settings.value)
  }

  fun sortByName(ascending: Boolean = true) {
    _pdfDocuments.value = if (ascending) {
      _pdfDocuments.value.sortedBy { it.fileName.lowercase() }
    } else {
      _pdfDocuments.value.sortedByDescending { it.fileName.lowercase() }
    }
  }

  fun sortBySize(descending: Boolean = true) {
    _pdfDocuments.value = if (descending) {
      _pdfDocuments.value.sortedByDescending { it.fileSizeBytes }
    } else {
      _pdfDocuments.value.sortedBy { it.fileSizeBytes }
    }
  }

  fun sortByPageCount(descending: Boolean = true) {
    _pdfDocuments.value = if (descending) {
      _pdfDocuments.value.sortedByDescending { it.pageCount }
    } else {
      _pdfDocuments.value.sortedBy { it.pageCount }
    }
  }

  fun startMerge(context: Context) {
    val items = _pdfDocuments.value
    if (items.size < 2) {
      _mergeState.value = MergeState.Error("Please select at least 2 PDF documents to merge.")
      return
    }

    // Check if any selected item is encrypted or corrupted
    val unreadable = items.filter { it.isEncrypted || it.isCorrupted }
    if (unreadable.isNotEmpty()) {
      _mergeState.value = MergeState.Error(
        "Cannot merge: \"${unreadable.first().fileName}\" is password protected or corrupted. Please remove it first."
      )
      return
    }

    viewModelScope.launch(Dispatchers.IO) {
      _mergeState.value = MergeState.Merging(1, items.size, items.first().fileName)

      val result = PdfMergeService.mergePdfs(
        context = context,
        items = items,
        settings = _settings.value,
        onProgress = { current, total, name ->
          _mergeState.value = MergeState.Merging(current, total, name)
        }
      )

      result.onSuccess { file ->
        val totalPages = items.sumOf { it.pageCount }
        _mergeState.value = MergeState.Success(
          mergedFile = file,
          totalPages = totalPages,
          fileSizeBytes = file.length()
        )
      }.onFailure { err ->
        _mergeState.value = MergeState.Error(err.localizedMessage ?: "Failed to merge PDF documents.")
      }
    }
  }

  fun resetMergeState() {
    _mergeState.value = MergeState.Idle
  }
}
