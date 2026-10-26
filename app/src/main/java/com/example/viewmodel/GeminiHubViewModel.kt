package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryKind
import com.example.data.HistoryRepository
import com.example.util.FileNames
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.util.TextPdfWriter
import com.example.util.resultOf
import com.example.util.userMessage
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            _contextDocument.value ?: if (_imageAttachmentUri.value != null) "Analizza e spiega questa immagine nel dettaglio." else return
        }
        val fullPrompt = if (_contextDocument.value != null && _inputText.value.isNotBlank()) {
            "${_inputText.value}\n\n=== CONTESTO DI RIFERIMENTO ===\n${_contextDocument.value}"
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
                    if (!isActive) return@streamGenerateWithFallback
                    // Text streamed by the model that failed is discarded so the answer is not duplicated.
                    outputSb.setLength(0)
                    thinkingSb.setLength(0)
                    _generationState.value = _generationState.value.copy(
                        modelUsed = to,
                        outputText = "",
                        thinkingTrace = "",
                        fallbackNotice = "Passato a ${to.displayName} (riserva automatica di ${from.displayName})"
                    )
                },
                onThinkingChunk = { chunk ->
                    // A stopped or replaced generation must not write into the new answer.
                    if (!isActive) return@streamGenerateWithFallback
                    thinkingSb.append(chunk)
                    _generationState.value = _generationState.value.copy(
                        thinkingTrace = thinkingSb.toString()
                    )
                },
                onTextChunk = { chunk ->
                    if (!isActive) return@streamGenerateWithFallback
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
                    error = err.localizedMessage ?: "Generazione non riuscita"
                )
            }
        }
    }

    fun stopGeneration() {
        activeJob?.cancel()
        _generationState.value = _generationState.value.copy(isGenerating = false)
        refreshQuotaUsages()
    }

    fun dismissError() {
        _generationState.value = _generationState.value.copy(error = null)
    }

    fun dismissFallbackNotice() {
        _generationState.value = _generationState.value.copy(fallbackNotice = null)
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
        _copiedNotice.value = "Testo copiato negli appunti (~${_generationState.value.estimatedTokens} token)"
    }

    fun exportMarkdownFile() {
        val text = _generationState.value.outputText
        if (text.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            resultOf {
                val fileName = "Guida_di_studio_${FileNames.timestamp()}.md"
                OutputStorage.save(app, fileName, "text/markdown", OutputFolder.STUDY_GUIDES) { os ->
                    os.write(text.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess { uri ->
                HistoryRepository.record(app, uri, "Gemini", HistoryKind.MARKDOWN, "Guida_di_studio.md")
                _copiedNotice.value = "Guida salvata in ${OutputStorage.displayPath(OutputFolder.STUDY_GUIDES)}"
            }.onFailure { e ->
                _copiedNotice.value = "Esportazione non riuscita: ${e.userMessage("errore sconosciuto")}"
            }
        }
    }

    fun convertOutputToPdf() {
        val text = _generationState.value.outputText
        if (text.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            resultOf {
                val fileName = "Guida_di_studio_${FileNames.timestamp()}.pdf"
                OutputStorage.save(app, fileName, "application/pdf", OutputFolder.PDFS) { os ->
                    TextPdfWriter.write(text, "Guida di studio · Morph", os)
                }
            }.onSuccess { uri ->
                HistoryRepository.record(app, uri, "Gemini", HistoryKind.PDF, "Guida_di_studio.pdf")
                _copiedNotice.value = "PDF salvato in ${OutputStorage.displayPath(OutputFolder.PDFS)}"
            }.onFailure { e ->
                _copiedNotice.value = "Creazione del PDF non riuscita: ${e.userMessage("errore sconosciuto")}"
            }
        }
    }

    fun dismissCopiedNotice() {
        _copiedNotice.value = null
    }
}
