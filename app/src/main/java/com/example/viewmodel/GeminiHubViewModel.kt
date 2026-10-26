package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.GeminiGenerationState
import com.example.model.GeminiPromptPreset
import com.example.model.GeminiTargetModel
import com.example.model.ModelQuotaUsage
import com.example.service.GeminiApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GeminiHubViewModel(application: Application) : AndroidViewModel(application) {

    private val geminiService = GeminiApiService(application.applicationContext)

    private val _selectedModel = MutableStateFlow(GeminiTargetModel.GEMINI_3_8_FLASH_LITE)
    val selectedModel: StateFlow<GeminiTargetModel> = _selectedModel.asStateFlow()

    private val _smartRouting = MutableStateFlow(true)
    val smartRouting: StateFlow<Boolean> = _smartRouting.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _contextDocument = MutableStateFlow<String?>(null)
    val contextDocument: StateFlow<String?> = _contextDocument.asStateFlow()

    private val _audioAttachmentUri = MutableStateFlow<Uri?>(null)
    val audioAttachmentUri: StateFlow<Uri?> = _audioAttachmentUri.asStateFlow()

    private val _audioMimeType = MutableStateFlow<String?>("audio/mp4")
    val audioMimeType: StateFlow<String?> = _audioMimeType.asStateFlow()

    private val _imageAttachmentUri = MutableStateFlow<Uri?>(null)
    val imageAttachmentUri: StateFlow<Uri?> = _imageAttachmentUri.asStateFlow()

    private val _generationState = MutableStateFlow(GeminiGenerationState())
    val generationState: StateFlow<GeminiGenerationState> = _generationState.asStateFlow()

    private val _quotaUsages = MutableStateFlow<List<ModelQuotaUsage>>(emptyList())
    val quotaUsages: StateFlow<List<ModelQuotaUsage>> = _quotaUsages.asStateFlow()

    private val _apiKeyStatus = MutableStateFlow("UNKNOWN") // "CONNECTED", "ERROR", "UNCONFIGURED", "TESTING"
    val apiKeyStatus: StateFlow<String> = _apiKeyStatus.asStateFlow()

    private val _connectionLatency = MutableStateFlow<Long?>(null)
    val connectionLatency: StateFlow<Long?> = _connectionLatency.asStateFlow()

    private val _maskedApiKey = MutableStateFlow(geminiService.getMaskedApiKey())
    val maskedApiKey: StateFlow<String> = _maskedApiKey.asStateFlow()

    private val _apiKeyInput = MutableStateFlow("")
    val apiKeyInput: StateFlow<String> = _apiKeyInput.asStateFlow()

    private val _showApiKeyDialog = MutableStateFlow(false)
    val showApiKeyDialog: StateFlow<Boolean> = _showApiKeyDialog.asStateFlow()

    private val _copiedNotice = MutableStateFlow<String?>(null)
    val copiedNotice: StateFlow<String?> = _copiedNotice.asStateFlow()

    private var activeJob: Job? = null

    // 5 Bespoke Pre-Engineered Study & Document AI Pipelines
    val studyPipelines: List<GeminiPromptPreset> = listOf(
        GeminiPromptPreset(
            id = "ai_ocr_post_correction",
            title = "Correzione OCR e Markdown perfetto",
            subtitle = "Ripulisce parole sporche e filigrane degli editori, ricostruisce le tabelle.",
            targetModel = GeminiTargetModel.GEMINI_3_8_FLASH_LITE,
            systemInstruction = """
                You are an expert document OCR post-processing and text restoration engine.
                Your task is to:
                1. Correct misspelled and distorted OCR character errors faithfully.
                2. Reconnect hyphenated words split across lines into single words.
                3. Strip out all residual publisher watermarks, header artifacts, and UI hints (e.g. 'ZANICHELLI', 'tieni premuto esc', 'modalità a schermo intero', 'copia riservata').
                4. Reconstruct complex multi-column layouts, tables, and lists into pristine, publication-grade GitHub-flavored Markdown.
                5. Format formulas into standard LaTeX ($...$ and $$...$$).
                Do not summarize; reconstruct the original text content completely.
            """.trimIndent(),
            promptTemplate = { ctx -> "Please apply comprehensive AI OCR post-correction and clean this document text into pristine Markdown:\n\n$ctx" }
        ),
        GeminiPromptPreset(
            id = "executive_summary_concepts",
            title = "Riassunto e concetti chiave",
            subtitle = "Capitoli strutturati con date, autori, opere e formule.",
            targetModel = GeminiTargetModel.GEMINI_3_8_FLASH_LITE,
            systemInstruction = """
                You are a high-level academic research consultant.
                Given the document, synthesize a structured study guide with:
                - ### 1. Chapter Executive Summary (High-level narrative thesis)
                - ### 2. Historical & Event Timeline (Chronological dates and milestones)
                - ### 3. Authors, Key Figures & Notable Works (With their significance)
                - ### 4. Core Definitions & Technical Terminology (Clear, concise definitions)
                - ### 5. Key Conceptual Takeaways (Bullet points)
                Use clean headings, bold keywords, and bullet points.
            """.trimIndent(),
            promptTemplate = { ctx -> "Synthesize this study document into an executive summary, chronological timeline, key figures, and core definitions:\n\n$ctx" }
        ),
        GeminiPromptPreset(
            id = "oral_exam_simulator",
            title = "Simulatore di interrogazione",
            subtitle = "10 domande mirate con spunti di approfondimento.",
            targetModel = GeminiTargetModel.GEMINI_3_8_FLASH,
            systemInstruction = """
                You are a university professor preparing a student for a demanding oral examination ('interrogazione').
                Based strictly on the provided text, formulate:
                - 10 targeted oral exam questions ranging from foundational comprehension to deep analytical synthesis.
                - For each question:
                  * Provide an exhaustive Model Answer with citations of dates, figures, or theories from the text.
                  * List Key Concepts that the examiner expects to hear.
                  * Highlight Common Pitfalls/Mistakes students usually make on this question.
                Format clearly with numbered headings and blockquotes.
            """.trimIndent(),
            promptTemplate = { ctx -> "Generate a 10-question university-level oral exam simulator with complete model answers based on this material:\n\n$ctx" }
        ),
        GeminiPromptPreset(
            id = "flashcards_concept_map",
            title = "Flashcard e mappa concettuale",
            subtitle = "Entità, date e opere in schede di studio.",
            targetModel = GeminiTargetModel.GEMINI_3_8_FLASH_LITE,
            systemInstruction = """
                You are a cognitive study architect.
                Analyze the provided document and produce:
                1. A comprehensive Markdown Study Flashcard Table with columns:
                   | Front (Term / Question / Artwork) | Back (Definition / Significance / Date) | Category |
                2. A Mermaid.js diagram (`graph TD`) mapping the semantic relationships between all major entities, concepts, and movements.
                3. A Quick-Review Bullet Checklist for exam morning.
            """.trimIndent(),
            promptTemplate = { ctx -> "Extract a study flashcard table, Mermaid concept map, and exam review checklist from this document:\n\n$ctx" }
        ),
        GeminiPromptPreset(
            id = "vision_inspector",
            title = "Analisi di immagini e schemi",
            subtitle = "Spiega diagrammi, schemi, opere e formule con Gemini Vision.",
            targetModel = GeminiTargetModel.GEMINI_3_8_FLASH_LITE,
            systemInstruction = """
                You are an advanced multimodal visual intelligence specialist.
                Examine the attached image or diagram in thorough detail.
                - Identify and explain all visual components, diagrams, figures, formulas, or artworks present.
                - Transcribe any text embedded in the image with 100% accuracy.
                - Explain the academic or practical meaning of the graphic in context.
            """.trimIndent(),
            promptTemplate = { ctx -> "Analyze the attached diagram/page image in detail. Explain all visual elements and academic significance:\n\n$ctx" }
        )
    )

    init {
        refreshQuotaUsages()
        testApiKey()
    }

    fun toggleSmartRouting(enabled: Boolean) {
        _smartRouting.value = enabled
    }

    fun selectModel(model: GeminiTargetModel) {
        _selectedModel.value = model
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun setContextDocument(docText: String) {
        _contextDocument.value = docText
    }

    fun clearContextDocument() {
        _contextDocument.value = null
    }

    fun attachAudio(uri: Uri, mimeType: String = "audio/mp4") {
        _audioAttachmentUri.value = uri
        _audioMimeType.value = mimeType
        _selectedModel.value = GeminiTargetModel.GEMINI_3_8_TRANSCRIBE
    }

    fun clearAudioAttachment() {
        _audioAttachmentUri.value = null
    }

    fun attachImage(uri: Uri) {
        _imageAttachmentUri.value = uri
        if (!_selectedModel.value.supportsVision) {
            _selectedModel.value = GeminiTargetModel.GEMINI_3_8_FLASH_LITE
        }
    }

    fun clearImageAttachment() {
        _imageAttachmentUri.value = null
    }

    fun refreshQuotaUsages() {
        _quotaUsages.value = geminiService.getQuotaUsages()
    }

    fun applyPreset(preset: GeminiPromptPreset) {
        _selectedModel.value = preset.targetModel
        val ctx = _contextDocument.value ?: _inputText.value
        val prompt = preset.promptTemplate(ctx)
        generate(prompt, preset.systemInstruction)
    }

    fun generateCurrent() {
        val prompt = _inputText.value.ifBlank {
            _contextDocument.value ?: if (_imageAttachmentUri.value != null) "Please inspect and explain this image in detail." else return
        }
        val fullPrompt = if (_contextDocument.value != null && _inputText.value.isNotBlank()) {
            "${_inputText.value}\n\n=== REFERENCE CONTEXT ===\n${_contextDocument.value}"
        } else {
            prompt
        }
        generate(fullPrompt)
    }

    fun generate(prompt: String, systemInstruction: String? = null) {
        activeJob?.cancel()

        activeJob = viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            _generationState.value = GeminiGenerationState(
                isGenerating = true,
                thinkingTrace = "",
                outputText = "",
                error = null,
                modelUsed = _selectedModel.value,
                fallbackNotice = null
            )

            val thinkingSb = StringBuilder()
            val outputSb = StringBuilder()

            val result = geminiService.streamGenerateWithFallback(
                requestedModel = _selectedModel.value,
                prompt = prompt,
                systemInstruction = systemInstruction,
                inlineAudioUri = _audioAttachmentUri.value,
                audioMimeType = _audioMimeType.value,
                inlineImageUri = _imageAttachmentUri.value,
                smartRouting = _smartRouting.value,
                onFallbackEvent = { from, to, _ ->
                    _generationState.value = _generationState.value.copy(
                        modelUsed = to,
                        fallbackNotice = "Switched to ${to.displayName} (Auto-fallback from ${from.displayName})"
                    )
                },
                onThinkingChunk = { chunk ->
                    thinkingSb.append(chunk)
                    _generationState.value = _generationState.value.copy(
                        thinkingTrace = thinkingSb.toString()
                    )
                },
                onTextChunk = { chunk ->
                    outputSb.append(chunk)
                    _generationState.value = _generationState.value.copy(
                        outputText = outputSb.toString(),
                        estimatedTokens = Math.max(1, (outputSb.length / 3.8).toInt())
                    )
                }
            )

            val duration = System.currentTimeMillis() - startTime
            refreshQuotaUsages()

            result.onSuccess { execResult ->
                _generationState.value = _generationState.value.copy(
                    isGenerating = false,
                    modelUsed = execResult.finalModelUsed,
                    latencyMs = duration,
                    fallbackNotice = execResult.fallbackNotice
                )
            }.onFailure { err ->
                _generationState.value = _generationState.value.copy(
                    isGenerating = false,
                    error = err.localizedMessage ?: "Generation failed"
                )
            }
        }
    }

    fun stopGeneration() {
        activeJob?.cancel()
        _generationState.value = _generationState.value.copy(isGenerating = false)
        refreshQuotaUsages()
    }

    fun clearOutput() {
        activeJob?.cancel()
        _generationState.value = GeminiGenerationState(modelUsed = _selectedModel.value)
    }

    fun testApiKey() {
        viewModelScope.launch(Dispatchers.IO) {
            val key = geminiService.getApiKey()
            _maskedApiKey.value = geminiService.getMaskedApiKey()
            if (key.isBlank()) {
                _apiKeyStatus.value = "UNCONFIGURED"
                _connectionLatency.value = null
                return@launch
            }

            _apiKeyStatus.value = "TESTING"
            val res = geminiService.testConnection()
            res.onSuccess { latency ->
                _apiKeyStatus.value = "CONNECTED"
                _connectionLatency.value = latency
            }.onFailure {
                _apiKeyStatus.value = "ERROR"
                _connectionLatency.value = null
            }
        }
    }

    fun openApiKeyDialog() {
        _apiKeyInput.value = geminiService.getApiKey()
        _showApiKeyDialog.value = true
    }

    fun closeApiKeyDialog() {
        _showApiKeyDialog.value = false
    }

    fun saveApiKey(key: String) {
        geminiService.saveCustomApiKey(key)
        _maskedApiKey.value = geminiService.getMaskedApiKey()
        _showApiKeyDialog.value = false
        testApiKey()
    }

    fun clearApiKey() {
        geminiService.clearCustomApiKey()
        _maskedApiKey.value = geminiService.getMaskedApiKey()
        _showApiKeyDialog.value = false
        testApiKey()
    }

    fun copyOutputToClipboard() {
        val text = _generationState.value.outputText
        if (text.isEmpty()) return
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Gemini Output", text)
        clipboard.setPrimaryClip(clip)
        _copiedNotice.value = "Generated text copied to clipboard (~${_generationState.value.estimatedTokens} tokens)"
    }

    fun exportMarkdownFile() {
        val text = _generationState.value.outputText
        if (text.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fileName = "AI_Study_Guide_$timestamp.md"
                val app = getApplication<Application>()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/markdown")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/OmniConvert/StudyGuides")
                    }
                    val resolver = app.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { os ->
                            os.write(text.toByteArray(Charsets.UTF_8))
                        }
                    }
                } else {
                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "OmniConvert/StudyGuides")
                    dir.mkdirs()
                    val file = File(dir, fileName)
                    FileOutputStream(file).use { os ->
                        os.write(text.toByteArray(Charsets.UTF_8))
                    }
                }
                _copiedNotice.value = "Saved study guide to Downloads/OmniConvert/StudyGuides/$fileName"
            } catch (e: Exception) {
                _copiedNotice.value = "Export failed: ${e.message}"
            }
        }
    }

    fun convertOutputToPdf() {
        val text = _generationState.value.outputText
        if (text.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fileName = "AI_Study_Guide_$timestamp.pdf"
                val app = getApplication<Application>()

                val pdfDocument = PdfDocument()
                val pageWidth = 595
                val pageHeight = 842
                val margin = 40
                val usableWidth = pageWidth - (margin * 2)

                val paint = Paint().apply {
                    color = Color.DKGRAY
                    textSize = 10.5f
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                }

                val titlePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 15f
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }

                val subPaint = Paint().apply {
                    color = Color.GRAY
                    textSize = 8.5f
                    isAntiAlias = true
                }

                // Word wrap text into lines
                val lines = mutableListOf<String>()
                text.lines().forEach { rawLine ->
                    if (rawLine.isBlank()) {
                        lines.add("")
                    } else {
                        val words = rawLine.split(" ")
                        var curLine = ""
                        for (w in words) {
                            val candidate = if (curLine.isEmpty()) w else "$curLine $w"
                            if (paint.measureText(candidate) <= usableWidth) {
                                curLine = candidate
                            } else {
                                if (curLine.isNotEmpty()) lines.add(curLine)
                                curLine = w
                            }
                        }
                        if (curLine.isNotEmpty()) lines.add(curLine)
                    }
                }

                val linesPerPage = 48
                val totalPages = Math.max(1, (lines.size + linesPerPage - 1) / linesPerPage)

                for (pageIdx in 0 until totalPages) {
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIdx + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas: Canvas = page.canvas

                    // Header
                    canvas.drawText("OmniConvert AI Studio • Study Guide & Analysis", margin.toFloat(), 32f, subPaint)
                    canvas.drawLine(margin.toFloat(), 38f, (pageWidth - margin).toFloat(), 38f, subPaint)

                    var y = 60f
                    val startLine = pageIdx * linesPerPage
                    val endLine = Math.min(lines.size, startLine + linesPerPage)

                    for (l in startLine until endLine) {
                        val lineStr = lines[l]
                        if (lineStr.startsWith("#")) {
                            canvas.drawText(lineStr.replace("#", "").trim(), margin.toFloat(), y, titlePaint)
                            y += 20f
                        } else {
                            canvas.drawText(lineStr, margin.toFloat(), y, paint)
                            y += 15f
                        }
                    }

                    // Footer
                    canvas.drawLine(margin.toFloat(), (pageHeight - 30).toFloat(), (pageWidth - margin).toFloat(), (pageHeight - 30).toFloat(), subPaint)
                    canvas.drawText("Page ${pageIdx + 1} of $totalPages", (pageWidth - margin - 60).toFloat(), (pageHeight - 16).toFloat(), subPaint)

                    pdfDocument.finishPage(page)
                }

                // Save PDF to Downloads/OmniConvert/PDFs
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/OmniConvert/PDFs")
                    }
                    val resolver = app.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { os ->
                            pdfDocument.writeTo(os)
                        }
                    }
                } else {
                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "OmniConvert/PDFs")
                    dir.mkdirs()
                    val file = File(dir, fileName)
                    FileOutputStream(file).use { os ->
                        pdfDocument.writeTo(os)
                    }
                }
                pdfDocument.close()
                _copiedNotice.value = "Converted to PDF: Downloads/OmniConvert/PDFs/$fileName"
            } catch (e: Exception) {
                _copiedNotice.value = "PDF generation failed: ${e.message}"
            }
        }
    }

    fun dismissCopiedNotice() {
        _copiedNotice.value = null
    }
}
