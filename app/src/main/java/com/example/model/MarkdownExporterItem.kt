package com.example.model

import android.net.Uri
import java.io.File
import java.util.Locale

enum class SourceFileType(val badgeText: String, val extension: String) {
  PDF("PDF", "pdf"),
  IMAGE_OCR("OCR", "png"),
  TEXT_DOCUMENT("TXT", "txt"),
  CODE_FILE("CODE", "code"),
  AUDIO_NOTE("AUD", "audio");

  val isTextBased: Boolean
    get() = this == TEXT_DOCUMENT || this == CODE_FILE
}

enum class LlmPromptWrapper(val displayName: String, val description: String) {
  CLAUDE_XML(
    "Claude XML Context",
    "Uses <documents><document index=\"N\"> tags for multi-document reasoning"
  ),
  CHATGPT_SYSTEM(
    "ChatGPT Analyst Prompt",
    "Injects system role context for structured query answering"
  ),
  GEMINI_BRIEF(
    "Gemini Research Brief",
    "High-density factual synthesis framing for deep context windows"
  ),
  RAW_MARKDOWN(
    "Raw Markdown (Clean)",
    "Clean standard Markdown headers and horizontal rule separators"
  );
}

data class MarkdownExportSettings(
  val bundleTitle: String = "Context_Bundle_${System.currentTimeMillis() / 1000}",
  val llmWrapper: LlmPromptWrapper = LlmPromptWrapper.CLAUDE_XML,
  val includeMetadataHeader: Boolean = true,
  val convertTablesToMarkdown: Boolean = true,
  val convertFormulasToLatex: Boolean = true,
  val fencedCodeBlocks: Boolean = true,
  val includeFileSeparators: Boolean = true
)

data class MarkdownSourceItem(
  val id: String,
  val uri: Uri,
  val fileName: String,
  val fileSizeBytes: Long,
  val fileType: SourceFileType,
  val extractedRawText: String = "",
  val structuredMarkdown: String = "",
  val tokenEstimate: Int = 0,
  val isExtracting: Boolean = false,
  val error: String? = null
) {
  val displaySize: String
    get() = formatBytes(fileSizeBytes)

  companion object {
    fun formatBytes(bytes: Long): String {
      if (bytes <= 0) return "0 B"
      val kb = bytes / 1024.0
      val mb = kb / 1024.0
      return when {
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
        else -> "$bytes B"
      }
    }

    fun estimateTokens(text: String): Int {
      if (text.isBlank()) return 0
      // High-precision heuristic for LLM BPE tokenizers (~3.8 to 4 characters per token)
      return (text.length / 3.8).toInt().coerceAtLeast(1)
    }
  }
}

data class CompiledMarkdownResult(
  val title: String,
  val compiledMarkdown: String,
  val totalWordCount: Int,
  val estimatedTokens: Int,
  val sourceCount: Int,
  val file: File? = null
)
