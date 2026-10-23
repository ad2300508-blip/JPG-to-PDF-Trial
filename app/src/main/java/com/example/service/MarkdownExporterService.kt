package com.example.service

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.model.CompiledMarkdownResult
import com.example.model.LlmPromptWrapper
import com.example.model.MarkdownExportSettings
import com.example.model.MarkdownSourceItem
import com.example.model.SourceFileType
import com.example.util.DownloadUtils
import com.example.util.OcrUtils
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
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

  private const val TAG = "MarkdownExporterService"
  private const val LARGE_FILE_THRESHOLD_BYTES = 50 * 1024 * 1024L // 50 MB
  private const val MAX_PDF_PAGES_TO_EXTRACT = 250
  private const val MAX_TEXT_LINES_TO_EXTRACT = 15_000

  private var pdfBoxInitialized = false

  private fun ensurePdfBox(context: Context) {
    if (!pdfBoxInitialized) {
      try {
        PDFBoxResourceLoader.init(context.applicationContext)
        pdfBoxInitialized = true
      } catch (t: Throwable) {
        Log.e(TAG, "Failed to initialize PDFBoxResourceLoader", t)
      }
    }
  }

  /**
   * Extracts and structures document content into Markdown with streaming memory management,
   * chunked parsing, and protection against OutOfMemoryError on massive files (e.g. 150MB PDFs).
   */
  suspend fun extractSource(
    context: Context,
    uri: Uri,
    onProgress: (progress: Float, status: String) -> Unit = { _, _ -> }
  ): MarkdownSourceItem = withContext(Dispatchers.IO) {
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
    } catch (t: Throwable) {
      Log.w(TAG, "Could not resolve metadata for $uri: ${t.localizedMessage}")
    }

    val isLargeFile = fileSize >= LARGE_FILE_THRESHOLD_BYTES
    if (isLargeFile) {
      Log.i(TAG, "Ingesting large document: $fileName ($fileSize bytes). Using temp disk memory backing.")
      onProgress(0.05f, "Preparing large document (${fileSize / (1024 * 1024)} MB)...")
    }

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
    var parseError: String? = null

    try {
      when (fileType) {
        SourceFileType.PDF -> {
          // Open via ParcelFileDescriptor and streaming input with MemoryUsageSetting.setupTempFileOnly()
          // This keeps scratch buffers on disk instead of exhausting the JVM heap.
          val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("Cannot access file descriptor for $fileName")

          pfd.use {
            context.contentResolver.openInputStream(uri)?.use { stream ->
              val memorySetting = MemoryUsageSetting.setupTempFileOnly()
              PDDocument.load(stream, memorySetting).use { doc ->
                val totalPages = doc.numberOfPages
                val pagesToProcess = minOf(totalPages, MAX_PDF_PAGES_TO_EXTRACT)
                val stripper = PDFTextStripper()
                val sb = StringBuilder()
                sb.appendLine("> Document Length: $totalPages pages\n")

                for (page in 1..pagesToProcess) {
                  stripper.startPage = page
                  stripper.endPage = page
                  val pageText = stripper.getText(doc).trim()
                  if (pageText.isNotBlank()) {
                    sb.appendLine("### Page $page\n")
                    sb.appendLine(pageText)
                    sb.appendLine()
                  }

                  if (page % 5 == 0 || page == pagesToProcess) {
                    val progressRatio = page.toFloat() / pagesToProcess.toFloat()
                    onProgress(0.1f + (progressRatio * 0.85f), "Parsing PDF page $page of $pagesToProcess...")
                  }
                }

                if (totalPages > pagesToProcess) {
                  sb.appendLine("\n> ℹ️ Note: Parsed first $pagesToProcess of $totalPages pages to protect memory and LLM context limit.")
                }

                extractedRaw = sb.toString().trim()
                structuredMd = extractedRaw
              }
            }
          }
        }

        SourceFileType.IMAGE_OCR -> {
          onProgress(0.2f, "Scanning image OCR text blocks...")
          val ocrResult = OcrUtils.processImageOcr(context, uri, 0)
          extractedRaw = ocrResult.fullText.trim()
          structuredMd = structureOcrText(extractedRaw, ocrResult.blocks.map { it.text })
          onProgress(1f, "OCR extraction complete")
        }

        SourceFileType.TEXT_DOCUMENT, SourceFileType.CODE_FILE -> {
          onProgress(0.1f, "Streaming document content...")
          // Stream read line-by-line with MAX_TEXT_LINES_TO_EXTRACT guard
          val pfd = context.contentResolver.openFileDescriptor(uri, "r")
          pfd?.use {
            context.contentResolver.openInputStream(uri)?.use { stream ->
              BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                val sb = StringBuilder()
                var lineCount = 0
                var line = reader.readLine()
                while (line != null && lineCount < MAX_TEXT_LINES_TO_EXTRACT) {
                  sb.appendLine(line)
                  lineCount++
                  line = reader.readLine()
                }
                if (line != null) {
                  sb.appendLine("\n> ℹ️ Note: Text stream truncated at $MAX_TEXT_LINES_TO_EXTRACT lines for LLM prompt safety.")
                }
                extractedRaw = sb.toString().trim()
              }
            }
          }

          structuredMd = if (extension == "csv") {
            convertCsvToMarkdownTable(extractedRaw)
          } else if (fileType == SourceFileType.CODE_FILE) {
            "```$extension\n$extractedRaw\n```"
          } else {
            extractedRaw
          }
          onProgress(1f, "Text parsed successfully")
        }

        SourceFileType.AUDIO_NOTE -> {
          onProgress(0.5f, "Extracting audio track metadata...")
          val metadata = extractAudioMetadata(context, uri)
          extractedRaw = metadata
          structuredMd = metadata
          onProgress(1f, "Audio metadata processed")
        }
      }
    } catch (t: Throwable) {
      Log.e(TAG, "Fatal parsing error for $fileName: ${t.localizedMessage}", t)
      System.gc() // Clean up any garbage left by failed extraction
      val isOom = t is OutOfMemoryError || t.message?.contains("memory", ignoreCase = true) == true
      val displayError = if (isOom) {
        "Insufficient memory: File is too large to parse completely on-device"
      } else {
        t.localizedMessage ?: "Unsupported or corrupted document format"
      }
      parseError = displayError
      extractedRaw = "Failed to parse document: $displayError"
      structuredMd = "> ⚠️ Failed to parse document `$fileName`: $displayError"
    }

    val tokens = if (parseError != null) 0 else MarkdownSourceItem.estimateTokens(structuredMd)

    MarkdownSourceItem(
      id = UUID.randomUUID().toString(),
      uri = uri,
      fileName = fileName,
      fileSizeBytes = fileSize,
      fileType = fileType,
      extractedRawText = extractedRaw,
      structuredMarkdown = structuredMd,
      tokenEstimate = tokens,
      isExtracting = false,
      error = parseError
    )
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

    val maxTableRows = 1000 // Cap table rendering to avoid freezing UI
    val rowsToRender = minOf(lines.size, maxTableRows)

    for (i in 1 until rowsToRender) {
      val cols = parseCsvLine(lines[i])
      val paddedCols = (0 until headerCols.size).map { idx ->
        if (idx < cols.size) cols[idx] else ""
      }
      sb.appendLine("| " + paddedCols.joinToString(" | ") + " |")
    }

    if (lines.size > maxTableRows) {
      sb.appendLine("\n> ℹ️ Note: Table rendered first $maxTableRows of ${lines.size} rows.")
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
    } catch (_: Throwable) {
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
    val validItems = items.filter { it.error == null }
    val totalSources = validItems.size

    val combinedContent = validItems.joinToString("\n\n---\n\n") { item ->
      buildString {
        appendLine("## 📑 Source: `${item.fileName}` [Type: ${item.fileType.badgeText} | Size: ${item.displaySize}]")
        appendLine(item.structuredMarkdown)
      }
    }

    when (settings.llmWrapper) {
      LlmPromptWrapper.CLAUDE_XML -> {
        sb.appendLine("You are an expert AI research and synthesis assistant. Below is an authoritative multi-document context bundle containing $totalSources reference sources.\n")
        sb.appendLine("<documents>")
        validItems.forEachIndexed { index, item ->
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
        validItems.forEachIndexed { index, item ->
          sb.appendLine("### [Document ${index + 1}/$totalSources]: `${item.fileName}` (${item.fileType.badgeText})")
          sb.appendLine(item.structuredMarkdown)
          if (index < totalSources - 1) sb.appendLine("\n---\n")
        }
      }

      LlmPromptWrapper.GEMINI_BRIEF -> {
        sb.appendLine("# 🌐 Research Briefing Bundle: ${settings.bundleTitle}")
        sb.appendLine("> Compiled via OmniConvert AI Markdown Exporter | Total Sources: $totalSources\n")
        validItems.forEachIndexed { index, item ->
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
    val words = if (finalMarkdown.isBlank()) 0 else finalMarkdown.split(Regex("\\s+")).filter { it.isNotBlank() }.size
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
