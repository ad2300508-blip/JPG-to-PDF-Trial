package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import android.util.Xml
import com.example.model.CompiledMarkdownResult
import com.example.model.LlmPromptWrapper
import com.example.model.MarkdownExportSettings
import com.example.model.MarkdownSourceItem
import com.example.util.BitmapLoader
import com.example.util.FileNames
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.util.resultOf
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream
import kotlin.coroutines.coroutineContext

class MarkdownExporterService(private val context: Context) {

    init {
        runCatching { PDFBoxResourceLoader.init(context) }
    }

    suspend fun extractTextFromSource(
        uri: Uri,
        name: String,
        mimeType: String,
        sizeBytes: Long,
        parseAllPages: Boolean = false,
        forceOcr: Boolean = false,
        options: PdfMarkdownOptions = PdfMarkdownOptions(),
        onProgress: (Float, String) -> Unit
    ): MarkdownSourceItem = withContext(Dispatchers.IO) {
        val lowerName = name.lowercase()
        val isPdf = mimeType == "application/pdf" || lowerName.endsWith(".pdf")
        val isDocx = mimeType == DOCX_MIME || lowerName.endsWith(".docx")
        val isImage = mimeType.startsWith("image/") || IMAGE_EXTENSIONS.any { lowerName.endsWith(it) }

        try {
            when {
                isPdf -> extractFromPdf(
                    uri = uri,
                    name = name,
                    mimeType = mimeType,
                    sizeBytes = sizeBytes,
                    maxPagesToExtract = if (parseAllPages) Int.MAX_VALUE else PREVIEW_PAGES,
                    forceOcr = forceOcr,
                    options = options,
                    onProgress = onProgress
                )
                isDocx -> extractFromDocx(uri, name, mimeType, sizeBytes, onProgress)
                isImage -> extractFromImage(uri, name, mimeType, sizeBytes, onProgress)
                else -> extractFromText(uri, name, mimeType, sizeBytes, onProgress)
            }
        } catch (c: CancellationException) {
            throw c
        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "OOM while extracting $name", oom)
            System.gc()
            MarkdownSourceItem(
                uri = uri, name = name, sizeBytes = sizeBytes, mimeType = mimeType,
                error = "Documento troppo grande per la memoria disponibile: prova a dividerlo"
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Error while extracting $name", t)
            MarkdownSourceItem(
                uri = uri, name = name, sizeBytes = sizeBytes, mimeType = mimeType,
                error = "Estrazione non riuscita: ${t.localizedMessage ?: "formato non supportato"}"
            )
        }
    }

    suspend fun extractRemainingPages(
        item: MarkdownSourceItem,
        forceOcr: Boolean = false,
        options: PdfMarkdownOptions = PdfMarkdownOptions(),
        onProgress: (Float, String) -> Unit
    ): MarkdownSourceItem = withContext(Dispatchers.IO) {
        val uri = item.uri ?: return@withContext item
        try {
            extractFromPdf(
                uri = uri,
                name = item.name,
                mimeType = item.mimeType,
                sizeBytes = item.sizeBytes,
                startPage = item.parsedPages + 1,
                maxPagesToExtract = item.pageCount,
                existingText = item.extractedText.substringBefore(PREVIEW_NOTICE_MARKER).trimEnd() + "\n\n",
                forceOcr = forceOcr,
                options = options,
                onProgress = onProgress
            ).copy(id = item.id)
        } catch (c: CancellationException) {
            throw c
        } catch (oom: OutOfMemoryError) {
            System.gc()
            item.copy(error = "Memoria esaurita: le pagine già estratte sono state mantenute")
        } catch (t: Throwable) {
            item.copy(error = "Impossibile estrarre le pagine rimanenti: ${t.localizedMessage}")
        }
    }

