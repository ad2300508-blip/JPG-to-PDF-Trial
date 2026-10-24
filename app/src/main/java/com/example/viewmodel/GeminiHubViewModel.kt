package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.GeminiGenerationState
import com.example.model.GeminiPromptPreset
import com.example.model.GeminiTargetModel
import com.example.service.GeminiApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GeminiHubViewModel(application: Application) : AndroidViewModel(application) {

    private val geminiService = GeminiApiService(application.applicationContext)

    private val _selectedModel = MutableStateFlow(GeminiTargetModel.GEMINI_3_FLASH)
    val selectedModel: StateFlow<GeminiTargetModel> = _selectedModel.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _contextDocument = MutableStateFlow<String?>(null)
    val contextDocument: StateFlow<String?> = _contextDocument.asStateFlow()

    private val _audioAttachmentUri = MutableStateFlow<Uri?>(null)
    val audioAttachmentUri: StateFlow<Uri?> = _audioAttachmentUri.asStateFlow()

    private val _audioMimeType = MutableStateFlow<String?>("audio/mp4")
    val audioMimeType: StateFlow<String?> = _audioMimeType.asStateFlow()

    private val _generationState = MutableStateFlow(GeminiGenerationState())
    val generationState: StateFlow<GeminiGenerationState> = _generationState.asStateFlow()

    private val _apiKeyStatus = MutableStateFlow("UNKNOWN") // "CONNECTED", "ERROR", "UNCONFIGURED", "TESTING"
    val apiKeyStatus: StateFlow<String> = _apiKeyStatus.asStateFlow()

    private val _connectionLatency = MutableStateFlow<Long?>(null)
    val connectionLatency: StateFlow<Long?> = _connectionLatency.asStateFlow()

    private val _apiKeyInput = MutableStateFlow("")
    val apiKeyInput: StateFlow<String> = _apiKeyInput.asStateFlow()

    private val _showApiKeyDialog = MutableStateFlow(false)
    val showApiKeyDialog: StateFlow<Boolean> = _showApiKeyDialog.asStateFlow()

    private val _copiedNotice = MutableStateFlow<String?>(null)
    val copiedNotice: StateFlow<String?> = _copiedNotice.asStateFlow()

    private var activeJob: Job? = null

    val presets: List<GeminiPromptPreset> = listOf(
        GeminiPromptPreset(
            id = "summary_flash",
            title = "Executive Summary",
            subtitle = "Extract core insights, action items & outline at top speed",
            targetModel = GeminiTargetModel.GEMINI_3_FLASH,
            systemInstruction = "You are a concise executive researcher. Summarize the provided document into a structured overview, key takeaways, and action items using clean Markdown.",
            promptTemplate = { ctx -> "Please synthesize this document into an executive summary:\n\n$ctx" }
        ),
        GeminiPromptPreset(
            id = "deep_thinking",
            title = "Extended Thinking Analysis",
            subtitle = "Multi-step reasoning, finding contradictions & technical critique",
            targetModel = GeminiTargetModel.GEMINI_3_8_THINKING,
            systemInstruction = "You are a principal engineer and critical reasoning specialist. Think deeply step-by-step through every assumption, identify subtle flaws, and provide a comprehensive architectural critique.",
            promptTemplate = { ctx -> "Perform a deep analytical review of the following material with explicit justification for each conclusion:\n\n$ctx" }
        ),
        GeminiPromptPreset(
            id = "translate_italian",
            title = "Translate to Italian",
            subtitle = "Translate document keeping all LaTeX formulas & Markdown intact",
            targetModel = GeminiTargetModel.GEMINI_3_5_TRANSLATE,
            systemInstruction = "You are an expert technical translator. Translate the document into natural, professional Italian. Strictly preserve all Markdown formatting, code fences, LaTeX equations ($...$ and $$...$$), and table borders.",
            promptTemplate = { ctx -> "Traduci accuratamente in italiano il seguente testo, preservando tabelle, formule e sintassi markdown:\n\n$ctx" }
        ),
        GeminiPromptPreset(
            id = "translate_english",
            title = "Translate to English",
            subtitle = "Translate document into fluent English preserving formatting",
            targetModel = GeminiTargetModel.GEMINI_3_5_TRANSLATE,
            systemInstruction = "You are an expert technical translator. Translate the document into fluent, professional English. Strictly preserve all Markdown tables, LaTeX equations, and code blocks.",
            promptTemplate = { ctx -> "Translate the following material into fluent technical English, strictly preserving all markdown structures:\n\n$ctx" }
        ),
        GeminiPromptPreset(
            id = "latex_tables",
            title = "Formulas & LaTeX Tables",
            subtitle = "Restructure scanned messy data into pristine LaTeX & tables",
            targetModel = GeminiTargetModel.GEMINI_3_FLASH,
            systemInstruction = "You are a LaTeX and mathematical formatting expert. Identify all numerical data, equations, and tables in the document and output them in standard LaTeX and Markdown table notation.",
            promptTemplate = { ctx -> "Extract and convert all mathematical formulas into LaTeX and all tabular data into Markdown tables from this document:\n\n$ctx" }
        ),
        GeminiPromptPreset(
            id = "audio_transcribe",
            title = "Transcribe & Structure Audio",
            subtitle = "Convert lecture/meeting recording into meeting notes",
            targetModel = GeminiTargetModel.GEMINI_3_5_TRANSCRIBE,
            systemInstruction = "You are a professional audio transcriptionist. Transcribe the audio clearly, organize into sections with timestamps, and generate key bullet points.",
            promptTemplate = { _ -> "Please transcribe this audio recording completely and provide a structured summary with key takeaways." }
        ),
        GeminiPromptPreset(
            id = "audio_voice_dialog",
            title = "1M High-Capacity Audio Dialog",
            subtitle = "Analyze long lecture, podcast or audio track with 1M token context",
            targetModel = GeminiTargetModel.GEMINI_2_5_FLASH_AUDIO,
            systemInstruction = "You are an intelligent audio conversational assistant with a 1 million token context window. Analyze this audio deeply, explain concepts discussed, and answer any inquiries.",
            promptTemplate = { _ -> "Analyze this audio track in detail. Provide an in-depth breakdown of topics discussed and notable quotes." }
        )
    )

    init {
        testApiKey()
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
        _selectedModel.value = GeminiTargetModel.GEMINI_3_5_TRANSCRIBE
    }

    fun clearAudioAttachment() {
        _audioAttachmentUri.value = null
    }

    fun applyPreset(preset: GeminiPromptPreset) {
        _selectedModel.value = preset.targetModel
        val ctx = _contextDocument.value ?: _inputText.value
        val prompt = preset.promptTemplate(ctx)
        generate(prompt, preset.systemInstruction)
    }

    fun generateCurrent() {
        val prompt = _inputText.value.ifBlank {
            _contextDocument.value ?: return
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
                modelUsed = _selectedModel.value
            )

            val thinkingSb = StringBuilder()
            val outputSb = StringBuilder()

            val result = geminiService.streamGenerate(
                model = _selectedModel.value,
                prompt = prompt,
                systemInstruction = systemInstruction,
                inlineAudioUri = _audioAttachmentUri.value,
                audioMimeType = _audioMimeType.value,
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
            result.onSuccess {
                _generationState.value = _generationState.value.copy(
                    isGenerating = false,
                    latencyMs = duration
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
    }

    fun clearOutput() {
        activeJob?.cancel()
        _generationState.value = GeminiGenerationState(modelUsed = _selectedModel.value)
    }

    fun testApiKey() {
        viewModelScope.launch(Dispatchers.IO) {
            val key = geminiService.getApiKey()
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
        _showApiKeyDialog.value = false
        testApiKey()
    }

    fun clearApiKey() {
        geminiService.clearCustomApiKey()
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

    fun dismissCopiedNotice() {
        _copiedNotice.value = null
    }
}
