package com.example.model

import android.net.Uri
import com.example.service.PageMarkerStyle
import com.example.service.PdfMarkdownOptions

enum class LlmPromptWrapper(val displayName: String, val description: String) {
    CLAUDE_XML(
        displayName = "Tag XML per Claude",
        description = "Racchiude il contesto nei tag <documents><document> con sorgente e contenuto."
    ),
    CHATGPT_SYSTEM(
        displayName = "Blocco di sistema ChatGPT",
        description = "Aggiunge istruzioni di contesto con sezioni Markdown e intestazioni dei file."
    ),
    GEMINI_BRIEF(
        displayName = "Brief di contesto per Gemini",
        description = "Delimitatori gerarchici (=== DOCUMENTO ===) ottimizzati per Gemini."
    ),
    RAW_MARKDOWN(
        displayName = "Markdown puro",
        description = "Markdown pulito senza istruzioni per l'AI, separato da linee orizzontali."
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
    val maxPagesPerDoc: Int = 250,
    val parseAllPages: Boolean = false,
    /** Ignore the PDF text layer and always run OCR (for PDFs with broken embedded text). */
    val forceOcr: Boolean = false,
    /** How PDF page boundaries are written (hidden comments by default, so headings stay meaningful). */
    val pageMarkers: PageMarkerStyle = PageMarkerStyle.COMMENT,
    /** Remove running headers/footers and page numbers repeated on every page. */
    val cleanMargins: Boolean = true
) {
    val pdfOptions: PdfMarkdownOptions get() = PdfMarkdownOptions(pageMarkers, cleanMargins)
}

data class CompiledMarkdownResult(
    val rawMarkdown: String,
    val wrappedPrompt: String,
    val totalTokens: Int,
    val totalChars: Int,
    val docCount: Int
)
