package com.example.service

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class MarkdownExporterService(private val context: Context) {

    suspend fun extractTextFromSource(
        uri: Uri,
        name: String,
        mimeType: String,
        sizeBytes: Long,
        parseAllPages: Boolean = false,
        onProgress: (Float, String) -> Unit
    ): MarkdownSourceItem = withContext(Dispatchers.IO) {
        val lowerName = name.lowercase()
        val isPdf = mimeType == "application/pdf" || lowerName.endsWith(".pdf")
        val isImage = mimeType.startsWith("image/") || lowerName.endsWith(".jpg") ||
                lowerName.endsWith(".png") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".webp")

        try {
            when {
                isPdf -> extractFromPdfWithRenderer(
                    uri = uri,
                    name = name,
                    mimeType = mimeType,
                    sizeBytes = sizeBytes,
                    maxPagesToExtract = if (parseAllPages) Int.MAX_VALUE else 15,
                    onProgress = onProgress
                )
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
                error = "Document too large for available RAM. Try downsampling or splitting the file."
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

    suspend fun extractRemainingPages(
        item: MarkdownSourceItem,
        onProgress: (Float, String) -> Unit
    ): MarkdownSourceItem = withContext(Dispatchers.IO) {
        val uri = item.uri ?: return@withContext item
        try {
            extractFromPdfWithRenderer(
                uri = uri,
                name = item.name,
                mimeType = item.mimeType,
                sizeBytes = item.sizeBytes,
                startPage = item.parsedPages + 1,
                maxPagesToExtract = item.pageCount,
                existingText = item.extractedText,
                onProgress = onProgress
            )
        } catch (oom: OutOfMemoryError) {
            Log.e("MarkdownExporterService", "OOM while parsing remaining pages for ${item.name}", oom)
            System.gc()
            item.copy(error = "Ran out of memory parsing remaining pages. Current pages preserved.")
        } catch (t: Throwable) {
            item.copy(error = "Failed to parse remaining pages: ${t.localizedMessage}")
        }
    }

    private suspend fun extractFromPdfWithRenderer(
        uri: Uri,
        name: String,
        mimeType: String,
        sizeBytes: Long,
        startPage: Int = 1,
        maxPagesToExtract: Int = 15,
        existingText: String = "",
        onProgress: (Float, String) -> Unit
    ): MarkdownSourceItem {
        onProgress(0.02f, "Opening PDF renderer descriptor...")
        val resolver = context.contentResolver

        val pfd = resolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("Could not access file descriptor for $name")

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val sb = StringBuilder(existingText)
        if (sb.isEmpty()) {
            sb.append("# ").append(name).append("\n\n")
        }

        var totalPages = 0
        var pagesParsed = startPage - 1

        pfd.use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                totalPages = renderer.pageCount
                val endPage = minOf(totalPages, maxPagesToExtract)
                val pagesToProcess = (endPage - startPage + 1).coerceAtLeast(1)

                for (pageIndex in (startPage - 1) until endPage) {
                    val displayPageNum = pageIndex + 1
                    val currentProgress = (displayPageNum - startPage + 1).toFloat() / pagesToProcess.toFloat()
                    onProgress(currentProgress, "Extracting page $displayPageNum of $totalPages (${(currentProgress * 100).toInt()}%)...")

                    val page = renderer.openPage(pageIndex)

                    // Render at high resolution 2200px max dimension for sharp OCR reading
                    val maxDim = 2200
                    val scale = maxDim.toFloat() / maxOf(page.width, page.height).toFloat()
                    val renderWidth = (page.width * scale).toInt().coerceAtLeast(1)
                    val renderHeight = (page.height * scale).toInt().coerceAtLeast(1)

                    val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE) // White background canvas for clear contrast
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    try {
                        val inputImage = InputImage.fromBitmap(bitmap, 0)
                        val visionText = recognizer.process(inputImage).await()
                        
                        // Multi-column and dual-page spatial sorting with hyphenation repair
                        val pageMarkdown = OcrUtils.processPageBlocks(visionText, renderWidth, renderHeight)

                        if (pageMarkdown.isNotEmpty()) {
                            sb.append("## Page ").append(displayPageNum).append("\n\n")
                            sb.append(pageMarkdown).append("\n\n")
                        }
                    } finally {
                        // Immediately recycle bitmap before next iteration to keep RAM footprint low
                        bitmap.recycle()
                    }

                    pagesParsed = displayPageNum
                }

                if (totalPages > endPage) {
                    sb.append("\n> *Preview limited to first $endPage of $totalPages pages. Use 'Parse Remaining Pages' or toggle 'Full Document Extraction' to extract all.*\n")
                }
            }
        }

        val hasMore = totalPages > pagesParsed

        return MarkdownSourceItem(
            uri = uri,
            name = name,
            sizeBytes = sizeBytes,
            mimeType = mimeType,
            extractedText = sb.toString(),
            pageCount = totalPages,
            parsedPages = pagesParsed,
            hasMorePages = hasMore
        )
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

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        // Downsample large images to max 1920px
        val maxDim = 1920
        var sampleSize = 1
        while (options.outWidth / sampleSize > maxDim || options.outHeight / sampleSize > maxDim) {
            sampleSize *= 2
        }

        val stream2 = resolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot re-open image stream")
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = BitmapFactory.decodeStream(stream2, null, decodeOpts)
        stream2.close()
        if (bitmap == null) {
            throw IllegalStateException("Failed to decode image bitmap")
        }

        onProgress(0.5f, "Running on-device OCR...")
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val result = recognizer.process(image).await()
        val structuredText = OcrUtils.processPageBlocks(result, bitmap.width, bitmap.height)
        bitmap.recycle()

        val structured = if (structuredText.isEmpty()) {
            "# $name\n\n*(No text detected in image)*"
        } else {
            "# $name (OCR Extracted)\n\n$structuredText\n"
        }

        onProgress(1.0f, "OCR extraction complete")
        return MarkdownSourceItem(
            uri = uri,
            name = name,
            sizeBytes = sizeBytes,
            mimeType = mimeType,
            extractedText = structured,
            pageCount = 1,
            parsedPages = 1,
            hasMorePages = false
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
        val sb = StringBuilder()
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
            pageCount = 1,
            parsedPages = 1,
            hasMorePages = false
        )
    }

    fun compileMarkdown(
        items: List<MarkdownSourceItem>,
        settings: MarkdownExportSettings
    ): CompiledMarkdownResult {
        val validItems = items.filter { it.extractedText.isNotEmpty() && it.error == null }
        val rawBuilder = StringBuilder()

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
                val sb = StringBuilder()
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
                val sb = StringBuilder()
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
                val sb = StringBuilder()
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