    /**
     * Uses the PDF's own text layer when a page has one (fast and exact) and falls back to on-device
     * OCR only for scanned pages.
     */
    private suspend fun extractFromPdf(
        uri: Uri,
        name: String,
        mimeType: String,
        sizeBytes: Long,
        startPage: Int = 1,
        maxPagesToExtract: Int = PREVIEW_PAGES,
        existingText: String = "",
        forceOcr: Boolean,
        options: PdfMarkdownOptions = PdfMarkdownOptions(),
        onProgress: (Float, String) -> Unit
    ): MarkdownSourceItem {
        onProgress(0.02f, "Apro il PDF…")
        val resolver = context.contentResolver
        val pfd = resolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("Impossibile accedere a $name")

        val textDocument: PDDocument? = if (forceOcr) null else runCatching {
            PDFBoxResourceLoader.init(context)
            resolver.openInputStream(uri)?.use { PDDocument.load(it, MemoryUsageSetting.setupTempFileOnly()) }
        }.getOrNull()
        val stripper = textDocument?.let { LineCollectingStripper() }
        // Text pages keep their styled lines until the end, so running headers/footers and the body
        // font size can be computed over the whole range before writing Markdown.
        val pageContents = mutableListOf<Pair<Int, Any>>()

        var recognizer: TextRecognizer? = null
        val sb = StringBuilder(existingText)
        if (sb.isEmpty()) sb.append("# ").append(FileNames.baseName(name)).append("\n\n")

        var totalPages = 0
        var pagesParsed = startPage - 1
        var ocrPages = 0

        try {
            pfd.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    totalPages = renderer.pageCount
                    val endPage = minOf(totalPages, maxPagesToExtract)
                    val pagesToProcess = (endPage - startPage + 1).coerceAtLeast(1)

                    for (pageIndex in (startPage - 1) until endPage) {
                        coroutineContext.ensureActive()
                        val pageNumber = pageIndex + 1
                        val fraction = (pageNumber - startPage + 1).toFloat() / pagesToProcess
                        onProgress(fraction, "Pagina $pageNumber di $totalPages")

                        val lines = stripper?.let { s -> textDocument?.let { d -> runCatching { s.collect(d, pageNumber) }.getOrNull() } }.orEmpty()

                        val pageContent: Any = if (lines.sumOf { it.charCount } >= MIN_TEXT_LAYER_CHARS) {
                            lines
                        } else {
                            ocrPages++
                            val ocr = recognizer ?: TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).also { recognizer = it }
                            renderer.openPage(pageIndex).use { page ->
                                val scale = OCR_RENDER_SIDE.toFloat() / maxOf(page.width, page.height)
                                val width = (page.width * scale).toInt().coerceAtLeast(1)
                                val height = (page.height * scale).toInt().coerceAtLeast(1)
                                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                bitmap.eraseColor(android.graphics.Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                try {
                                    val visionText = ocr.process(InputImage.fromBitmap(bitmap, 0)).await()
                                    OcrUtils.processPageBlocks(visionText, width, height)
                                } finally {
                                    bitmap.recycle()
                                }
                            }
                        }

                        pageContents += pageNumber to pageContent
                        pagesParsed = pageNumber
                    }

                    onProgress(1f, "Ricostruisco titoli, elenchi e tabelle…")
                    @Suppress("UNCHECKED_CAST")
                    val textPages = pageContents.mapNotNull { it.second as? List<PdfLine> }
                    val body = PdfMarkdownBuilder.bodySize(textPages)
                    val repeated = if (options.cleanMargins) PdfMarkdownBuilder.repeatedMargins(textPages) else emptySet()
                    for ((pageNumber, content) in pageContents) {
                        @Suppress("UNCHECKED_CAST")
                        val pageMarkdown = when (content) {
                            is String -> content
                            else -> PdfMarkdownBuilder.render(content as List<PdfLine>, body, repeated, options)
                        }
                        if (pageMarkdown.isBlank()) continue
                        when (options.pageMarkers) {
                            PageMarkerStyle.COMMENT -> sb.append("<!-- Pagina ").append(pageNumber).append(" -->\n\n")
                            PageMarkerStyle.HEADING -> sb.append("## Pagina ").append(pageNumber).append("\n\n")
                            PageMarkerStyle.NONE -> Unit
                        }
                        sb.append(pageMarkdown.trim()).append("\n\n")
                    }

                    if (totalPages > endPage) {
                        sb.append("\n").append(PREVIEW_NOTICE_MARKER)
                            .append(" delle prime $endPage pagine su $totalPages. Usa \"Estrai tutte le pagine\" per il resto.*\n")
                    }
                }
            }
        } finally {
            runCatching { textDocument?.close() }
            runCatching { recognizer?.close() }
        }

        Log.d(TAG, "$name: $pagesParsed pages, $ocrPages via OCR")
        return MarkdownSourceItem(
            uri = uri,
            name = name,
            sizeBytes = sizeBytes,
            mimeType = mimeType,
            extractedText = sb.toString(),
            pageCount = totalPages,
            parsedPages = pagesParsed,
            hasMorePages = totalPages > pagesParsed
        )
    }

    private suspend fun extractFromImage(
        uri: Uri,
        name: String,
        mimeType: String,
        sizeBytes: Long,
        onProgress: (Float, String) -> Unit
    ): MarkdownSourceItem {
        onProgress(0.15f, "Preparo l'immagine…")
        // EXIF-aware decoding: a sideways phone photo would otherwise be read as gibberish.
        val bitmap = BitmapLoader.decode(context, uri, OCR_IMAGE_SIDE)
            ?: throw IllegalStateException("Impossibile leggere l'immagine")

        onProgress(0.5f, "Riconoscimento del testo…")
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val structured = try {
            val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
            OcrUtils.processPageBlocks(result, bitmap.width, bitmap.height)
        } finally {
            bitmap.recycle()
            recognizer.close()
        }

        val title = FileNames.baseName(name)
        val text = if (structured.isEmpty()) {
            "# $title\n\n*(Nessun testo riconosciuto nell'immagine)*"
        } else {
            "# $title\n\n$structured\n"
        }
        onProgress(1f, "Testo riconosciuto")
        return MarkdownSourceItem(
            uri = uri, name = name, sizeBytes = sizeBytes, mimeType = mimeType,
            extractedText = text, pageCount = 1, parsedPages = 1, hasMorePages = false
        )
    }

    /** Reads word/document.xml from a .docx: headings, bullet lists, bold/italic and tables. */
    private suspend fun extractFromDocx(
        uri: Uri,
        name: String,
        mimeType: String,
        sizeBytes: Long,
        onProgress: (Float, String) -> Unit
    ): MarkdownSourceItem {
        onProgress(0.2f, "Leggo il documento Word…")
        val markdown = context.contentResolver.openInputStream(uri)?.use { raw ->
            ZipInputStream(raw.buffered()).use { zip ->
                var result: String? = null
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.name == "word/document.xml") {
                        result = parseDocxXml(zip)
                        break
                    }
                }
                result
            }
        } ?: throw IllegalStateException("File Word non valido (manca word/document.xml)")

        coroutineContext.ensureActive()
        onProgress(1f, "Documento Word letto")
        return MarkdownSourceItem(
            uri = uri, name = name, sizeBytes = sizeBytes, mimeType = mimeType,
            extractedText = "# ${FileNames.baseName(name)}\n\n${markdown.trim()}\n",
            pageCount = 1, parsedPages = 1, hasMorePages = false
        )
    }

    private fun parseDocxXml(stream: java.io.InputStream): String {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, "UTF-8")

        val out = StringBuilder()
        val paragraph = StringBuilder()
        var headingLevel = 0
        var isListItem = false
        var bold = false
        var italic = false
        var inTable = false
        var tableRow = mutableListOf<String>()
        var tableRowsWritten = 0
        var cellText = StringBuilder()
        var inCell = false

        fun flushParagraph() {
            val text = paragraph.toString().trim()
            paragraph.setLength(0)
            if (inCell) {
                if (text.isNotEmpty()) {
                    if (cellText.isNotEmpty()) cellText.append(' ')
                    cellText.append(text)
                }
            } else if (text.isNotEmpty()) {
                when {
                    headingLevel in 1..6 -> out.append("#".repeat(headingLevel + 1)).append(' ').append(text.replace("**", "")).append("\n\n")
                    isListItem -> out.append("- ").append(text).append('\n')
                    else -> {
                        if (out.isNotEmpty() && out.last() == '\n' && out.length > 1 && out[out.length - 2] != '\n') out.append('\n')
                        out.append(text).append("\n\n")
                    }
                }
            }
            headingLevel = 0
            isListItem = false
        }

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            val tag = parser.name
            when (event) {
                XmlPullParser.START_TAG -> when (tag) {
                    "w:pStyle" -> {
                        val value = parser.getAttributeValue(null, "w:val").orEmpty().lowercase()
                        HEADING_REGEX.find(value)?.groupValues?.get(1)?.toIntOrNull()?.let { headingLevel = it }
                        if (value == "title" || value == "titolo") headingLevel = 1
                        if (value.contains("list")) isListItem = true
                    }
                    "w:numPr" -> isListItem = true
                    "w:b" -> bold = parser.getAttributeValue(null, "w:val") != "0"
                    "w:i" -> italic = parser.getAttributeValue(null, "w:val") != "0"
                    "w:r" -> { bold = false; italic = false }
                    "w:t" -> {
                        val text = parser.nextText()
                        if (text.isNotEmpty()) {
                            val decorated = when {
                                bold && italic && text.isNotBlank() -> "***${text}***"
                                bold && text.isNotBlank() -> "**${text}**"
                                italic && text.isNotBlank() -> "*${text}*"
                                else -> text
                            }
                            paragraph.append(decorated)
                        }
                    }
                    "w:tab" -> paragraph.append(' ')
                    "w:br" -> paragraph.append(' ')
                    "w:tbl" -> { inTable = true; tableRowsWritten = 0; if (out.isNotEmpty() && !out.endsWith("\n\n")) out.append('\n') }
                    "w:tr" -> tableRow = mutableListOf()
                    "w:tc" -> { inCell = true; cellText = StringBuilder() }
                }
                XmlPullParser.END_TAG -> when (tag) {
                    "w:p" -> flushParagraph()
                    "w:tc" -> { inCell = false; tableRow.add(cellText.toString().replace("|", "\\|")) }
                    "w:tr" -> if (inTable && tableRow.isNotEmpty()) {
                        out.append("| ").append(tableRow.joinToString(" | ")).append(" |\n")
                        if (tableRowsWritten == 0) out.append("|").append(" --- |".repeat(tableRow.size)).append('\n')
                        tableRowsWritten++
                    }
                    "w:tbl" -> { inTable = false; out.append('\n') }
                }
            }
            event = parser.next()
        }
        flushParagraph()
        return out.toString().replace(Regex("\\*\\*\\*\\*"), "").replace(Regex("\n{3,}"), "\n\n")
    }

    private suspend fun extractFromText(
        uri: Uri,
        name: String,
        mimeType: String,
        sizeBytes: Long,
        onProgress: (Float, String) -> Unit
    ): MarkdownSourceItem {
        onProgress(0.2f, "Leggo il testo…")
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Impossibile aprire il file")

        val sb = StringBuilder()
        val isMarkdown = name.lowercase().endsWith(".md")
        if (!isMarkdown) sb.append("# ").append(FileNames.baseName(name)).append("\n\n")

        var lineCount = 0
        var binaryChars = 0
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).useLines { lines ->
            for (line in lines) {
                coroutineContext.ensureActive()
                lineCount++
                binaryChars += line.count { it == '\u0000' || it == '\uFFFD' }
                if (lineCount <= MAX_TEXT_LINES) {
                    sb.append(line).append('\n')
                } else {
                    sb.append("\n> *File troncato a $MAX_TEXT_LINES righe.*\n")
                    break
                }
            }
        }
        if (binaryChars > 50) throw IllegalStateException("Formato non supportato: usa PDF, DOCX, TXT, MD o immagini")

        onProgress(1f, "$lineCount righe lette")
        return MarkdownSourceItem(
            uri = uri, name = name, sizeBytes = sizeBytes, mimeType = mimeType,
            extractedText = sb.toString(), pageCount = 1, parsedPages = 1, hasMorePages = false
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
            rawBuilder.append("Morph - Contesto Markdown per AI\n")
            rawBuilder.append("Data di esportazione: ").append(java.util.Date()).append("\n")
            rawBuilder.append("Documenti: ").append(validItems.size).append("\n")
            rawBuilder.append("-->\n\n")
        }

        if (settings.includeTableOfContents && validItems.size > 1) {
            rawBuilder.append("## Indice dei documenti\n\n")
            validItems.forEachIndexed { idx, itm ->
                rawBuilder.append("${idx + 1}. **${itm.name}** (~${itm.estimatedTokens} token)\n")
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
        resultOf {
            val fileName = FileNames.stamped(title, "Markdown", "md")
            OutputStorage.save(context, fileName, "text/markdown", OutputFolder.MARKDOWN) { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }
        }
    }

    private companion object {
        const val TAG = "MarkdownExporter"
        const val DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        const val PREVIEW_PAGES = 15
        const val PREVIEW_NOTICE_MARKER = "> *Anteprima"
        const val MIN_TEXT_LAYER_CHARS = 40
        const val OCR_RENDER_SIDE = 2200
        const val OCR_IMAGE_SIDE = 2400
        const val MAX_TEXT_LINES = 15000
        val IMAGE_EXTENSIONS = listOf(".jpg", ".jpeg", ".png", ".webp", ".heic", ".heif", ".bmp")
        val HEADING_REGEX = Regex("(?:heading|titolo|berschrift)\\s*(\\d)")
        val BULLET_REGEX = Regex("^[•●▪◦‣∙·]\\s*.+")
    }
}
