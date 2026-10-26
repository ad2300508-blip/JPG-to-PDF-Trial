package com.example.model

enum class GeminiModelTier(val label: String) {
    DAILY_WORKHORSE("500 RPD • Fast & High Quota"),
    DEEP_REASONING("20 RPD • Deep Reasoning"),
    ULTRA_QUOTA("14.4K RPD • High Volume Text"),
    SPECIALIZED("Specialized Audio & Speech")
}

enum class GeminiTargetModel(
    val displayName: String,
    val technicalModelId: String,
    val tier: GeminiModelTier,
    val maxRpd: Int,
    val maxRpm: Int,
    val maxTpm: Int,
    val description: String,
    val categoryBadge: String,
    val hasExtendedThinking: Boolean = false,
    val supportsVision: Boolean = true,
    val defaultTemperature: Float = 0.7f
) {
    // a) High-Quota Daily Workhorses (500 RPD, 15 RPM, 250K TPM)
    GEMINI_3_8_FLASH_LITE(
        displayName = "Gemini 3.8 Flash-Lite",
        technicalModelId = "gemini-3.8-flash-lite",
        tier = GeminiModelTier.DAILY_WORKHORSE,
        maxRpd = 500,
        maxRpm = 15,
        maxTpm = 250_000,
        description = "Modello principale di tutti i giorni: velocissimo per impaginazione, tabelle e correzione OCR.",
        categoryBadge = "FAST 500 RPD",
        supportsVision = true
    ),
    GEMINI_3_6_FLASH_LITE(
        displayName = "Gemini 3.6 Flash-Lite",
        technicalModelId = "gemini-3.6-flash-lite",
        tier = GeminiModelTier.DAILY_WORKHORSE,
        maxRpd = 500,
        maxRpm = 15,
        maxTpm = 250_000,
        description = "Riserva con quota alta: 500 richieste al giorno, rapido su JSON e Markdown.",
        categoryBadge = "BACKUP 500 RPD",
        supportsVision = true
    ),
    GEMINI_3_1_FLASH_LITE(
        displayName = "Gemini 3.1 Flash-Lite",
        technicalModelId = "gemini-3.1-flash-lite",
        tier = GeminiModelTier.DAILY_WORKHORSE,
        maxRpd = 500,
        maxRpm = 15,
        maxTpm = 250_000,
        description = "Stabile e veloce per riformattare testi, riassunti e conversioni in Markdown.",
        categoryBadge = "BACKUP 500 RPD",
        supportsVision = true
    ),

    // b) Deep Reasoning & Full-Book Context (20 RPD, 5 RPM, 250K TPM)
    GEMINI_3_8_FLASH(
        displayName = "Gemini 3.8 Flash",
        technicalModelId = "gemini-3.8-flash",
        tier = GeminiModelTier.DEEP_REASONING,
        maxRpd = 20,
        maxRpm = 5,
        maxTpm = 250_000,
        description = "Ragionamento approfondito passo passo. Ideale per simulare interrogazioni e analisi complesse.",
        categoryBadge = "REASONING 20 RPD",
        hasExtendedThinking = true,
        supportsVision = true,
        defaultTemperature = 0.4f
    ),
    GEMINI_3_6_FLASH(
        displayName = "Gemini 3.6 Flash",
        technicalModelId = "gemini-3.6-flash",
        tier = GeminiModelTier.DEEP_REASONING,
        maxRpd = 20,
        maxRpm = 5,
        maxTpm = 250_000,
        description = "Sintesi analitica avanzata e comprensione di documenti complessi a più colonne.",
        categoryBadge = "REASONING 20 RPD",
        supportsVision = true,
        defaultTemperature = 0.5f
    ),
    GEMINI_2_5_FLASH(
        displayName = "Gemini 2.5 Flash",
        technicalModelId = "gemini-2.5-flash",
        tier = GeminiModelTier.DEEP_REASONING,
        maxRpd = 20,
        maxRpm = 5,
        maxTpm = 250_000,
        description = "Contesto grande quanto un libro: perfetto per manuali interi e ricerche lunghe.",
        categoryBadge = "DEEP CONTEXT 20 RPD",
        supportsVision = true,
        defaultTemperature = 0.5f
    ),

    // c) Ultra-High Quota Text Models (14,400 RPD, 30 RPM, 16K TPM)
    GEMMA_4_31B_IT(
        displayName = "Gemma 4 31B IT",
        technicalModelId = "gemma-4-31b-it",
        tier = GeminiModelTier.ULTRA_QUOTA,
        maxRpd = 14_400,
        maxRpm = 30,
        maxTpm = 16_000,
        description = "14.400 richieste al giorno: la rete di sicurezza quando le altre quote sono finite.",
        categoryBadge = "ULTRA 14.4K RPD",
        supportsVision = false
    ),
    GEMMA_4_26B_IT(
        displayName = "Gemma 4 26B IT",
        technicalModelId = "gemma-4-26b-it",
        tier = GeminiModelTier.ULTRA_QUOTA,
        maxRpd = 14_400,
        maxRpm = 30,
        maxTpm = 16_000,
        description = "Modello open ad altissimo volume per estrazioni continue.",
        categoryBadge = "ULTRA 14.4K RPD",
        supportsVision = false
    ),

    // d) Specialized Audio/Speech Models
    GEMINI_3_8_TRANSCRIBE(
        displayName = "Gemini 3.8 Transcribe",
        technicalModelId = "gemini-3.8-transcribe",
        tier = GeminiModelTier.SPECIALIZED,
        maxRpd = 15,
        maxRpm = 5,
        maxTpm = 250_000,
        description = "Specializzato nel parlato: trascrive lezioni e note vocali direttamente in Markdown.",
        categoryBadge = "TRANSCRIBE 15 RPD",
        supportsVision = false
    ),
    GEMINI_3_8_FLASH_TTS(
        displayName = "Gemini 3.8 Flash TTS",
        technicalModelId = "gemini-3.8-flash-tts",
        tier = GeminiModelTier.SPECIALIZED,
        maxRpd = 10,
        maxRpm = 5,
        maxTpm = 250_000,
        description = "Genera brevi riassunti audio e flashcard parlate.",
        categoryBadge = "TTS AUDIO 10 RPD",
        supportsVision = false
    );

    val tpmQuotaLabel: String
        get() = "${maxTpm / 1000}K TPM • $maxRpd RPD"
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
    val modelUsed: GeminiTargetModel = GeminiTargetModel.GEMINI_3_8_FLASH_LITE,
    val latencyMs: Long = 0,
    val estimatedTokens: Int = 0,
    val fallbackNotice: String? = null
)

data class ModelQuotaUsage(
    val model: GeminiTargetModel,
    val usedRpd: Int,
    val remainingRpd: Int,
    val isThrottled: Boolean
)
