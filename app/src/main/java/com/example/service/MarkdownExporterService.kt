package com.example.service

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.model.CompiledMarkdownResult
import com.example.model.LlmPromptWrapper
import com.example.model.MarkdownExportSettings
import com.example.model.MarkdownSourceItem
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class MarkdownExporterService(private val context: Context) {

    init {
        try {
            PDFBoxResourceLoader.init(context)
        } catch (t: Throwable) {
            Log.e("MarkdownExporterService", "Failed to init PDFBox: ${t.message}")
        }
    }

    suspend fun extractTextFromSource(
        uri: Uri,
        name: String,
        mimeType: String,
        sizeBytes: Long,
        onProgress: (Float, String) -> Unit
    ): MarkdownSourceItem = withContext(Dispatchers.IO) {
        val lowerName = name.lowercase()
        val isPdf = mimeType == "application/pdf" || lowerName.endsWith(".pdf")
        val isImage = mimeType.startsWith("image/") || lowerName.endsWith(".jpg") ||
                lowerName.endsWith(".png") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".webp")

        try {
            when {
                isPdf -> extractFromPdf(uri, name, mimeType, sizeBytes, onProgress)
                isImage -> extractFromImage(uri, name, mimeType, sizeBytes, onProgress)
                else -> extractFromText(uri, name, mimeType, sizeBytes, onProgress)
            }
        } catch (oom: OutOfMemoryError) {
            Log.e("MarkdownExporterService", "OOM while extracting $name", oom)
            System.gc()
            MarkdownSourceItem(
                uri = uri,
                name = name,
                sizeBytes = sizeBytes,
                mimeType = mimeType,
                extractedText = "",
                error = "Document too large for available RAM. Try splitting the file."
            )
        } catch (t: Throwable) {
            Log.e("MarkdownExporterService", "Error while extracting $name", t)
            MarkdownSourceItem(
                uri = uri,
                name = name,
                sizeBytes = sizeBytes,
                mimeType = mimeType,
                extractedText = "",
                error = "Extraction failed: ${t.localizedMessage ?: "Unknown format or read error"}"
            )
        }
    }

    private suspend fun extractFromPdf(
        uri: Uri,
        name: String,
        mimeType: String,
        sizeBytes: Long,
        onProgress: (Float, String) -> Unit
    ): MarkdownSourceItem {
        onProgress(0.05f, "Opening document stream...")
        val resolver = context.contentResolver

        val pfd = resolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("Could not access document descriptor")

        val document = pfd.use { descriptor ->
            resolver.openInputStream(uri)?.use { stream ->
                // Temp file backing avoids loading massive PDFs entirely into heap
                PDDocument.load(stream, MemoryUsageSetting.setupTempFileOnly())
            } ?: throw IllegalStateException("Could not read document stream")
        }

        return document.use { doc ->
            val totalPages = doc.numberOfPages
            val maxPages = minOf(totalPages, 250) // Safe upper cap
            val sb = java.lang.StringBuilder()
            sb.append("# ").append(name).append("\n\n")

            val stripper = PDFTextStripper()
            stripper.sortByPosition = true

            for (page in 1..maxPages) {
                stripper.startPage = page
                stripper.endPage = page
                val pageText = stripper.getText(doc).trim()

                if (pageText.isNotEmpty()) {
                    sb.append("## Page ").append(page).append("\n\n")
                    sb.append(pageText).append("\n\n")
                }

                val progress = page.toFloat() / maxPages.toFloat()
                onProgress(progress, "Parsing page $page of $maxPages (${(progress * 100).toInt()}%)")
            }

            if (totalPages > maxPages) {
                sb.append("\n> *Note: Truncated at $maxPages of $totalPages pages to preserve mobile memory.*\n")
            }

            MarkdownSourceItem(
                uri = uri,
                name = name,
                sizeBytes = sizeBytes,
                mimeType = mimeType,
                extractedText = sb.toString(),
                pageCount = totalPages
            )
        }
    }

    private suspend fun extractFromImage(
        uri: Uri,
        name: String,
        mimeType: String,
        sizeBytes: Long,
        onProgress: (Float, String) -> Unit
    ): MarkdownSourceItem {
        onProgress(0.15f, "Decoding image for OCR...")
        val resolver = context.contentResolver
        val inputStream = resolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open image")

        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
            ?: throw IllegalStateException("Failed to decode image bitmap")

        onProgress(0.5f, "Running on-device OCR...")
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val result = recognizer.process(image).await()
        bitmap.recycle()

        val rawText = result.text.trim()
        val structured = if (rawText.isEmpty()) {
            "# $name\n\n*(No text detected in image)*"
        } else {
            "# $name (OCR Extracted)\n\n$rawText\n"
        }

        onProgress(1.0f, "OCR extraction complete")
        return MarkdownSourceItem(
            uri = uri,
            name = name,
            sizeBytes = sizeBytes,
            mimeType = mimeType,
            extractedText = structured,
            pageCount = 1
        )
    }

    private suspend fun extractFromText(
        uri: Uri,
        name: String,
        mimeType: String,
        sizeBytes: Long,
        onProgress: (Float, String) -> Unit
    ): MarkdownSourceItem {
        onProgress(0.2f, "Reading text stream...")
        val resolver = context.contentResolver
        val inputStream = resolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open file stream")

        val reader = BufferedReader(InputStreamReader(inputStream))
        val sb = java.lang.StringBuilder()
        sb.append("# ").append(name).append("\n\n")

        var lineCount = 0
        val maxLines = 15000 // Guard against infinite files
        reader.useLines { lines ->
            for (line in lines) {
                lineCount++
                if (lineCount <= maxLines) {
                    sb.append(line).append("\n")
                } else {
                    sb.append("\n> *File truncated at $maxLines lines.*\n")
                    break
                }
            }
        }

        onProgress(1.0f, "Read $lineCount lines")
        return MarkdownSourceItem(
            uri = uri,
            name = name,
            sizeBytes = sizeBytes,
            mimeType = mimeType,
            extractedText = sb.toString(),
            pageCount = 1
        )
    }

    fun compileMarkdown(
        items: List<MarkdownSourceItem>,
        settings: MarkdownExportSettings
    ): CompiledMarkdownResult {
        val validItems = items.filter { it.extractedText.isNotEmpty() && it.error == null }
        val rawBuilder = java.lang.StringBuilder()

        if (settings.includeMetadataHeader) {
            rawBuilder.append("<!--\n")
            rawBuilder.append("OmniConvert Studio - Compiled AI Markdown Context\n")
            rawBuilder.append("Export Date: ").append(java.util.Date()).append("\n")
            rawBuilder.append("Total Documents: ").append(validItems.size).append("\n")
            rawBuilder.append("-->\n\n")
        }

        if (settings.includeTableOfContents && validItems.size > 1) {
            rawBuilder.append("## Table of Documents\n\n")
            validItems.forEachIndexed { idx, itm ->
                rawBuilder.append("${idx + 1}. **${itm.name}** (~${itm.estimatedTokens} tokens)\n")
            }
            rawBuilder.append("\n---\n\n")
        }

        validItems.forEachIndexed { index, item ->
            rawBuilder.append(item.extractedText.trim())
            if (index < validItems.size - 1) {
                rawBuilder.append("\n\n---\n\n")
            }
        }

        val rawMarkdown = rawBuilder.toString()
        val wrappedPrompt = wrapForLlm(validItems, rawMarkdown, settings.promptWrapper)
        val totalChars = wrappedPrompt.length
        val totalTokens = MarkdownSourceItem.estimateTokens(wrappedPrompt)

        return CompiledMarkdownResult(
            rawMarkdown = rawMarkdown,
            wrappedPrompt = wrappedPrompt,
            totalTokens = totalTokens,
            totalChars = totalChars,
            docCount = validItems.size
        )
    }

    private fun wrapForLlm(
        items: List<MarkdownSourceItem>,
        rawMarkdown: String,
        wrapper: LlmPromptWrapper
    ): String {
        return when (wrapper) {
            LlmPromptWrapper.CLAUDE_XML -> {
                val sb = java.lang.StringBuilder()
                sb.append("You are an expert AI assistant. Below is the reference material provided in structured XML format.\n\n")
                sb.append("<documents>\n")
                items.forEachIndexed { index, item ->
                    sb.append("  <document index=\"").append(index + 1).append("\">\n")
                    sb.append("    <source>").append(item.name).append("</source>\n")
                    sb.append("    <document_content>\n")
                    sb.append(item.extractedText.trim())
                    sb.append("\n    </document_content>\n")
                    sb.append("  </document>\n")
                }
                sb.append("</documents>\n\n")
                sb.append("Please analyze the documents provided above and answer the user query accurately using only the facts presented.")
                sb.toString()
            }
            LlmPromptWrapper.CHATGPT_SYSTEM -> {
                val sb = java.lang.StringBuilder()
                sb.append("You are a helpful and precise assistant. Use the following verified context files to complete the request.\n\n")
                items.forEachIndexed { index, item ->
                    sb.append("### Context File ").append(index + 1).append(": ").append(item.name).append("\n")
                    sb.append("```markdown\n")
                    sb.append(item.extractedText.trim()).append("\n")
                    sb.append("```\n\n")
                }
                sb.toString()
            }
            LlmPromptWrapper.GEMINI_BRIEF -> {
                val sb = java.lang.StringBuilder()
                sb.append("=== OMNICONVERT AI CONTEXT BRIEF ===\n")
                sb.append("DOCUMENTS INGESTED: ").append(items.size).append("\n\n")
                items.forEachIndexed { index, item ->
                    sb.append("========================================\n")
                    sb.append("DOCUMENT [").append(index + 1).append("]: ").append(item.name).append("\n")
                    sb.append("========================================\n\n")
                    sb.append(item.extractedText.trim()).append("\n\n")
                }
                sb.toString()
            }
            LlmPromptWrapper.RAW_MARKDOWN -> rawMarkdown
        }
    }

    suspend fun saveMarkdownFile(content: String, title: String): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val filename = "${title.ifBlank { "OmniMarkdown" }}_${System.currentTimeMillis()}.md"
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/markdown")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/OmniConvert/Markdown")
                }
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("Cannot create markdown MediaStore entry"))

            resolver.openOutputStream(uri)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }

            Result.success(uri)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
