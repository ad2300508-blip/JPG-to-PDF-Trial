package com.example.model

enum class GeminiTargetModel(
    val displayName: String,
    val technicalModelId: String,
    val tpmQuota: String,
    val description: String,
    val categoryBadge: String,
    val hasExtendedThinking: Boolean = false,
    val defaultTemperature: Float = 0.7f
) {
    GEMINI_3_FLASH(
        displayName = "Gemini 3 Flash Live",
        technicalModelId = "gemini-3-flash",
        tpmQuota = "65K TPM (Unlimited RPD)",
        description = "Ultra-fast document summarization, layout extraction & table conversions.",
        categoryBadge = "FAST STREAMING"
    ),
    GEMINI_3_8_THINKING(
        displayName = "Gemini 3.8 Extended Thinking",
        technicalModelId = "gemini-3.8-flash",
        tpmQuota = "65K TPM (Unlimited RPD)",
        description = "Deep cognitive reasoning with multi-step thinking trace before answering.",
        categoryBadge = "REASONING",
        hasExtendedThinking = true,
        defaultTemperature = 0.4f
    ),
    GEMINI_3_5_TRANSLATE(
        displayName = "Gemini 3.5 Live Translate",
        technicalModelId = "gemini-3.5-flash",
        tpmQuota = "20K TPM (Unlimited RPD)",
        description = "Context-aware translation preserving Markdown structure, code fences & LaTeX.",
        categoryBadge = "TRANSLATE"
    ),
    GEMINI_3_5_TRANSCRIBE(
        displayName = "Gemini 3.5 Transcribe Live",
        technicalModelId = "gemini-3.5-flash",
        tpmQuota = "20K TPM (Unlimited RPD)",
        description = "Speech-to-text audio transcription directly into structured Markdown notes.",
        categoryBadge = "AUDIO SPEECH"
    ),
    GEMINI_2_5_FLASH_AUDIO(
        displayName = "Gemini 2.5 Flash Native Audio",
        technicalModelId = "gemini-2.5-flash",
        tpmQuota = "1M TPM (Unlimited RPD)",
        description = "Massive 1M token capacity for long audio dialog, podcasts & voice reasoning.",
        categoryBadge = "HIGH CAPACITY"
    ),
    GEMINI_3_8_LIVE(
        displayName = "Gemini 3.8 Live",
        technicalModelId = "gemini-3.8-flash",
        tpmQuota = "65K TPM (Unlimited RPD)",
        description = "High-precision analytical synthesis, code generation & document architecture.",
        categoryBadge = "SYNTHESIS"
    )
}

data class GeminiPromptPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val targetModel: GeminiTargetModel,
    val systemInstruction: String,
    val promptTemplate: (contextText: String) -> String
)

data class GeminiGenerationState(
    val isGenerating: Boolean = false,
    val thinkingTrace: String = "",
    val outputText: String = "",
    val error: String? = null,
    val modelUsed: GeminiTargetModel = GeminiTargetModel.GEMINI_3_FLASH,
    val latencyMs: Long = 0,
    val estimatedTokens: Int = 0
)
