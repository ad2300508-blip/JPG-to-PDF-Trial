package com.example.model

import android.net.Uri

enum class LlmPromptWrapper(val displayName: String, val description: String) {
    CLAUDE_XML(
        displayName = "Claude XML Tags",
        description = "Wraps context in <documents><document index=\"n\"><source>...</source><document_content> tags."
    ),
    CHATGPT_SYSTEM(
        displayName = "ChatGPT / OpenAI System Block",
        description = "Prepend developer context instructions with markdown sections and file headers."
    ),
    GEMINI_BRIEF(
        displayName = "Gemini Context Brief",
        description = "Optimized with hierarchical delimiters (=== DOCUMENT: [NAME] ===) and executive summary prefix."
    ),
    RAW_MARKDOWN(
        displayName = "Pure Markdown",
        description = "Clean markdown without AI instruction framing, separated by horizontal rules."
    )
}

data class MarkdownSourceItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: Uri? = null,
    val name: String,
    val sizeBytes: Long = 0,
    val mimeType: String = "",
    val extractedText: String = "",
    val pageCount: Int = 1,
    val parsedPages: Int = 1,
    val hasMorePages: Boolean = false,
    val isProcessing: Boolean = false,
    val error: String? = null
) {
    val estimatedTokens: Int
        get() = estimateTokens(extractedText)

    companion object {
        fun estimateTokens(text: String): Int {
            if (text.isEmpty()) return 0
            // ~3.8 characters per token heuristic for LLMs
            return Math.max(1, (text.length / 3.8).toInt())
        }
    }
}

data class MarkdownExportSettings(
    val promptWrapper: LlmPromptWrapper = LlmPromptWrapper.CLAUDE_XML,
    val includeMetadataHeader: Boolean = true,
    val includeTableOfContents: Boolean = true,
    val maxPagesPerDoc: Int = 250
)

data class CompiledMarkdownResult(
    val rawMarkdown: String,
    val wrappedPrompt: String,
    val totalTokens: Int,
    val totalChars: Int,
    val docCount: Int
)
