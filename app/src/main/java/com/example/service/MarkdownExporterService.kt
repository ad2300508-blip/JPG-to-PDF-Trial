package com.example.service

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.CompiledMarkdownResult
import com.example.model.LlmPromptWrapper
import com.example.model.MarkdownExportSettings
import com.example.model.MarkdownSourceItem
import com.example.model.SourceFileType
import com.example.util.DownloadUtils
import com.example.util.OcrUtils
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.UUID

object MarkdownExporterService {

  private var pdfBoxInitialized = false

  private fun ensurePdfBox(context: Context) {
    if (!pdfBoxInitialized) {
      try {
        PDFBoxResourceLoader.init(context.applicationContext)
        pdfBoxInitialized = true
      } catch (_: Exception) { }
    }
  }

  suspend fun extractSource(context: Context, uri: Uri): MarkdownSourceItem = withContext(Dispatchers.IO) {
    ensurePdfBox(context)
    var fileName = "source_${System.currentTimeMillis()}"
    var fileSize = 0L

    try {
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
          if (nameIdx != -1) {
            val name = cursor.getString(nameIdx)
            if (!name.isNullOrBlank()) fileName = name
          }
          if (sizeIdx != -1 && !cursor.isNull(sizeIdx)) {
            fileSize = cursor.getLong(sizeIdx)
          }
        }
      }
    } catch (_: Exception) { }

    val extension = fileName.substringAfterLast(".", "").lowercase()
    val mimeType = context.contentResolver.getType(uri) ?: ""

    val fileType = when {
      extension == "pdf" || mimeType.contains("pdf") -> SourceFileType.PDF
      extension in listOf("jpg", "jpeg", "png", "webp", "bmp", "heic") || mimeType.startsWith("image/") -> SourceFileType.IMAGE_OCR
      extension in listOf("mp3", "wav", "m4a", "aac", "ogg", "flac") || mimeType.startsWith("audio/") -> SourceFileType.AUDIO_NOTE
      extension in listOf("kt", "java", "py", "js", "ts", "json", "xml", "html", "css", "cpp", "c", "rs", "sql", "sh") -> SourceFileType.CODE_FILE
      else -> SourceFileType.TEXT_DOCUMENT
    }

    var extractedRaw = ""
    var structuredMd = ""

    try {
      when (fileType) {
        SourceFileType.PDF -> {
          context.contentResolver.openInputStream(uri)?.use { stream ->
            PDDocument.load(stream).use { doc ->
              val stripper = PDFTextStripper()
              extractedRaw = stripper.getText(doc).trim()
              structuredMd = structurePdfText(extractedRaw, doc.numberOfPages)
            }
          }
        }

        SourceFileType.IMAGE_OCR -> {
          val ocrResult = OcrUtils.processImageOcr(context, uri, 0)
          extractedRaw = ocrResult.fullText.trim()
          structuredMd = structureOcrText(extractedRaw, ocrResult.blocks.map { it.text })
        }

        SourceFileType.TEXT_DOCUMENT, SourceFileType.CODE_FILE -> {
          context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
              extractedRaw = reader.readText().trim()
            }
          }
          structuredMd = if (extension == "csv") {
            convertCsvToMarkdownTable(extractedRaw)
          } else if (fileType == SourceFileType.CODE_FILE) {
            "```$extension\n$extractedRaw\n```"
          } else {
            extractedRaw
          }
        }

        SourceFileType.AUDIO_NOTE -> {
          val metadata = extractAudioMetadata(context, uri)
          extractedRaw = metadata
          structuredMd = metadata
        }
      }
    } catch (e: Exception) {
      extractedRaw = "Failed to parse content: ${e.localizedMessage}"
      structuredMd = "> ⚠️ Error parsing content from `$fileName`: ${e.localizedMessage}"
    }

    val tokens = MarkdownSourceItem.estimateTokens(structuredMd)

    MarkdownSourceItem(
      id = UUID.randomUUID().toString(),
      uri = uri,
      fileName = fileName,
      fileSizeBytes = fileSize,
      fileType = fileType,
      extractedRawText = extractedRaw,
      structuredMarkdown = structuredMd,
      tokenEstimate = tokens,
      isExtracting = false
    )
  }

  private fun structurePdfText(text: String, totalPages: Int): String {
    if (text.isBlank()) return "> *Empty or scanned PDF with no digital text stream. Use Image OCR if pages contain images.*"
    val lines = text.lines()
    val sb = StringBuilder()
    sb.appendLine("> Document Length: $totalPages pages\n")

    var inList = false
    for (rawLine in lines) {
      val line = rawLine.trim()
      if (line.isEmpty()) {
        if (inList) inList = false
        sb.appendLine()
        continue
      }

      // Check if line is a major chapter / section header (short, capitalized, or numbered)
      if (line.matches(Regex("^(Chapter|Section|Part|[0-9]+[.)])\\s+.*", RegexOption.IGNORE_CASE)) && line.length < 90) {
        sb.appendLine("\n### $line\n")
        inList = false
      } else if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*")) {
        val cleanBullet = line.replaceFirst(Regex("^[•\\-*]\\s*"), "")
        sb.appendLine("- $cleanBullet")
        inList = true
      } else {
        sb.appendLine(line)
      }
    }
    return sb.toString().trim()
  }

  private fun structureOcrText(fullText: String, blocks: List<String>): String {
    if (fullText.isBlank()) return "> *No printed or handwritten text recognized in image.*"
    val sb = StringBuilder()
    sb.appendLine("> OCR Scan Result: ${blocks.size} structural text blocks detected\n")

    blocks.forEachIndexed { idx, block ->
      val trimmed = block.trim()
      if (trimmed.isNotBlank()) {
        if (idx == 0 && trimmed.length < 80) {
          sb.appendLine("### $trimmed\n")
        } else {
          sb.appendLine(trimmed)
          sb.appendLine()
        }
      }
    }
    return sb.toString().trim()
  }

  private fun convertCsvToMarkdownTable(csvContent: String): String {
    val lines = csvContent.lines().map { it.trim() }.filter { it.isNotEmpty() }
    if (lines.isEmpty()) return csvContent

    val sb = StringBuilder()
    val headerCols = parseCsvLine(lines[0])
    sb.appendLine("| " + headerCols.joinToString(" | ") + " |")
    sb.appendLine("| " + headerCols.map { "---" }.joinToString(" | ") + " |")

    for (i in 1 until lines.size) {
      val cols = parseCsvLine(lines[i])
      val paddedCols = (0 until headerCols.size).map { idx ->
        if (idx < cols.size) cols[idx] else ""
      }
      sb.appendLine("| " + paddedCols.joinToString(" | ") + " |")
    }
    return sb.toString()
  }

  private fun parseCsvLine(line: String): List<String> {
    val cols = mutableListOf<String>()
    val cur = StringBuilder()
    var inQuotes = false
    for (ch in line) {
      if (ch == '\"') {
        inQuotes = !inQuotes
      } else if (ch == ',' && !inQuotes) {
        cols.add(cur.toString().trim())
        cur.clear()
      } else {
        cur.append(ch)
      }
    }
    cols.add(cur.toString().trim())
    return cols
  }

  private fun extractAudioMetadata(context: Context, uri: Uri): String {
    val retriever = MediaMetadataRetriever()
    return try {
      val pfd = context.contentResolver.openFileDescriptor(uri, "r")
      pfd?.use {
        retriever.setDataSource(it.fileDescriptor)
      }
      val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Audio Recording"
      val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
      val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
      val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0

      val totalSec = durationMs / 1000
      val min = totalSec / 60
      val sec = totalSec % 60

      """
      ### 🎙️ Audio Track Context: $title
      - **Artist / Speaker**: $artist
      - **Duration**: ${String.format("%02d:%02d", min, sec)}
      - **Bitrate**: ${bitrate / 1000} kbps
      - **Note**: Audio file attached as reference source.
      """.trimIndent()
    } catch (_: Exception) {
      "> 🎙️ Audio Recording: (Metadata not available)"
    } finally {
      retriever.release()
    }
  }

  fun compileBundle(
    items: List<MarkdownSourceItem>,
    settings: MarkdownExportSettings
  ): CompiledMarkdownResult {
    val sb = StringBuilder()
    val totalSources = items.size

    val combinedContent = items.joinToString("\n\n---\n\n") { item ->
      buildString {
        appendLine("## 📑 Source: `${item.fileName}` [Type: ${item.fileType.badgeText} | Size: ${item.displaySize}]")
        appendLine(item.structuredMarkdown)
      }
    }

    when (settings.llmWrapper) {
      LlmPromptWrapper.CLAUDE_XML -> {
        sb.appendLine("You are an expert AI research and synthesis assistant. Below is an authoritative multi-document context bundle containing $totalSources reference sources.\n")
        sb.appendLine("<documents>")
        items.forEachIndexed { index, item ->
          sb.appendLine("<document index=\"${index + 1}\">")
          sb.appendLine("<source>${item.fileName}</source>")
          sb.appendLine("<type>${item.fileType.badgeText}</type>")
          sb.appendLine("<document_content>")
          sb.appendLine(item.structuredMarkdown)
          sb.appendLine("</document_content>")
          sb.appendLine("</document>")
        }
        sb.appendLine("</documents>\n")
        sb.appendLine("Please answer all questions by analyzing this context, maintaining high factual rigor, and citing `<document index=\"N\">` when referencing specific claims.")
      }

      LlmPromptWrapper.CHATGPT_SYSTEM -> {
        sb.appendLine("# System Prompt: Comprehensive Context Intelligence\n")
        sb.appendLine("You are an analytical assistant. Review the following $totalSources attached source documents to synthesize insights and address inquiries:\n")
        items.forEachIndexed { index, item ->
          sb.appendLine("### [Document ${index + 1}/$totalSources]: `${item.fileName}` (${item.fileType.badgeText})")
          sb.appendLine(item.structuredMarkdown)
          if (index < totalSources - 1) sb.appendLine("\n---\n")
        }
      }

      LlmPromptWrapper.GEMINI_BRIEF -> {
        sb.appendLine("# 🌐 Research Briefing Bundle: ${settings.bundleTitle}")
        sb.appendLine("> Compiled via OmniConvert AI Markdown Exporter | Total Sources: $totalSources\n")
        items.forEachIndexed { index, item ->
          sb.appendLine("## 📌 Reference Item ${index + 1}: `${item.fileName}`")
          sb.appendLine("- **Class**: ${item.fileType.badgeText}")
          sb.appendLine("- **Footprint**: ${item.displaySize}")
          sb.appendLine()
          sb.appendLine(item.structuredMarkdown)
          if (index < totalSources - 1) sb.appendLine("\n---\n")
        }
      }

      LlmPromptWrapper.RAW_MARKDOWN -> {
        if (settings.includeMetadataHeader) {
          sb.appendLine("# 📄 ${settings.bundleTitle}")
          sb.appendLine("> OmniConvert Clean Markdown Export | Sources: $totalSources\n")
          sb.appendLine("---\n")
        }
        sb.append(combinedContent)
      }
    }

    val finalMarkdown = sb.toString().trim()
    val words = finalMarkdown.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    val tokens = MarkdownSourceItem.estimateTokens(finalMarkdown)

    return CompiledMarkdownResult(
      title = settings.bundleTitle,
      compiledMarkdown = finalMarkdown,
      totalWordCount = words,
      estimatedTokens = tokens,
      sourceCount = totalSources
    )
  }

  suspend fun exportMarkdownFile(
    context: Context,
    result: CompiledMarkdownResult
  ): Result<DownloadUtils.DownloadResult> = withContext(Dispatchers.IO) {
    try {
      val baseName = result.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
      val fileName = if (baseName.endsWith(".md", ignoreCase = true)) baseName else "$baseName.md"
      val tempFile = File(context.cacheDir, "omni_${System.currentTimeMillis()}_$fileName")

      FileOutputStream(tempFile).use { out ->
        out.write(result.compiledMarkdown.toByteArray(Charsets.UTF_8))
        out.flush()
      }

      DownloadUtils.saveFileToDownloads(
        context = context,
        sourceFile = tempFile,
        desiredFileName = fileName,
        mimeType = "text/markdown",
        subfolder = "OmniConvert/Markdown"
      )
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
